package com.vanbora.api.config;

import com.vanbora.api.modules.chat.domain.Conversation;
import com.vanbora.api.modules.chat.domain.Message;
import com.vanbora.api.modules.chat.repository.ConversationRepository;
import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.modules.enrollment.repository.EnrollmentRepository;
import com.vanbora.api.modules.finance.domain.DailyDistance;
import com.vanbora.api.modules.finance.domain.Expense;
import com.vanbora.api.modules.finance.domain.FuelEntry;
import com.vanbora.api.modules.finance.repository.DailyDistanceRepository;
import com.vanbora.api.modules.finance.repository.ExpenseRepository;
import com.vanbora.api.modules.finance.repository.FuelEntryRepository;
import com.vanbora.api.modules.guardian.domain.Dependent;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.guardian.repository.DependentRepository;
import com.vanbora.api.modules.guardian.repository.GuardianProfileRepository;
import com.vanbora.api.modules.hire.domain.HireRequest;
import com.vanbora.api.modules.hire.repository.HireRequestRepository;
import com.vanbora.api.modules.notice.domain.Notice;
import com.vanbora.api.modules.notice.repository.NoticeRepository;
import com.vanbora.api.modules.payment.domain.Payment;
import com.vanbora.api.modules.route.domain.RouteStop;
import com.vanbora.api.modules.school.domain.School;
import com.vanbora.api.modules.school.repository.SchoolRepository;
import com.vanbora.api.modules.route.repository.RouteStopRepository;
import com.vanbora.api.modules.transporter.domain.Helper;
import com.vanbora.api.modules.transporter.domain.PriceZone;
import com.vanbora.api.modules.transporter.domain.Review;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.modules.transporter.service.ReviewService;
import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.modules.user.repository.UserRepository;
import com.vanbora.api.shared.enums.FinanceStatus;
import com.vanbora.api.shared.enums.HireStatus;
import com.vanbora.api.shared.enums.NoticePriority;
import com.vanbora.api.shared.enums.PaymentStatus;
import com.vanbora.api.shared.enums.RouteStopStatus;
import com.vanbora.api.shared.enums.UserRole;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Popula dados de demonstração na primeira execução (banco vazio),
 * permitindo testar o app de ponta a ponta com logins conhecidos.
 *
 * Logins (senha: 123456):
 *  - Responsável:    mariana@vanbora.com
 *  - Transportador:  roberto@vanbora.com
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
            UserRepository userRepository,
            GuardianProfileRepository guardianRepository,
            TransporterProfileRepository transporterRepository,
            EnrollmentRepository enrollmentRepository,
            DependentRepository dependentRepository,
            NoticeRepository noticeRepository,
            RouteStopRepository routeStopRepository,
            ConversationRepository conversationRepository,
            HireRequestRepository hireRequestRepository,
            ExpenseRepository expenseRepository,
            FuelEntryRepository fuelEntryRepository,
            DailyDistanceRepository dailyDistanceRepository,
            ReviewService reviewService,
            SchoolRepository schoolRepository,
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
                seedSchoolAccount(userRepository, schoolRepository, passwordEncoder.encode(DEFAULT_PASSWORD));
                // Recalcula média/contagem reais a partir das avaliações (corrige valores fixos antigos).
                transporterRepository.findAll().forEach(reviewService::recompute);
                return;
            }
            backfillSchools(schoolRepository);

            String password = passwordEncoder.encode(DEFAULT_PASSWORD);

            // ----- Transportador principal: Roberto -----
            TransporterProfile roberto = buildTransporter(
                    userRepository, password, "Roberto Almeida", "roberto@vanbora.com",
                    "ABC-1D34", "••• ••• 1234", 5, new BigDecimal("4.90"), 124,
                    new BigDecimal("350.00"),
                    Set.of("FIAP School", "Escola Adventista", "COC"),
                    Set.of("Centro", "Vila Nova", "Jd. América"));
            addHelper(roberto, "Carlos Mendes", "Monitor");
            addReview(roberto, "Juliana S.", 5,
                    "Excelente profissional, muito pontual e cuidadoso com as crianças.");
            addReview(roberto, "Marcos T.", 5,
                    "Sempre avisa com antecedência se vai atrasar. Van sempre limpa e organizada.");
            addPriceZone(roberto, "Premium", "FIAP School", new BigDecimal("520.00"),
                    Set.of("Jardins", "Moema"));
            addPriceZone(roberto, "Standard", "FIAP School", new BigDecimal("380.00"),
                    Set.of("Saúde", "Ipiranga"));
            // Posição inicial do transportador (será sobrescrita pelo GPS do app).
            roberto.setCurrentLatitude(ROBERTO_START_LAT);
            roberto.setCurrentLongitude(ROBERTO_START_LON);
            roberto.setLocationUpdatedAt(Instant.now());
            // Planos (mensal já vem do baseMonthlyFee; anual à vista e parcelado com desconto).
            roberto.setAnnualPlanFee(new BigDecimal("3600.00"));
            roberto.setInstallmentMonthlyFee(new BigDecimal("320.00"));
            // Configurações financeiras de demonstração.
            roberto.setMonthlyRevenueGoal(new BigDecimal("5000.00"));
            roberto.setMaintenanceIntervalKm(5000);
            roberto.setLastMaintenanceKm(0.0);
            transporterRepository.save(roberto);

            // ----- Responsáveis -----
            GuardianProfile mariana = buildGuardian(
                    userRepository, password, "Mariana Costa", "mariana@vanbora.com",
                    "São Paulo", "Centro");
            Dependent lucas = addDependent(mariana, "Lucas Costa", "FIAP School");
            // 2º dependente: usado para demonstrar o fluxo de contratação/assinatura.
            Dependent sofia = addDependent(mariana, "Sofia Costa", "FIAP School");
            guardianRepository.save(mariana);

            GuardianProfile juliana = buildGuardian(
                    userRepository, password, "Juliana Silva", "juliana@vanbora.com",
                    "São Paulo", "Vila Mariana");
            Dependent pedro = addDependent(juliana, "Pedro Silva", "Escola Adventista");
            guardianRepository.save(juliana);

            GuardianProfile marcos = buildGuardian(
                    userRepository, password, "Marcos Souza", "marcos@vanbora.com",
                    "São Paulo", "Jd. América");
            Dependent ana = addDependent(marcos, "Ana Souza", "COC");
            guardianRepository.save(marcos);

            // ----- Matrículas (alunos do Roberto) -----
            Enrollment lucasEnrollment = buildEnrollment(roberto, lucas,
                    new BigDecimal("350.00"), FinanceStatus.EM_DIA);
            seedPaymentHistory(lucasEnrollment);
            enrollmentRepository.save(lucasEnrollment);

            Enrollment pedroEnrollment = buildEnrollment(roberto, pedro,
                    new BigDecimal("350.00"), FinanceStatus.PENDENTE);
            enrollmentRepository.save(pedroEnrollment);

            Enrollment anaEnrollment = buildEnrollment(roberto, ana,
                    new BigDecimal("350.00"), FinanceStatus.ATRASADO);
            enrollmentRepository.save(anaEnrollment);

            // ----- Solicitações de contratação pendentes -----
            HireRequest pending = new HireRequest();
            pending.setGuardian(juliana);
            pending.setTransporter(roberto);
            pending.setDependent(pedro);
            pending.setStatus(HireStatus.PENDING);
            hireRequestRepository.save(pending);

            // Mariana → Roberto (Sofia): demonstra o fluxo completo com os 2 logins demo.
            HireRequest marianaPending = new HireRequest();
            marianaPending.setGuardian(mariana);
            marianaPending.setTransporter(roberto);
            marianaPending.setDependent(sofia);
            marianaPending.setStatus(HireStatus.PENDING);
            hireRequestRepository.save(marianaPending);

            // ----- Avisos -----
            noticeRepository.saveAll(List.of(
                    notice(roberto, "Amanhã o veículo sairá 10 min mais cedo devido a obras na "
                            + "avenida principal. Por favor, estejam prontos.", 3),
                    notice(roberto, "A mensalidade de Maio já está disponível para pagamento no "
                            + "aplicativo. Evite juros e pague até o dia 10.", 3),
                    notice(roberto, "Não haverá transporte na próxima sexta-feira (feriado nacional).", 3)));

            // ----- Rota do dia (com coordenadas para o mapa) -----
            routeStopRepository.saveAll(List.of(
                    routeStop(roberto, lucas, "Lucas Costa", "Rua das Flores, 123 - Centro",
                            RouteStopStatus.GOING, 1, -23.5489, -46.6388),
                    routeStop(roberto, pedro, "Pedro Silva", "Av. Brasil, 456 - Vila Mariana",
                            RouteStopStatus.NOT_GOING, 2, -23.5870, -46.6340),
                    routeStop(roberto, null, "FIAP School", "Rua do Parque, 789 - Centro",
                            RouteStopStatus.SCHOOL, 3, -23.5530, -46.6520)));

            // ----- Financeiro de demonstração (Roberto) -----
            expenseRepository.saveAll(List.of(
                    expense(roberto, LocalDate.now().minusDays(2), "Manutenção", "Troca de óleo", "180.00"),
                    expense(roberto, LocalDate.now().minusDays(5), "Pedágio", null, "45.00"),
                    expense(roberto, LocalDate.now().minusDays(8), "Lavagem", "Lava-rápido", "60.00")));
            fuelEntryRepository.saveAll(List.of(
                    fuelEntry(roberto, LocalDate.now().minusDays(1), 42.0, "294.00"),
                    fuelEntry(roberto, LocalDate.now().minusDays(7), 40.0, "276.00")));
            dailyDistanceRepository.saveAll(List.of(
                    dailyDistance(roberto, LocalDate.now(), 38.4),
                    dailyDistance(roberto, LocalDate.now().minusDays(1), 64.2),
                    dailyDistance(roberto, LocalDate.now().minusDays(2), 71.0)));

            // ----- Conversa -----
            Conversation conversation = new Conversation();
            conversation.setGuardian(mariana);
            conversation.setTransporter(roberto);
            addMessage(conversation, roberto.getUser(), "Bom dia! Já estou a caminho, chego em 5 minutos.", 50);
            addMessage(conversation, mariana.getUser(), "Bom dia, Roberto! O Lucas já está descendo.", 48);
            addMessage(conversation, roberto.getUser(), "Já estamos a caminho da escola!", 20);
            conversationRepository.save(conversation);

            // Calcula a média/contagem reais a partir das avaliações semeadas.
            transporterRepository.findAll().forEach(reviewService::recompute);

            // ----- Painel de gestão da escola: vincula os dependentes ao catálogo e cria o login -----
            backfillSchoolRefs(schoolRepository, dependentRepository);
            seedSchoolAccount(userRepository, schoolRepository, password);
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
     * Cria a conta de demonstração do painel de gestão (perfil SCHOOL), vinculada à "FIAP
     * School" — a escola do catálogo com mais dados reais semeados (transportador + alunos).
     * Idempotente: não faz nada se a conta já existir ou a escola não tiver sido semeada ainda.
     */
    private void seedSchoolAccount(UserRepository userRepository, SchoolRepository schoolRepository,
                                   String passwordHash) {
        String email = "secretaria@fiapschool.com.br";
        if (userRepository.existsByEmail(email)) {
            return;
        }
        schoolRepository.findFirstByNameIgnoreCaseAndCep("FIAP School", "01310-100")
                .filter(school -> school.getUser() == null)
                .ifPresent(school -> {
                    User user = new User();
                    user.setName("Secretaria - FIAP School");
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

    // ----- Helpers de construção -----

    private TransporterProfile buildTransporter(
            UserRepository userRepository, String password, String name, String email,
            String plate, String cnh, int years, BigDecimal rating, int reviews,
            BigDecimal baseFee, Set<String> schools, Set<String> neighborhoods) {
        User user = userRepository.save(user(name, email, password, UserRole.TRANSPORTER));
        TransporterProfile t = new TransporterProfile();
        t.setUser(user);
        t.setPlate(plate);
        t.setCnh(cnh);
        t.setDocument("000.000.000-00");
        t.setYearsExperience(years);
        t.setRatingAvg(rating);
        t.setReviewsCount(reviews);
        t.setBaseMonthlyFee(baseFee);
        t.getSchools().addAll(schools);
        t.getNeighborhoods().addAll(neighborhoods);
        return t;
    }

    private GuardianProfile buildGuardian(
            UserRepository userRepository, String password, String name, String email,
            String city, String neighborhood) {
        User user = userRepository.save(user(name, email, password, UserRole.GUARDIAN));
        GuardianProfile g = new GuardianProfile();
        g.setUser(user);
        g.setCity(city);
        g.setNeighborhood(neighborhood);
        return g;
    }

    private User user(String name, String email, String passwordHash, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setPhone("(11) 90000-0000");
        user.setRole(role);
        return user;
    }

    private void addHelper(TransporterProfile t, String name, String role) {
        Helper helper = new Helper();
        helper.setTransporter(t);
        helper.setName(name);
        helper.setRole(role);
        t.getHelpers().add(helper);
    }

    private void addReview(TransporterProfile t, String author, int rating, String comment) {
        Review review = new Review();
        review.setTransporter(t);
        review.setAuthorName(author);
        review.setRating(rating);
        review.setComment(comment);
        t.getReviews().add(review);
    }

    private void addPriceZone(TransporterProfile t, String name, String school,
                              BigDecimal fee, Set<String> neighborhoods) {
        PriceZone zone = new PriceZone();
        zone.setTransporter(t);
        zone.setName(name);
        zone.setSchool(school);
        zone.setMonthlyFee(fee);
        zone.getNeighborhoods().addAll(neighborhoods);
        t.getPriceZones().add(zone);
    }

    private Dependent addDependent(GuardianProfile guardian, String name, String school) {
        Dependent dependent = new Dependent();
        dependent.setName(name);
        dependent.setSchool(school);
        guardian.addDependent(dependent);
        return dependent;
    }

    private Enrollment buildEnrollment(TransporterProfile transporter, Dependent dependent,
                                       BigDecimal fee, FinanceStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setTransporter(transporter);
        enrollment.setDependent(dependent);
        enrollment.setMonthlyFee(fee);
        enrollment.setFinanceStatus(status);
        enrollment.setActive(true);
        return enrollment;
    }

    private void seedPaymentHistory(Enrollment enrollment) {
        addPayment(enrollment, "2025-12", new BigDecimal("350.00"), PaymentStatus.PAID, null);
        addPayment(enrollment, "2026-01", new BigDecimal("175.00"), PaymentStatus.PAID, null);
        addPayment(enrollment, "2026-02", new BigDecimal("350.00"), PaymentStatus.PAID, null);
        addPayment(enrollment, "2026-03", new BigDecimal("350.00"), PaymentStatus.PAID, null);
        addPayment(enrollment, "2026-04", new BigDecimal("350.00"), PaymentStatus.PAID, null);
        addPayment(enrollment, "2026-05", new BigDecimal("350.00"), PaymentStatus.PENDING,
                LocalDate.of(2026, 5, 10));
    }

    private void addPayment(Enrollment enrollment, String month, BigDecimal amount,
                            PaymentStatus status, LocalDate dueDate) {
        Payment payment = new Payment();
        payment.setEnrollment(enrollment);
        payment.setReferenceMonth(month);
        payment.setAmount(amount);
        payment.setStatus(status);
        payment.setDueDate(dueDate);
        if (status == PaymentStatus.PAID) {
            payment.setPaidAt(LocalDate.parse(month + "-05"));
        }
        enrollment.getPayments().add(payment);
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

    private Expense expense(TransporterProfile transporter, LocalDate date, String category,
                            String description, String amount) {
        Expense expense = new Expense();
        expense.setTransporter(transporter);
        expense.setDate(date);
        expense.setCategory(category);
        expense.setDescription(description);
        expense.setAmount(new BigDecimal(amount));
        return expense;
    }

    private FuelEntry fuelEntry(TransporterProfile transporter, LocalDate date, double liters, String amount) {
        FuelEntry entry = new FuelEntry();
        entry.setTransporter(transporter);
        entry.setDate(date);
        entry.setLiters(liters);
        entry.setAmount(new BigDecimal(amount));
        return entry;
    }

    private DailyDistance dailyDistance(TransporterProfile transporter, LocalDate date, double km) {
        DailyDistance distance = new DailyDistance();
        distance.setTransporter(transporter);
        distance.setDate(date);
        distance.setKm(km);
        return distance;
    }

    private Notice notice(TransporterProfile transporter, String message, int recipients) {
        Notice notice = new Notice();
        notice.setTransporter(transporter);
        notice.setMessage(message);
        notice.setRecipientsCount(recipients);
        notice.setPublishAt(Instant.now()); // já publicado (visível aos pais)
        notice.setAllowComments(true);
        notice.setPriority(NoticePriority.INFO);
        notice.setAudienceAll(true);
        return notice;
    }

    private RouteStop routeStop(TransporterProfile transporter, Dependent dependent, String label,
                                String address, RouteStopStatus status, int position,
                                double latitude, double longitude) {
        RouteStop stop = new RouteStop();
        stop.setTransporter(transporter);
        stop.setDependent(dependent);
        stop.setLabel(label);
        stop.setAddress(address);
        stop.setStatus(status);
        stop.setPosition(position);
        stop.setLatitude(latitude);
        stop.setLongitude(longitude);
        return stop;
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

    private void addMessage(Conversation conversation, User sender, String text, long minutesAgo) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setText(text);
        message.setSentAt(Instant.now().minus(minutesAgo, ChronoUnit.MINUTES));
        conversation.getMessages().add(message);
    }
}
