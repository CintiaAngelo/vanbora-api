package com.vanbora.api.config;

import com.vanbora.api.modules.guardian.domain.Dependent;
import com.vanbora.api.modules.guardian.repository.DependentRepository;
import com.vanbora.api.modules.notice.domain.Notice;
import com.vanbora.api.modules.notice.repository.NoticeRepository;
import com.vanbora.api.modules.route.domain.RouteStop;
import com.vanbora.api.modules.school.domain.School;
import com.vanbora.api.modules.school.repository.SchoolRepository;
import com.vanbora.api.modules.route.repository.RouteStopRepository;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.modules.transporter.service.ReviewService;
import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.modules.user.repository.UserRepository;
import com.vanbora.api.shared.enums.NoticePriority;
import com.vanbora.api.shared.enums.RouteStopStatus;
import com.vanbora.api.shared.enums.UserRole;
import com.vanbora.api.shared.validation.TextNormalization;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Popula dados de demonstração na primeira execução (banco vazio), permitindo testar
 * — e gravar — o app de ponta a ponta com logins conhecidos. O conteúdo em si vive em
 * {@link DemoDataSeeder}; aqui ficam a decisão de rodar e os backfills idempotentes
 * aplicados também a bancos já populados.
 *
 * Logins (senha: 123456):
 *  - Responsável:    mariana@vanbora.com
 *  - Transportador:  roberto@vanbora.com
 *  - Escola:         secretaria@objetivo.com.br
 *  - Monitor:        monitor.carlos@vanbora.com (equipe do Roberto)
 */
@Configuration
public class DataSeeder {

    private static final String DEFAULT_PASSWORD = "123456";

    /** Posição inicial de demonstração do transportador (centro de São Paulo). */
    private static final double ROBERTO_START_LAT = -23.5600;
    private static final double ROBERTO_START_LON = -46.6300;

    @Bean
    ApplicationRunner seedDemoData(
            @Value("${vanbora.seed-demo-data:true}") boolean enabled,
            org.springframework.transaction.PlatformTransactionManager transactionManager,
            UserRepository userRepository,
            TransporterProfileRepository transporterRepository,
            DependentRepository dependentRepository,
            NoticeRepository noticeRepository,
            RouteStopRepository routeStopRepository,
            ReviewService reviewService,
            SchoolRepository schoolRepository,
            DemoDataSeeder demoDataSeeder,
            MonitorDemoSeeder monitorDemoSeeder,
            PasswordEncoder passwordEncoder) {

        return args -> {
            if (!enabled) {
                return;
            }
            if (userRepository.count() > 0) {
                // Banco já populado: garante que os dados desta feature existam
                // (coordenadas, vínculo parada→dependente e publishAt dos avisos).
                backfillGeo(transporterRepository, routeStopRepository, dependentRepository);
                backfillNotices(noticeRepository);
                backfillSchools(schoolRepository);
                backfillSchoolRefs(schoolRepository, dependentRepository);
                // Precisa de uma transação aberta: acessa coleções LAZY (schools/neighborhoods)
                // que a leitura de findAll() sozinha não inicializa.
                new org.springframework.transaction.support.TransactionTemplate(transactionManager)
                        .executeWithoutResult(status ->
                                backfillServiceAreaNormalization(transporterRepository, schoolRepository));
                seedSchoolAccount(userRepository, schoolRepository, passwordEncoder.encode(DEFAULT_PASSWORD));
                // Recalcula média/contagem reais a partir das avaliações (corrige valores fixos antigos).
                transporterRepository.findAll().forEach(reviewService::recompute);
                monitorDemoSeeder.seed(passwordEncoder.encode(DEFAULT_PASSWORD));
                return;
            }

            String password = passwordEncoder.encode(DEFAULT_PASSWORD);

            backfillSchools(schoolRepository);
            // Antes do conjunto de demonstração: ele fecha registrando consentimento e
            // onboarding de TODOS os usuários existentes — inclusive o da escola.
            seedSchoolAccount(userRepository, schoolRepository, password);
            demoDataSeeder.seed(password);

            // Média e contagem de avaliações saem das avaliações realmente semeadas.
            transporterRepository.findAll().forEach(reviewService::recompute);

            // Painel de gestão da escola: vincula os dependentes ao catálogo de escolas.
            backfillSchoolRefs(schoolRepository, dependentRepository);

            // Perfil Monitor: conta do Carlos + percursos de demonstração.
            monitorDemoSeeder.seed(password);
        };
    }

    /** Popula o catálogo de escolas (demonstração) com coordenadas reais de São Paulo. */
    private void backfillSchools(SchoolRepository schoolRepository) {
        if (schoolRepository.count() > 0) {
            return;
        }
        schoolRepository.save(school("FIAP School", "01310-100", "Av. Paulista", "900",
                "Bela Vista", "São Paulo", "SP", -23.5614, -46.6562));
        schoolRepository.save(school("Escola Adventista", "02011-000", "Rua Voluntários da Pátria",
                "2000", "Santana", "São Paulo", "SP", -23.5012, -46.6256));
        schoolRepository.save(school("COC", "04101-300", "Rua Vergueiro", "3000",
                "Vila Mariana", "São Paulo", "SP", -23.5870, -46.6340));
        schoolRepository.save(school("Colégio Bandeirantes", "04026-002", "Rua Estela", "268",
                "Vila Mariana", "São Paulo", "SP", -23.5896, -46.6378));
        schoolRepository.save(school("Colégio Santa Cruz", "05058-001", "Rua Orobó", "277",
                "Alto de Pinheiros", "São Paulo", "SP", -23.5350, -46.7060));
        schoolRepository.save(school("Colégio Objetivo", "04101-000", "Rua Vergueiro", "3185",
                "Vila Mariana", "São Paulo", "SP", -23.5885, -46.6350));
        schoolRepository.save(school("Colégio Rio Branco", "01236-001", "Rua Rio Branco", "1370",
                "Campos Elíseos", "São Paulo", "SP", -23.5320, -46.6560));
    }

    /**
     * Vincula dependentes ao catálogo de escolas (schoolRef) casando o texto livre histórico
     * (Dependent.school) pelo nome — necessário para o painel de gestão da escola conseguir
     * enxergar os alunos, já que o cadastro de dependente sempre gravou só o nome em texto.
     */
    private void backfillSchoolRefs(SchoolRepository schoolRepository, DependentRepository dependentRepository) {
        Map<String, School> byName = new HashMap<>();
        for (School s : schoolRepository.findAll()) {
            byName.put(s.getName().trim().toLowerCase(), s);
        }
        for (Dependent d : dependentRepository.findAll()) {
            if (d.getSchoolRef() == null && d.getSchool() != null) {
                School match = byName.get(d.getSchool().trim().toLowerCase());
                if (match != null) {
                    d.setSchoolRef(match);
                    dependentRepository.save(d);
                }
            }
        }
    }

    /**
     * Cria a conta de demonstração do painel de gestão (perfil SCHOOL), vinculada ao
     * "Colégio Objetivo" — a escola do catálogo com mais dados reais semeados
     * (transportador + alunos). Idempotente: não faz nada se a conta já existir ou a
     * escola não tiver sido semeada ainda.
     */
    private void seedSchoolAccount(UserRepository userRepository, SchoolRepository schoolRepository,
                                   String passwordHash) {
        String email = "secretaria@objetivo.com.br";
        if (userRepository.existsByEmail(email)) {
            return;
        }
        schoolRepository.findFirstByNameIgnoreCaseAndCep("Colégio Objetivo", "04101-000")
                .filter(school -> school.getUser() == null)
                .ifPresent(school -> {
                    User user = new User();
                    user.setName("Secretaria - Colégio Objetivo");
                    user.setEmail(email);
                    user.setPasswordHash(passwordHash);
                    user.setPhone("(11) 93081-4520");
                    user.setRole(UserRole.SCHOOL);
                    userRepository.save(user);
                    school.setUser(user);
                    schoolRepository.save(school);
                });
    }

    private School school(String name, String cep, String street, String number,
                          String neighborhood, String city, String uf, double lat, double lon) {
        School s = new School();
        s.setName(name);
        s.setCep(cep);
        s.setStreet(street);
        s.setNumber(number);
        s.setNeighborhood(neighborhood);
        s.setCity(city);
        s.setUf(uf);
        s.setLatitude(lat);
        s.setLongitude(lon);
        return s;
    }

    /** Preenche campos novos nos avisos antigos (publishAt, allowComments, priority). */
    private void backfillNotices(NoticeRepository noticeRepository) {
        for (Notice notice : noticeRepository.findAll()) {
            boolean changed = false;
            if (notice.getPublishAt() == null) {
                notice.setPublishAt(notice.getCreatedAt() != null ? notice.getCreatedAt() : Instant.now());
                changed = true;
            }
            if (notice.getAllowComments() == null) {
                notice.setAllowComments(true);
                changed = true;
            }
            if (notice.getPriority() == null) {
                notice.setPriority(NoticePriority.INFO);
                changed = true;
            }
            if (changed) {
                noticeRepository.save(notice);
            }
        }
    }

    /**
     * Padroniza (Title Case, preserva acentos) escolas/bairros já cadastrados antes da
     * normalização entrar em vigor. Idempotente — roda a cada boot, sem custo em bancos já
     * normalizados. Não funde registros diferentes do catálogo de escolas (ids distintos);
     * apenas renomeia. Ver TextNormalization.titleCase.
     */
    private void backfillServiceAreaNormalization(
            TransporterProfileRepository transporterRepository, SchoolRepository schoolRepository) {
        for (TransporterProfile t : transporterRepository.findAll()) {
            Set<String> normalizedSchools = new LinkedHashSet<>();
            for (String s : t.getSchools()) {
                normalizedSchools.add(TextNormalization.titleCase(s));
            }
            Set<String> normalizedNeighborhoods = new LinkedHashSet<>();
            for (String n : t.getNeighborhoods()) {
                normalizedNeighborhoods.add(TextNormalization.titleCase(n));
            }
            boolean changed = !normalizedSchools.equals(t.getSchools())
                    || !normalizedNeighborhoods.equals(t.getNeighborhoods());
            if (changed) {
                t.getSchools().clear();
                t.getSchools().addAll(normalizedSchools);
                t.getNeighborhoods().clear();
                t.getNeighborhoods().addAll(normalizedNeighborhoods);
                transporterRepository.save(t);
            }
        }
        for (School school : schoolRepository.findAll()) {
            String normalized = TextNormalization.titleCase(school.getName());
            if (!normalized.equals(school.getName())) {
                school.setName(normalized);
                schoolRepository.save(school);
            }
        }
    }

    /**
     * Preenche dados desta feature ausentes em bancos já existentes (criados antes
     * dela), sem exigir reset: coordenadas das paradas, posição inicial do
     * transportador e o vínculo parada→dependente. Idempotente: só toca campos nulos.
     */
    private void backfillGeo(TransporterProfileRepository transporterRepository,
                             RouteStopRepository routeStopRepository,
                             DependentRepository dependentRepository) {
        Map<String, double[]> coordsByLabel = Map.of(
                "Lucas Costa", new double[]{-23.5489, -46.6388},
                "Pedro Silva", new double[]{-23.5870, -46.6340},
                "FIAP School", new double[]{-23.5530, -46.6520});

        // Index de dependentes por nome, para reconectar paradas antigas (label == nome).
        Map<String, Dependent> dependentByName = new HashMap<>();
        for (Dependent dependent : dependentRepository.findAll()) {
            dependentByName.putIfAbsent(dependent.getName(), dependent);
        }

        for (RouteStop stop : routeStopRepository.findAll()) {
            boolean changed = false;
            if (stop.getLatitude() == null || stop.getLongitude() == null) {
                double[] coords = coordsByLabel.get(stop.getLabel());
                if (coords != null) {
                    stop.setLatitude(coords[0]);
                    stop.setLongitude(coords[1]);
                    changed = true;
                }
            }
            if (stop.getDependent() == null && stop.getStatus() != RouteStopStatus.SCHOOL) {
                Dependent match = dependentByName.get(stop.getLabel());
                if (match != null) {
                    stop.setDependent(match);
                    changed = true;
                }
            }
            if (changed) {
                routeStopRepository.save(stop);
            }
        }

        for (TransporterProfile transporter : transporterRepository.findAll()) {
            boolean hasStops = !routeStopRepository
                    .findByTransporterIdOrderByPositionAsc(transporter.getId()).isEmpty();
            if (transporter.getCurrentLatitude() == null && hasStops) {
                transporter.setCurrentLatitude(ROBERTO_START_LAT);
                transporter.setCurrentLongitude(ROBERTO_START_LON);
                transporter.setLocationUpdatedAt(Instant.now());
                transporterRepository.save(transporter);
            }
        }
    }
}
