package com.vanbora.api.config;

import com.vanbora.api.modules.chat.domain.Conversation;
import com.vanbora.api.modules.chat.domain.Message;
import com.vanbora.api.modules.chat.repository.ConversationRepository;
import com.vanbora.api.modules.enrollment.domain.Absence;
import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.modules.enrollment.repository.EnrollmentRepository;
import com.vanbora.api.modules.finance.domain.DailyDistance;
import com.vanbora.api.modules.finance.domain.Expense;
import com.vanbora.api.modules.finance.domain.FuelEntry;
import com.vanbora.api.modules.finance.domain.ManualRevenue;
import com.vanbora.api.modules.finance.repository.DailyDistanceRepository;
import com.vanbora.api.modules.finance.repository.ExpenseRepository;
import com.vanbora.api.modules.finance.repository.FuelEntryRepository;
import com.vanbora.api.modules.finance.repository.ManualRevenueRepository;
import com.vanbora.api.modules.guardian.domain.Dependent;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.guardian.repository.GuardianProfileRepository;
import com.vanbora.api.modules.hire.domain.Contract;
import com.vanbora.api.modules.hire.domain.HireRequest;
import com.vanbora.api.modules.hire.repository.ContractRepository;
import com.vanbora.api.modules.hire.repository.HireRequestRepository;
import com.vanbora.api.modules.hire.service.ContractTextBuilder;
import com.vanbora.api.modules.notice.domain.Notice;
import com.vanbora.api.modules.notice.domain.NoticeComment;
import com.vanbora.api.modules.notice.domain.NoticeReaction;
import com.vanbora.api.modules.notice.repository.NoticeCommentRepository;
import com.vanbora.api.modules.notice.repository.NoticeReactionRepository;
import com.vanbora.api.modules.notice.repository.NoticeRepository;
import com.vanbora.api.modules.payment.domain.Payment;
import com.vanbora.api.modules.payment.domain.PaymentMethod;
import com.vanbora.api.modules.payment.repository.PaymentMethodRepository;
import com.vanbora.api.modules.route.domain.RouteStop;
import com.vanbora.api.modules.route.repository.RouteStopRepository;
import com.vanbora.api.modules.school.domain.School;
import com.vanbora.api.modules.school.repository.SchoolRepository;
import com.vanbora.api.modules.transporter.domain.Helper;
import com.vanbora.api.modules.transporter.domain.LocationShareWindow;
import com.vanbora.api.modules.transporter.domain.PriceZone;
import com.vanbora.api.modules.transporter.domain.Review;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.domain.VehicleAccessibilityFeature;
import com.vanbora.api.modules.transporter.domain.VehicleCharacteristic;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.modules.user.domain.OnboardingProgress;
import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.modules.user.domain.UserConsent;
import com.vanbora.api.modules.user.repository.OnboardingProgressRepository;
import com.vanbora.api.modules.user.repository.UserConsentRepository;
import com.vanbora.api.modules.user.repository.UserRepository;
import com.vanbora.api.shared.enums.ConsentType;
import com.vanbora.api.shared.enums.ContractStatus;
import com.vanbora.api.shared.enums.FinanceStatus;
import com.vanbora.api.shared.enums.HireStatus;
import com.vanbora.api.shared.enums.MessageType;
import com.vanbora.api.shared.enums.NoticePriority;
import com.vanbora.api.shared.enums.PaymentKind;
import com.vanbora.api.shared.enums.PaymentMethodType;
import com.vanbora.api.shared.enums.PaymentStatus;
import com.vanbora.api.shared.enums.PlanType;
import com.vanbora.api.shared.enums.RouteStopStatus;
import com.vanbora.api.shared.enums.UserRole;
import com.vanbora.api.shared.legal.LegalDocuments;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Conjunto de dados de demonstração do VanBora: transportadores, responsáveis,
 * dependentes, rota do dia, conversas, financeiro, avisos e contratação.
 *
 * <p>Foi desenhado para que <b>toda</b> tela do app tenha conteúdo real ao gravar
 * uma demonstração — inclusive fotos, que apontam para {@code /uploads/seed/…}
 * (baixadas por {@code database/download_seed_media.ps1}).
 *
 * <p>Roda uma única vez, em banco vazio, a partir de {@link DataSeeder}. Todas as
 * datas são relativas a "hoje", então a demonstração nunca fica desatualizada.
 */
@Component
public class DemoDataSeeder {

    /** Prefixo público das imagens de demonstração (ver StorageService/WebConfig). */
    private static final String MEDIA = "/uploads/seed/";

    /** Posição atual da van do Roberto: em rota, no Ipiranga, subindo para a Vila Mariana. */
    private static final double ROBERTO_LAT = -23.5975;
    private static final double ROBERTO_LON = -46.6120;

    private final UserRepository userRepository;
    private final GuardianProfileRepository guardianRepository;
    private final TransporterProfileRepository transporterRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NoticeRepository noticeRepository;
    private final NoticeCommentRepository noticeCommentRepository;
    private final NoticeReactionRepository noticeReactionRepository;
    private final RouteStopRepository routeStopRepository;
    private final ConversationRepository conversationRepository;
    private final HireRequestRepository hireRequestRepository;
    private final ContractRepository contractRepository;
    private final ContractTextBuilder contractTextBuilder;
    private final PaymentMethodRepository paymentMethodRepository;
    private final ExpenseRepository expenseRepository;
    private final FuelEntryRepository fuelEntryRepository;
    private final DailyDistanceRepository dailyDistanceRepository;
    private final ManualRevenueRepository manualRevenueRepository;
    private final SchoolRepository schoolRepository;
    private final OnboardingProgressRepository onboardingProgressRepository;
    private final UserConsentRepository userConsentRepository;

    public DemoDataSeeder(UserRepository userRepository,
                          GuardianProfileRepository guardianRepository,
                          TransporterProfileRepository transporterRepository,
                          EnrollmentRepository enrollmentRepository,
                          NoticeRepository noticeRepository,
                          NoticeCommentRepository noticeCommentRepository,
                          NoticeReactionRepository noticeReactionRepository,
                          RouteStopRepository routeStopRepository,
                          ConversationRepository conversationRepository,
                          HireRequestRepository hireRequestRepository,
                          ContractRepository contractRepository,
                          ContractTextBuilder contractTextBuilder,
                          PaymentMethodRepository paymentMethodRepository,
                          ExpenseRepository expenseRepository,
                          FuelEntryRepository fuelEntryRepository,
                          DailyDistanceRepository dailyDistanceRepository,
                          ManualRevenueRepository manualRevenueRepository,
                          SchoolRepository schoolRepository,
                          OnboardingProgressRepository onboardingProgressRepository,
                          UserConsentRepository userConsentRepository) {
        this.userRepository = userRepository;
        this.guardianRepository = guardianRepository;
        this.transporterRepository = transporterRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.noticeRepository = noticeRepository;
        this.noticeCommentRepository = noticeCommentRepository;
        this.noticeReactionRepository = noticeReactionRepository;
        this.routeStopRepository = routeStopRepository;
        this.conversationRepository = conversationRepository;
        this.hireRequestRepository = hireRequestRepository;
        this.contractRepository = contractRepository;
        this.contractTextBuilder = contractTextBuilder;
        this.paymentMethodRepository = paymentMethodRepository;
        this.expenseRepository = expenseRepository;
        this.fuelEntryRepository = fuelEntryRepository;
        this.dailyDistanceRepository = dailyDistanceRepository;
        this.manualRevenueRepository = manualRevenueRepository;
        this.schoolRepository = schoolRepository;
        this.onboardingProgressRepository = onboardingProgressRepository;
        this.userConsentRepository = userConsentRepository;
    }

    // ================================================================= seed ===

    @Transactional
    public void seed(String passwordHash) {
        // ---------- Transportadores ----------
        TransporterProfile roberto = seedRoberto(passwordHash);
        TransporterProfile fernanda = seedFernanda(passwordHash);
        // Concorrência da tela "Comparar e escolher": preços, notas e diferenciais distintos.
        seedCarlos(passwordHash);
        seedPatricia(passwordHash);
        seedAnderson(passwordHash);
        seedSimone(passwordHash);

        // ---------- Responsáveis, dependentes e endereços ----------
        GuardianProfile mariana = guardian(passwordHash, "Mariana Costa", "mariana@vanbora.com",
                "(11) 98145-2233", "guardian-mariana.jpg", "384.512.908-11",
                "Mãe do Lucas, da Sofia e do Miguel. Trabalho no centro e conto com a van "
                        + "para levar e buscar as crianças com segurança.",
                "04010-100", "Rua Domingos de Morais", "1250", "Vila Mariana",
                -23.5895, -46.6390);
        Dependent lucas = dependent(mariana, "Lucas Costa", "Colégio Objetivo", "kid-lucas.png");
        Dependent sofia = dependent(mariana, "Sofia Costa", "Colégio Objetivo", "kid-sofia.png");
        // Sem transportador: é com ele que se demonstra a busca + contratação do zero.
        dependent(mariana, "Miguel Costa", "Colégio Objetivo", "kid-miguel.png");
        guardianRepository.save(mariana);

        GuardianProfile juliana = guardian(passwordHash, "Juliana Silva", "juliana@vanbora.com",
                "(11) 99632-8814", "guardian-juliana.jpg", "271.884.336-40",
                "Mãe do Pedro e da Beatriz. Prefiro receber aviso sempre que a van atrasar.",
                "04120-020", "Rua Luís Góis", "890", "Saúde",
                -23.6120, -46.6360);
        Dependent pedro = dependent(juliana, "Pedro Silva", "Colégio Objetivo", "kid-pedro.png");
        Dependent beatriz = dependent(juliana, "Beatriz Silva", "Colégio Objetivo", "kid-beatriz.png");
        guardianRepository.save(juliana);

        GuardianProfile marcos = guardian(passwordHash, "Marcos Souza", "marcos@vanbora.com",
                "(11) 97744-1096", "guardian-marcos.jpg", "509.223.741-88",
                "Pai da Ana. Moro no Ipiranga e trabalho em escala, então a van resolve minha vida.",
                "04202-000", "Rua Bom Pastor", "1430", "Ipiranga",
                -23.5930, -46.6100);
        Dependent ana = dependent(marcos, "Ana Souza", "Colégio Objetivo", "kid-ana.png");
        guardianRepository.save(marcos);

        GuardianProfile renata = guardian(passwordHash, "Renata Alves", "renata@vanbora.com",
                "(11) 98330-7712", "guardian-renata.jpg", "146.907.552-23",
                "Mãe da Helena. Acompanho o trajeto pelo mapa todos os dias.",
                "04090-001", "Alameda dos Nhambiquaras", "620", "Moema",
                -23.6060, -46.6650);
        Dependent helena = dependent(renata, "Helena Alves", "Colégio Objetivo", "kid-helena.png");
        guardianRepository.save(renata);

        GuardianProfile thiago = guardian(passwordHash, "Thiago Ramos", "thiago@vanbora.com",
                "(11) 99120-4458", "guardian-thiago.jpg", "832.415.660-07",
                "Pai do Gabriel e do Théo. Procuro transporte com monitor no veículo.",
                "01419-002", "Alameda Santos", "1500", "Jardim Paulista",
                -23.5665, -46.6530);
        Dependent gabriel = dependent(thiago, "Gabriel Ramos", "Colégio Objetivo", "kid-gabriel.png");
        Dependent theo = dependent(thiago, "Théo Ramos", "Colégio Rio Branco", "kid-theo.png");
        guardianRepository.save(thiago);

        GuardianProfile camila = guardian(passwordHash, "Camila Duarte", "camila@vanbora.com",
                "(11) 98007-6621", "guardian-camila.jpg", "620.338.194-52",
                "Mãe do Enzo e da Laura. Os dois estudam em escolas diferentes.",
                "01327-000", "Rua Treze de Maio", "700", "Bela Vista",
                -23.5605, -46.6420);
        Dependent enzo = dependent(camila, "Enzo Duarte", "Colégio Objetivo", "kid-enzo.png");
        Dependent laura = dependent(camila, "Laura Duarte", "Colégio Bandeirantes", "kid-laura.png");
        guardianRepository.save(camila);

        linkSchoolRefs();

        // ---------- Matrículas do Roberto (6 alunos) ----------
        Enrollment eLucas = enrollment(roberto, lucas, "380.00", FinanceStatus.EM_DIA);
        Enrollment ePedro = enrollment(roberto, pedro, "380.00", FinanceStatus.PENDENTE);
        Enrollment eAna = enrollment(roberto, ana, "380.00", FinanceStatus.ATRASADO);
        Enrollment eHelena = enrollment(roberto, helena, "480.00", FinanceStatus.EM_DIA);
        Enrollment eGabriel = enrollment(roberto, gabriel, "520.00", FinanceStatus.EM_DIA);
        Enrollment eEnzo = enrollment(roberto, enzo, "520.00", FinanceStatus.EM_DIA);
        // Matrícula da Fernanda, para o perfil dela não ficar vazio.
        Enrollment eLaura = enrollment(fernanda, laura, "440.00", FinanceStatus.EM_DIA);

        // Histórico de mensalidades (7 meses corridos até o mês atual).
        payments(eLucas, "380.00", PaymentStatus.PAID, 10, 6);
        payments(ePedro, "380.00", PaymentStatus.PENDING, 10, null);
        payments(eAna, "380.00", PaymentStatus.OVERDUE, 5, null);
        payments(eHelena, "480.00", PaymentStatus.PAID, 10, 8);
        payments(eGabriel, "520.00", PaymentStatus.PAID, 10, 4);
        payments(eEnzo, "520.00", PaymentStatus.PAID, 10, 7);
        payments(eLaura, "440.00", PaymentStatus.PAID, 10, 5);

        enrollmentRepository.saveAll(List.of(eLucas, ePedro, eAna, eHelena, eGabriel, eEnzo, eLaura));

        // A Ana avisou falta hoje — a rota e o rastreamento mostram "não vai hoje".
        Absence absence = new Absence();
        absence.setEnrollment(eAna);
        absence.setDate(LocalDate.now());
        eAna.getAbsences().add(absence);
        enrollmentRepository.save(eAna);

        // ---------- Rota do dia do Roberto ----------
        seedRoute(roberto, ana, pedro, helena, lucas, gabriel, enzo);

        // Rota mínima da Fernanda (para o mapa dela também abrir).
        routeStopRepository.saveAll(List.of(
                stop(fernanda, laura, "Laura Duarte", "Rua Treze de Maio, 700 - Bela Vista",
                        RouteStopStatus.GOING, 1, -23.5605, -46.6420),
                stop(fernanda, null, "Colégio Bandeirantes", "Rua Estela, 268 - Vila Mariana",
                        RouteStopStatus.SCHOOL, 2, -23.5896, -46.6378)));

        // ---------- Financeiro do Roberto ----------
        seedFinance(roberto);
        seedFinanceLight(fernanda);

        // ---------- Avisos ----------
        seedNotices(roberto, mariana, juliana, renata);

        // ---------- Conversas ----------
        seedConversations(roberto, fernanda, mariana, juliana, marcos, renata, thiago, camila);

        // ---------- Contratação ----------
        seedHiring(roberto, mariana, sofia, juliana, beatriz, thiago, theo);

        // ---------- Meios de pagamento salvos ----------
        paymentMethod(mariana, PaymentMethodType.CREDIT_CARD, "Visa", "4242");
        paymentMethod(mariana, PaymentMethodType.AUTO_DEBIT, "Itaú", "0913");
        paymentMethod(juliana, PaymentMethodType.CREDIT_CARD, "Mastercard", "5518");
        paymentMethod(camila, PaymentMethodType.CREDIT_CARD, "Elo", "7701");

        // ---------- Consentimento (LGPD) e onboarding já visto ----------
        for (User user : userRepository.findAll()) {
            consent(user);
            onboardingSeen(user);
        }
    }

    // ====================================================== transportadores ===

    private TransporterProfile seedRoberto(String passwordHash) {
        TransporterProfile t = transporter(passwordHash, "Roberto Almeida", "roberto@vanbora.com",
                "(11) 98812-4477", "transporter-roberto.jpg", "ABC-1D34", "••• ••• 1234",
                "187.442.318-05", 8, "380.00",
                "Motorista escolar há 8 anos, sempre na região da Vila Mariana e da Paulista. "
                        + "Van revisada todo mês, monitora a bordo em todos os trajetos e aviso no "
                        + "aplicativo sempre que houver qualquer imprevisto no caminho.",
                Set.of("Colégio Objetivo", "Colégio Rio Branco", "Colégio Bandeirantes"),
                Set.of("Vila Mariana", "Saúde", "Ipiranga", "Moema", "Jardim Paulista",
                        "Bela Vista", "Paraíso"),
                List.of("van-roberto-1.jpg", "van-roberto-2.jpg", "van-roberto-3.jpg"));
        t.setAnnualPlanFee(new BigDecimal("4104.00"));
        t.setInstallmentMonthlyFee(new BigDecimal("342.00"));
        t.setAcceptsProposals(true);
        t.setCurrentLatitude(ROBERTO_LAT);
        t.setCurrentLongitude(ROBERTO_LON);
        t.setCurrentHeading(335.0);
        t.setLocationUpdatedAt(Instant.now().minus(40, ChronoUnit.SECONDS));
        t.setLocationSharingEnabled(true);
        t.setMonthlyRevenueGoal(new BigDecimal("2600.00"));
        t.setMaintenanceIntervalKm(5000);
        t.setLastMaintenanceKm(1200.0);
        t.getVehicleCharacteristics().addAll(Set.of(
                VehicleCharacteristic.AIR_CONDITIONING,
                VehicleCharacteristic.SEATBELT_ALL_SEATS,
                VehicleCharacteristic.FREQUENT_SANITIZATION,
                VehicleCharacteristic.CHILD_LOCK_WINDOWS,
                VehicleCharacteristic.COMFORTABLE_SEATS,
                VehicleCharacteristic.CHILD_SEAT));
        t.getVehicleAccessibilityFeatures().addAll(Set.of(
                VehicleAccessibilityFeature.TRANSPORTS_STUDENTS_WITH_DISABILITY,
                VehicleAccessibilityFeature.WHEELCHAIR_SPACE));
        helper(t, "Carlos Mendes", "Monitor", "helper-carlos-mendes.jpg");
        helper(t, "Lúcia Ferreira", "Monitora", "helper-lucia-ferreira.jpg");
        priceZone(t, "Premium", "Colégio Objetivo", "480.00", "5184.00", "432.00",
                Set.of("Moema", "Jardim Paulista"));
        priceZone(t, "Standard", "Colégio Objetivo", "380.00", "4104.00", "342.00",
                Set.of("Vila Mariana", "Saúde", "Ipiranga"));
        priceZone(t, "Centro", "Colégio Rio Branco", "520.00", "5616.00", "468.00",
                Set.of("Bela Vista", "Paraíso"));
        shareWindow(t, DayOfWeek.MONDAY, DayOfWeek.FRIDAY, LocalTime.of(6, 0), LocalTime.of(8, 30));
        shareWindow(t, DayOfWeek.MONDAY, DayOfWeek.FRIDAY, LocalTime.of(11, 30), LocalTime.of(13, 30));
        review(t, "Juliana S.", 5, "Excelente profissional, muito pontual e cuidadoso com as crianças.");
        review(t, "Marcos T.", 5, "Sempre avisa com antecedência se vai atrasar. Van sempre limpa e organizada.");
        review(t, "Renata A.", 5, "A Helena adora ir na van. O Roberto e a monitora tratam todo mundo muito bem.");
        review(t, "Thiago R.", 5, "Contratei pelo aplicativo em dois dias. Recomendo demais.");
        review(t, "Camila D.", 4, "Muito bom. Só peço para avisar antes quando muda o horário da volta.");
        review(t, "Paulo H.", 5, "Três anos com ele e nunca tive um problema sequer.");
        review(t, "Fernanda B.", 5, "Cinto em todos os bancos e ar-condicionado. Vale cada centavo.");
        review(t, "Cláudia M.", 4, "Atencioso e organizado. Poderia ter uma opção de rota mais cedo.");
        review(t, "Rodrigo L.", 5, "Meu filho tem TEA e o Roberto tem uma paciência enorme com ele.");
        review(t, "Sandra P.", 5, "Recebo a localização em tempo real, isso me deu uma tranquilidade enorme.");
        return transporterRepository.save(t);
    }

    private TransporterProfile seedFernanda(String passwordHash) {
        TransporterProfile t = transporter(passwordHash, "Fernanda Lima", "fernanda@vanbora.com",
                "(11) 99274-3310", "transporter-fernanda.jpg", "FGH-4J21", "••• ••• 8877",
                "302.556.887-19", 12, "420.00",
                "12 anos levando crianças na zona sul. Van com ar-condicionado, cinto em todos "
                        + "os bancos e monitora fixa. Atendo Moema, Campo Belo e Vila Mariana.",
                Set.of("Colégio Objetivo", "Colégio Bandeirantes", "Colégio Santa Cruz"),
                Set.of("Moema", "Campo Belo", "Vila Mariana", "Saúde"),
                List.of("van-fernanda-1.jpg", "van-fernanda-2.jpg"));
        t.setAnnualPlanFee(new BigDecimal("4536.00"));
        t.setInstallmentMonthlyFee(new BigDecimal("378.00"));
        t.setAcceptsProposals(true);
        t.setCurrentLatitude(-23.6045);
        t.setCurrentLongitude(-46.6640);
        t.setLocationUpdatedAt(Instant.now().minus(3, ChronoUnit.MINUTES));
        t.setLocationSharingEnabled(true);
        t.setMonthlyRevenueGoal(new BigDecimal("3200.00"));
        t.setMaintenanceIntervalKm(6000);
        t.setLastMaintenanceKm(0.0);
        t.getVehicleCharacteristics().addAll(Set.of(
                VehicleCharacteristic.AIR_CONDITIONING,
                VehicleCharacteristic.SEATBELT_ALL_SEATS,
                VehicleCharacteristic.FREQUENT_SANITIZATION,
                VehicleCharacteristic.SOUND_SYSTEM));
        helper(t, "Joana Prado", "Monitora", "helper-joana-prado.jpg");
        priceZone(t, "Zona Sul", "Colégio Objetivo", "420.00", "4536.00", "378.00",
                Set.of("Moema", "Campo Belo"));
        review(t, "Camila D.", 5, "A Laura pegou amor pela Fernanda logo na primeira semana.");
        review(t, "Beatriz N.", 5, "Pontualidade impecável, todos os dias no mesmo horário.");
        review(t, "Otávio S.", 5, "Ótima comunicação com os pais pelo aplicativo.");
        review(t, "Larissa F.", 4, "Muito boa. Só é um pouco mais cara que a média do bairro.");
        review(t, "Andréa V.", 5, "Monitora sempre presente, isso faz toda a diferença.");
        review(t, "Gustavo R.", 5, "Recomendo de olhos fechados.");
        return transporterRepository.save(t);
    }

    private TransporterProfile seedCarlos(String passwordHash) {
        TransporterProfile t = transporter(passwordHash, "Carlos Eduardo Pires", "carlos@vanbora.com",
                "(11) 97188-5540", "transporter-carlos.jpg", "KLM-7P08", "••• ••• 3391",
                "455.120.874-33", 6, "340.00",
                "Trabalho com transporte escolar desde 2020, com foco em preço justo. "
                        + "Rotas no Ipiranga, Sacomã e Vila Prudente.",
                Set.of("Colégio Objetivo", "Escola Adventista"),
                Set.of("Ipiranga", "Sacomã", "Vila Prudente", "Vila Mariana"),
                List.of("van-carlos-1.jpg", "van-carlos-2.jpg"));
        t.setInstallmentMonthlyFee(new BigDecimal("306.00"));
        t.setAcceptsProposals(true);
        t.setCurrentLatitude(-23.5905);
        t.setCurrentLongitude(-46.6050);
        t.setLocationUpdatedAt(Instant.now().minus(11, ChronoUnit.MINUTES));
        t.getVehicleCharacteristics().addAll(Set.of(
                VehicleCharacteristic.SEATBELT_ALL_SEATS,
                VehicleCharacteristic.AIR_CONDITIONING));
        priceZone(t, "Padrão", "Colégio Objetivo", "340.00", null, "306.00",
                Set.of("Ipiranga", "Sacomã"));
        review(t, "Marcelo A.", 5, "Preço honesto e serviço bom. Nunca atrasou comigo.");
        review(t, "Denise C.", 4, "Bom atendimento, van um pouco antiga mas bem conservada.");
        review(t, "Ricardo M.", 5, "Resolveu meu problema em cima da hora, muito solícito.");
        review(t, "Vanessa L.", 4, "Cumpre o combinado. Recomendo para quem busca custo-benefício.");
        review(t, "Elaine S.", 5, "Meu filho gosta muito dele.");
        return transporterRepository.save(t);
    }

    private TransporterProfile seedPatricia(String passwordHash) {
        TransporterProfile t = transporter(passwordHash, "Patrícia Nogueira", "patricia@vanbora.com",
                "(11) 98455-2277", "transporter-patricia.jpg", "QRS-9T14", "••• ••• 5502",
                "778.031.229-64", 15, "495.00",
                "Van adaptada para cadeira de rodas, com plataforma elevatória e monitora "
                        + "treinada em primeiros socorros. Atendo alunos com deficiência há 15 anos.",
                Set.of("Colégio Rio Branco", "Colégio Santa Cruz", "Colégio Objetivo"),
                Set.of("Jardim Paulista", "Bela Vista", "Pinheiros", "Alto de Pinheiros"),
                List.of("van-patricia-1.jpg", "van-patricia-2.jpg"));
        t.setAnnualPlanFee(new BigDecimal("5346.00"));
        t.setInstallmentMonthlyFee(new BigDecimal("445.50"));
        t.setCurrentLatitude(-23.5640);
        t.setCurrentLongitude(-46.6720);
        t.setLocationUpdatedAt(Instant.now().minus(6, ChronoUnit.MINUTES));
        t.getVehicleCharacteristics().addAll(Set.of(
                VehicleCharacteristic.AIR_CONDITIONING,
                VehicleCharacteristic.SEATBELT_ALL_SEATS,
                VehicleCharacteristic.COMFORTABLE_SEATS,
                VehicleCharacteristic.FREQUENT_SANITIZATION,
                VehicleCharacteristic.CHILD_LOCK_WINDOWS));
        t.getVehicleAccessibilityFeatures().addAll(Set.of(
                VehicleAccessibilityFeature.WHEELCHAIR_ADAPTED,
                VehicleAccessibilityFeature.WHEELCHAIR_SPACE,
                VehicleAccessibilityFeature.ACCESSIBLE_BOARDING_EQUIPMENT,
                VehicleAccessibilityFeature.TRANSPORTS_STUDENTS_WITH_DISABILITY));
        priceZone(t, "Adaptada", "Colégio Rio Branco", "495.00", "5346.00", "445.50",
                Set.of("Jardim Paulista", "Bela Vista"));
        review(t, "Mônica T.", 5, "A única da região com van realmente adaptada. Salvou nossa rotina.");
        review(t, "Eduardo P.", 5, "Preparo técnico impressionante, e um carinho enorme com as crianças.");
        review(t, "Silvia R.", 5, "Vale cada real. Segurança total.");
        review(t, "Jorge A.", 5, "Profissionalismo raro. Indico para todas as famílias que conheço.");
        return transporterRepository.save(t);
    }

    private TransporterProfile seedAnderson(String passwordHash) {
        TransporterProfile t = transporter(passwordHash, "Anderson Rocha", "anderson@vanbora.com",
                "(11) 99801-3364", "transporter-anderson.jpg", "UVW-2X66", "••• ••• 7743",
                "213.667.940-71", 3, "290.00",
                "Comecei em 2023 e já tenho a agenda quase cheia. Van 2022, ar-condicionado "
                        + "e o menor preço da Saúde e do Jabaquara.",
                Set.of("Colégio Objetivo", "Escola Adventista"),
                Set.of("Saúde", "Jabaquara", "Vila Mariana"),
                List.of("van-anderson-1.jpg", "van-anderson-2.jpg"));
        t.setInstallmentMonthlyFee(new BigDecimal("261.00"));
        t.setAcceptsProposals(true);
        t.setCurrentLatitude(-23.6210);
        t.setCurrentLongitude(-46.6415);
        t.setLocationUpdatedAt(Instant.now().minus(22, ChronoUnit.MINUTES));
        t.getVehicleCharacteristics().addAll(Set.of(
                VehicleCharacteristic.AIR_CONDITIONING,
                VehicleCharacteristic.SEATBELT_ALL_SEATS,
                VehicleCharacteristic.TV));
        helper(t, "Rafael Dias", "Monitor", "helper-rafael-dias.jpg");
        priceZone(t, "Econômica", "Colégio Objetivo", "290.00", null, "261.00",
                Set.of("Saúde", "Jabaquara"));
        review(t, "Tatiana B.", 5, "Melhor preço que encontrei e o serviço é muito bom.");
        review(t, "Wagner F.", 4, "Novo no ramo, mas muito responsável.");
        review(t, "Priscila O.", 4, "Bom. Às vezes atrasa uns minutos na volta.");
        review(t, "Hugo N.", 5, "Van nova e limpa, meu filho adora a TV.");
        return transporterRepository.save(t);
    }

    private TransporterProfile seedSimone(String passwordHash) {
        TransporterProfile t = transporter(passwordHash, "Simone Barbosa", "simone@vanbora.com",
                "(11) 98670-1129", "transporter-simone.jpg", "YZA-5B93", "••• ••• 2214",
                "664.902.157-38", 9, "450.00",
                "Atendo somente meio período da manhã, com no máximo 12 alunos por van "
                        + "para garantir conforto. Rota fixa Paulista / Consolação / Higienópolis.",
                Set.of("Colégio Rio Branco", "Colégio Bandeirantes"),
                Set.of("Bela Vista", "Consolação", "Higienópolis", "Jardim Paulista"),
                List.of("van-simone-1.jpg", "van-simone-2.jpg"));
        t.setAnnualPlanFee(new BigDecimal("4860.00"));
        t.setInstallmentMonthlyFee(new BigDecimal("405.00"));
        t.setCurrentLatitude(-23.5510);
        t.setCurrentLongitude(-46.6600);
        t.setLocationUpdatedAt(Instant.now().minus(2, ChronoUnit.MINUTES));
        t.getVehicleCharacteristics().addAll(Set.of(
                VehicleCharacteristic.AIR_CONDITIONING,
                VehicleCharacteristic.COMFORTABLE_SEATS,
                VehicleCharacteristic.SEATBELT_ALL_SEATS,
                VehicleCharacteristic.FREQUENT_SANITIZATION));
        priceZone(t, "Manhã", "Colégio Rio Branco", "450.00", "4860.00", "405.00",
                Set.of("Bela Vista", "Consolação"));
        review(t, "Adriana K.", 5, "Turma pequena, atendimento personalizado. Adorei.");
        review(t, "Bruno S.", 5, "Muito organizada, manda a rota do dia toda manhã.");
        review(t, "Letícia G.", 4, "Ótima, mas só atende de manhã — não serve para todo mundo.");
        review(t, "Marina C.", 5, "Segura e pontual, recomendo.");
        review(t, "Felipe D.", 5, "Melhor decisão que tomamos neste ano.");
        return transporterRepository.save(t);
    }

    // ================================================================= rota ===

    private void seedRoute(TransporterProfile roberto, Dependent ana, Dependent pedro,
                           Dependent helena, Dependent lucas, Dependent gabriel, Dependent enzo) {
        // Sequência geográfica em laço: Ipiranga → Vila Mariana → Saúde → Moema →
        // Jardim Paulista → Bela Vista, terminando na escola. A escola vem por último de
        // propósito: o RouteService sempre exibe os destinos depois dos embarques.
        routeStopRepository.saveAll(List.of(
                stop(roberto, ana, "Ana Souza", "Rua Bom Pastor, 1430 - Ipiranga",
                        RouteStopStatus.GOING, 1, -23.5930, -46.6100),
                stop(roberto, lucas, "Lucas Costa", "Rua Domingos de Morais, 1250 - Vila Mariana",
                        RouteStopStatus.GOING, 2, -23.5895, -46.6390),
                stop(roberto, pedro, "Pedro Silva", "Rua Luís Góis, 890 - Saúde",
                        RouteStopStatus.GOING, 3, -23.6120, -46.6360),
                stop(roberto, helena, "Helena Alves", "Alameda dos Nhambiquaras, 620 - Moema",
                        RouteStopStatus.GOING, 4, -23.6060, -46.6650),
                stop(roberto, gabriel, "Gabriel Ramos", "Alameda Santos, 1500 - Jardim Paulista",
                        RouteStopStatus.GOING, 5, -23.5665, -46.6530),
                stop(roberto, enzo, "Enzo Duarte", "Rua Treze de Maio, 700 - Bela Vista",
                        RouteStopStatus.GOING, 6, -23.5605, -46.6420),
                stop(roberto, null, "Colégio Objetivo", "Rua Vergueiro, 3185 - Vila Mariana",
                        RouteStopStatus.SCHOOL, 7, -23.5885, -46.6350)));
    }

    // =========================================================== financeiro ===

    /** Despesas, combustível e quilometragem do Roberto nos últimos 6 meses. */
    private void seedFinance(TransporterProfile t) {
        LocalDate today = LocalDate.now();

        List<Expense> expenses = new ArrayList<>(List.of(
                expense(t, today.minusDays(1), "Pedágio", "Rodoanel - volta", "38.60"),
                expense(t, today.minusDays(3), "Lavagem", "Lava-rápido completo", "70.00"),
                expense(t, today.minusDays(6), "Manutenção", "Troca de óleo e filtros", "420.00"),
                expense(t, today.minusDays(9), "Estacionamento", "Mensalista", "260.00"),
                expense(t, today.minusDays(12), "Pedágio", null, "42.20"),
                expense(t, today.minusDays(18), "Peças", "Jogo de pastilhas de freio", "385.00"),
                expense(t, today.minusDays(24), "Documentação", "Vistoria anual CET", "195.00"),
                expense(t, today.minusDays(31), "Seguro", "Parcela mensal da apólice", "310.00"),
                expense(t, today.minusDays(35), "Lavagem", "Higienização interna", "150.00"),
                expense(t, today.minusDays(44), "Manutenção", "Alinhamento e balanceamento", "180.00"),
                expense(t, today.minusDays(52), "Pedágio", null, "40.80"),
                expense(t, today.minusDays(58), "Seguro", "Parcela mensal da apólice", "310.00"),
                expense(t, today.minusDays(66), "Peças", "Correia dentada", "540.00"),
                expense(t, today.minusDays(74), "Estacionamento", "Mensalista", "260.00"),
                expense(t, today.minusDays(83), "Manutenção", "Revisão de 40.000 km", "890.00"),
                expense(t, today.minusDays(89), "Seguro", "Parcela mensal da apólice", "310.00"),
                expense(t, today.minusDays(97), "Lavagem", "Lava-rápido completo", "70.00"),
                expense(t, today.minusDays(105), "IPVA", "Parcela 3/3", "742.00"),
                expense(t, today.minusDays(118), "Seguro", "Parcela mensal da apólice", "310.00"),
                expense(t, today.minusDays(126), "Pedágio", null, "44.00"),
                expense(t, today.minusDays(139), "Manutenção", "Troca de óleo e filtros", "420.00"),
                expense(t, today.minusDays(150), "Seguro", "Parcela mensal da apólice", "310.00"),
                expense(t, today.minusDays(161), "Peças", "Bateria 60Ah", "620.00"),
                expense(t, today.minusDays(172), "Estacionamento", "Mensalista", "260.00")));
        expenseRepository.saveAll(expenses);

        List<FuelEntry> fuel = new ArrayList<>();
        // ~2 abastecimentos por mês nos últimos 6 meses.
        int[] days = {2, 11, 19, 27, 36, 45, 54, 62, 71, 80, 88, 97, 106, 115, 124, 133, 142, 151, 160, 169};
        double[] liters = {48.0, 52.5, 45.0, 50.0, 47.5, 53.0, 44.0, 49.5, 51.0, 46.0,
                50.5, 48.5, 52.0, 45.5, 49.0, 47.0, 53.5, 46.5, 50.0, 48.0};
        for (int i = 0; i < days.length; i++) {
            BigDecimal price = new BigDecimal(i < 6 ? "6.19" : i < 12 ? "6.04" : "5.89");
            BigDecimal amount = price.multiply(BigDecimal.valueOf(liters[i]))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            fuel.add(fuelEntry(t, today.minusDays(days[i]), liters[i], amount));
        }
        fuelEntryRepository.saveAll(fuel);

        // Quilometragem: dias úteis dos últimos 120 dias (hoje ainda é parcial).
        List<DailyDistance> distances = new ArrayList<>();
        for (int i = 0; i <= 120; i++) {
            LocalDate date = today.minusDays(i);
            DayOfWeek dow = date.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                continue;
            }
            double km = i == 0 ? 41.8 : 58.0 + ((i * 7) % 19) + (dow == DayOfWeek.FRIDAY ? 6.5 : 0);
            distances.add(dailyDistance(t, date, Math.round(km * 10.0) / 10.0));
        }
        dailyDistanceRepository.saveAll(distances);

        // Receita lançada à mão nos 2 meses anteriores ao histórico do app.
        YearMonth base = YearMonth.now().minusMonths(7);
        manualRevenueRepository.saveAll(List.of(
                manualRevenue(t, base, "2280.00"),
                manualRevenue(t, base.minusMonths(1), "1900.00")));
    }

    /** Financeiro enxuto da Fernanda (o suficiente para o painel dela abrir com números). */
    private void seedFinanceLight(TransporterProfile t) {
        LocalDate today = LocalDate.now();
        expenseRepository.saveAll(List.of(
                expense(t, today.minusDays(4), "Manutenção", "Troca de óleo", "390.00"),
                expense(t, today.minusDays(15), "Pedágio", null, "36.00"),
                expense(t, today.minusDays(28), "Seguro", "Parcela mensal", "295.00")));
        fuelEntryRepository.saveAll(List.of(
                fuelEntry(t, today.minusDays(3), 44.0, new BigDecimal("272.36")),
                fuelEntry(t, today.minusDays(17), 46.5, new BigDecimal("287.84"))));
        List<DailyDistance> distances = new ArrayList<>();
        for (int i = 0; i <= 30; i++) {
            LocalDate date = today.minusDays(i);
            DayOfWeek dow = date.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                continue;
            }
            distances.add(dailyDistance(t, date, 44.0 + ((i * 5) % 13)));
        }
        dailyDistanceRepository.saveAll(distances);
    }

    // ================================================================ avisos ===

    private void seedNotices(TransporterProfile roberto, GuardianProfile mariana,
                             GuardianProfile juliana, GuardianProfile renata) {
        Notice obras = notice(roberto, "Saída 10 minutos mais cedo amanhã",
                "Por causa das obras na Domingos de Morais, amanhã eu começo a rota 10 minutos "
                        + "mais cedo. Por favor, deixem as crianças prontas. Qualquer imprevisto, "
                        + "me chamem no chat.",
                NoticePriority.URGENT, 6, Instant.now().minus(3, ChronoUnit.HOURS));
        Notice mensalidade = notice(roberto, "Mensalidade deste mês já disponível",
                "A mensalidade deste mês já está aberta no aplicativo. Pagando até o dia 10 "
                        + "não há acréscimo. Quem preferir, consigo gerar o boleto pelo chat.",
                NoticePriority.INFO, 6, Instant.now().minus(2, ChronoUnit.DAYS));
        Notice feriado = notice(roberto, "Sem transporte na próxima sexta",
                "Na próxima sexta-feira é feriado e não haverá transporte. Voltamos ao normal "
                        + "na segunda, no horário de sempre.",
                NoticePriority.INFO, 6, Instant.now().minus(5, ChronoUnit.DAYS));
        Notice revisao = notice(roberto, "Van na revisão nesta quarta (van reserva)",
                "Nesta quarta a van vai para a revisão programada. Uso a van reserva, também "
                        + "com cinto em todos os bancos e a monitora Lúcia a bordo.",
                NoticePriority.INFO, 6, Instant.now().minus(9, ChronoUnit.DAYS));
        Notice reuniao = notice(roberto, "Reunião de pais do Colégio Objetivo",
                "Aviso agendado: na semana que vem tem reunião de pais e o horário da volta "
                        + "muda para 17h30. Confirmem no chat quem vai precisar do transporte.",
                NoticePriority.INFO, 6, Instant.now().plus(2, ChronoUnit.DAYS));
        noticeRepository.saveAll(List.of(obras, mensalidade, feriado, revisao, reuniao));

        noticeCommentRepository.saveAll(List.of(
                noticeComment(obras, mariana.getUser(), "Combinado, Roberto! O Lucas vai estar pronto às 6h20."),
                noticeComment(obras, juliana.getUser(), "Obrigada pelo aviso. O Pedro também estará pronto."),
                noticeComment(feriado, renata.getUser(), "Perfeito, obrigada por avisar com antecedência!"),
                noticeComment(mensalidade, mariana.getUser(), "Já paguei pelo app, chegou aí?")));

        noticeReactionRepository.saveAll(List.of(
                noticeReaction(obras, mariana.getUser(), "👍"),
                noticeReaction(obras, juliana.getUser(), "👍"),
                noticeReaction(obras, renata.getUser(), "❤️"),
                noticeReaction(feriado, mariana.getUser(), "👍"),
                noticeReaction(revisao, renata.getUser(), "👍")));
    }

    // ============================================================== conversas ===

    private void seedConversations(TransporterProfile roberto, TransporterProfile fernanda,
                                   GuardianProfile mariana, GuardianProfile juliana,
                                   GuardianProfile marcos, GuardianProfile renata,
                                   GuardianProfile thiago, GuardianProfile camila) {
        User rb = roberto.getUser();
        User fe = fernanda.getUser();

        // --- Mariana ↔ Roberto: conversa principal, com foto e 2 mensagens não lidas ---
        Conversation c1 = new Conversation();
        c1.setGuardian(mariana);
        c1.setTransporter(roberto);
        msg(c1, rb, "Bom dia, Mariana! Já saí, chego aí em uns 12 minutos.", 2, 6, 32);
        msg(c1, mariana.getUser(), "Bom dia, Roberto! O Lucas já está tomando café, vai estar pronto.", 2, 6, 34);
        msg(c1, rb, "Perfeito. Hoje a Lúcia está comigo na van.", 2, 6, 35);
        msg(c1, mariana.getUser(), "Ótimo! Ele fica mais tranquilo com ela.", 2, 6, 37);
        msg(c1, rb, "Chegamos na escola às 7h05, tudo certo. 👍", 2, 7, 6);
        msgImage(c1, rb, "Todos entregues na portaria.", "chat-escola.jpg", 2, 7, 8);
        msg(c1, mariana.getUser(), "Obrigada! Hoje a volta é no horário normal?", 2, 11, 40);
        msg(c1, rb, "É sim, 17h20 na portaria da escola.", 2, 11, 52);
        msg(c1, mariana.getUser(), "Roberto, amanhã o Lucas tem consulta às 15h, ele não volta na van.", 1, 19, 14);
        msg(c1, rb, "Anotado! Já marquei a falta dele na rota de amanhã.", 1, 19, 21);
        msg(c1, mariana.getUser(), "Você conseguiu ver a solicitação da Sofia? Queria fechar essa semana.", 0, 7, 12);
        msg(c1, rb, "Vi sim! Já liberei o contrato dela, é só assinar pelo app. 🙂", 0, 7, 18);
        msgImage(c1, rb, "Passando na Domingos de Morais agora.", "chat-van-parada.jpg", 0, 7, 26);
        msg(c1, rb, "Chego no seu portão em 4 minutos.", 0, 7, 28);
        // Mariana ainda não abriu as 2 últimas.
        c1.setGuardianLastReadAt(instant(0, 7, 20));
        c1.setTransporterLastReadAt(Instant.now());
        conversationRepository.save(c1);

        // --- Mariana ↔ Fernanda: cotação antes de contratar ---
        Conversation c2 = new Conversation();
        c2.setGuardian(mariana);
        c2.setTransporter(fernanda);
        msg(c2, mariana.getUser(), "Oi, Fernanda! Você tem vaga para o Colégio Objetivo, saindo da Vila Mariana?", 3, 20, 5);
        msg(c2, fe, "Oi, Mariana! Tenho sim, 2 vagas para o período da manhã.", 3, 20, 41);
        msg(c2, mariana.getUser(), "Qual o valor?", 3, 20, 44);
        msg(c2, fe, "R$ 420 no mensal, ou R$ 378 no parcelado com fidelidade de 12 meses.", 3, 20, 50);
        msg(c2, mariana.getUser(), "Obrigada! Vou comparar aqui no app e te falo.", 3, 21, 2);
        c2.setGuardianLastReadAt(Instant.now());
        c2.setTransporterLastReadAt(Instant.now());
        conversationRepository.save(c2);

        // --- Juliana ↔ Roberto ---
        Conversation c3 = new Conversation();
        c3.setGuardian(juliana);
        c3.setTransporter(roberto);
        msg(c3, juliana.getUser(), "Roberto, bom dia! A Beatriz também vai começar no Objetivo em outubro.", 1, 8, 12);
        msg(c3, rb, "Que ótima notícia! Manda a solicitação pelo app que eu já libero.", 1, 8, 30);
        msg(c3, juliana.getUser(), "Mandei agora. O valor pode ser o mesmo do Pedro?", 1, 8, 44);
        msg(c3, rb, "Como são dois irmãos, faço os dois por R$ 350 cada.", 1, 9, 2);
        msg(c3, juliana.getUser(), "Fechado! Muito obrigada. 😊", 1, 9, 5);
        msg(c3, rb, "Bom dia! Estou a 3 paradas do Pedro.", 0, 6, 48);
        msg(c3, juliana.getUser(), "Ele já está no portão!", 0, 6, 50);
        msg(c3, rb, "Peguei ele, seguindo para a escola.", 0, 6, 58);
        c3.setGuardianLastReadAt(Instant.now());
        c3.setTransporterLastReadAt(Instant.now());
        conversationRepository.save(c3);

        // --- Marcos ↔ Roberto: cobrança em atraso ---
        Conversation c4 = new Conversation();
        c4.setGuardian(marcos);
        c4.setTransporter(roberto);
        msg(c4, rb, "Marcos, tudo bem? A mensalidade deste mês consta em aberto aqui.", 1, 14, 10);
        msg(c4, marcos.getUser(), "Opa, Roberto! Desculpa, tive um imprevisto. Consigo pagar sexta.", 1, 14, 38);
        msg(c4, rb, "Sem problema, deixo anotado para sexta. 👍", 1, 14, 42);
        msg(c4, marcos.getUser(), "Valeu! Ah, e hoje a Ana não vai — está gripada.", 0, 6, 15);
        msg(c4, rb, "Melhoras para ela! Já tirei a Ana da rota de hoje.", 0, 6, 22);
        msg(c4, marcos.getUser(), "Obrigado, Roberto! Pago a mensalidade hoje ainda.", 0, 7, 40);
        c4.setGuardianLastReadAt(Instant.now());
        c4.setTransporterLastReadAt(instant(0, 7, 0)); // 1 não lida para o Roberto
        conversationRepository.save(c4);

        // --- Renata ↔ Roberto ---
        Conversation c5 = new Conversation();
        c5.setGuardian(renata);
        c5.setTransporter(roberto);
        msg(c5, renata.getUser(), "Roberto, a Helena esqueceu o casaco na van ontem.", 1, 18, 30);
        msg(c5, rb, "Achei aqui! Levo amanhã cedo. 🧥", 1, 18, 45);
        msg(c5, renata.getUser(), "Muito obrigada!", 1, 18, 47);
        msg(c5, rb, "Entreguei o casaco para a Helena agora há pouco.", 0, 7, 10);
        c5.setGuardianLastReadAt(instant(0, 7, 5)); // 1 não lida para a Renata
        c5.setTransporterLastReadAt(Instant.now());
        conversationRepository.save(c5);

        // --- Thiago ↔ Roberto ---
        Conversation c6 = new Conversation();
        c6.setGuardian(thiago);
        c6.setTransporter(roberto);
        msg(c6, thiago.getUser(), "Bom dia! Mandei a solicitação para o Théo também.", 0, 8, 5);
        msg(c6, rb, "Recebi! Analiso hoje ainda e te retorno.", 0, 8, 20);
        msg(c6, thiago.getUser(), "Show, obrigado!", 0, 8, 22);
        c6.setGuardianLastReadAt(Instant.now());
        c6.setTransporterLastReadAt(Instant.now());
        conversationRepository.save(c6);

        // --- Camila ↔ Roberto ---
        Conversation c7 = new Conversation();
        c7.setGuardian(camila);
        c7.setTransporter(roberto);
        msg(c7, camila.getUser(), "Roberto, o Enzo vai sair mais cedo na quinta, às 15h.", 2, 16, 12);
        msg(c7, rb, "Anotado. Consigo buscar às 15h sem problema.", 2, 16, 30);
        msg(c7, camila.getUser(), "Perfeito, obrigada!", 2, 16, 32);
        msg(c7, rb, "Bom dia! Chego na Treze de Maio em 8 minutos.", 0, 7, 15);
        c7.setGuardianLastReadAt(Instant.now());
        c7.setTransporterLastReadAt(Instant.now());
        conversationRepository.save(c7);

        // --- Camila ↔ Fernanda (a Laura vai com a Fernanda) ---
        Conversation c8 = new Conversation();
        c8.setGuardian(camila);
        c8.setTransporter(fernanda);
        msg(c8, fe, "Bom dia, Camila! Saindo para pegar a Laura.", 0, 6, 40);
        msg(c8, camila.getUser(), "Bom dia, Fernanda! Ela já está pronta.", 0, 6, 43);
        msg(c8, fe, "Peguei a Laura, seguindo para o Bandeirantes.", 0, 6, 55);
        msg(c8, camila.getUser(), "Obrigada! 🙏", 0, 6, 57);
        c8.setGuardianLastReadAt(Instant.now());
        c8.setTransporterLastReadAt(Instant.now());
        conversationRepository.save(c8);
    }

    // =========================================================== contratação ===

    private void seedHiring(TransporterProfile roberto, GuardianProfile mariana, Dependent sofia,
                            GuardianProfile juliana, Dependent beatriz,
                            GuardianProfile thiago, Dependent theo) {
        // 1) Sofia: aceita pelo Roberto, contrato liberado e AGUARDANDO ASSINATURA.
        //    É o roteiro da demonstração "Contratar pelo app" (assinar ao vivo).
        HireRequest sofiaRequest = hireRequest(mariana, roberto, sofia, HireStatus.ACCEPTED, null);
        hireRequestRepository.save(sofiaRequest);

        Contract contract = new Contract();
        contract.setHireRequest(sofiaRequest);
        contract.setGuardian(mariana);
        contract.setTransporter(roberto);
        contract.setDependent(sofia);
        contract.setMonthlyFee(new BigDecimal("380.00"));
        contract.setAnnualPlanFee(new BigDecimal("4104.00"));
        contract.setInstallmentMonthlyFee(new BigDecimal("342.00"));
        contract.setPlanType(PlanType.MONTHLY);
        contract.setFidelityMonths(0);
        contract.setStatus(ContractStatus.PENDING_SIGNATURE);
        contract.setSignatureToken(UUID.randomUUID().toString());
        contract.setContractText(contractTextBuilder.build(contract));
        contractRepository.save(contract);

        // 2) Beatriz e Théo: solicitações PENDENTES na caixa do Roberto (aceitar ao vivo).
        hireRequestRepository.save(
                hireRequest(juliana, roberto, beatriz, HireStatus.PENDING, new BigDecimal("350.00")));
        hireRequestRepository.save(
                hireRequest(thiago, roberto, theo, HireStatus.PENDING, new BigDecimal("480.00")));
    }

    // ============================================================== helpers ===

    private TransporterProfile transporter(String passwordHash, String name, String email,
                                           String phone, String photo, String plate, String cnh,
                                           String document, int years, String baseFee, String bio,
                                           Set<String> schools, Set<String> neighborhoods,
                                           List<String> vehiclePhotos) {
        User user = userRepository.save(user(name, email, phone, passwordHash, UserRole.TRANSPORTER));
        TransporterProfile t = new TransporterProfile();
        t.setUser(user);
        t.setPhotoUrl(MEDIA + photo);
        t.setPlate(plate);
        t.setCnh(cnh);
        t.setDocument(document);
        t.setYearsExperience(years);
        t.setBaseMonthlyFee(new BigDecimal(baseFee));
        t.setBio(bio);
        t.setRatingAvg(BigDecimal.ZERO);   // recalculado por ReviewService.recompute
        t.setReviewsCount(0);
        t.getSchools().addAll(schools);
        t.getNeighborhoods().addAll(neighborhoods);
        vehiclePhotos.forEach(p -> t.getVehiclePhotoUrls().add(MEDIA + p));
        return t;
    }

    private GuardianProfile guardian(String passwordHash, String name, String email, String phone,
                                     String photo, String cpf, String bio, String cep,
                                     String street, String number, String neighborhood,
                                     double lat, double lon) {
        User user = userRepository.save(user(name, email, phone, passwordHash, UserRole.GUARDIAN));
        GuardianProfile g = new GuardianProfile();
        g.setUser(user);
        g.setPhotoUrl(MEDIA + photo);
        g.setCpf(cpf);
        g.setBio(bio);
        g.setCep(cep);
        g.setCity("São Paulo");
        g.setNeighborhood(neighborhood);
        g.setStreet(street);
        g.setNumber(number);
        g.setLatitude(lat);
        g.setLongitude(lon);
        // Entrega no mesmo endereço do embarque (ida e volta para casa).
        g.setDeliveryCep(cep);
        g.setDeliveryCity("São Paulo");
        g.setDeliveryNeighborhood(neighborhood);
        g.setDeliveryStreet(street);
        g.setDeliveryNumber(number);
        g.setDeliveryLatitude(lat);
        g.setDeliveryLongitude(lon);
        return g;
    }

    private User user(String name, String email, String phone, String passwordHash, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        return user;
    }

    private Dependent dependent(GuardianProfile guardian, String name, String school, String photo) {
        Dependent d = new Dependent();
        d.setName(name);
        d.setSchool(school);
        d.setPhotoUrl(MEDIA + photo);
        guardian.addDependent(d);
        return d;
    }

    private void helper(TransporterProfile t, String name, String role, String photo) {
        Helper helper = new Helper();
        helper.setTransporter(t);
        helper.setName(name);
        helper.setRole(role);
        helper.setPhotoUrl(MEDIA + photo);
        helper.setActive(true);
        t.getHelpers().add(helper);
    }

    private void review(TransporterProfile t, String author, int rating, String comment) {
        Review review = new Review();
        review.setTransporter(t);
        review.setAuthorName(author);
        review.setRating(rating);
        review.setComment(comment);
        t.getReviews().add(review);
    }

    private void priceZone(TransporterProfile t, String name, String school, String monthly,
                           String annual, String installment, Set<String> neighborhoods) {
        PriceZone zone = new PriceZone();
        zone.setTransporter(t);
        zone.setName(name);
        zone.setSchool(school);
        zone.setMonthlyFee(new BigDecimal(monthly));
        if (annual != null) {
            zone.setAnnualFee(new BigDecimal(annual));
        }
        if (installment != null) {
            zone.setInstallmentMonthlyFee(new BigDecimal(installment));
        }
        zone.getNeighborhoods().addAll(neighborhoods);
        t.getPriceZones().add(zone);
    }

    private void shareWindow(TransporterProfile t, DayOfWeek from, DayOfWeek to,
                             LocalTime start, LocalTime end) {
        for (int day = from.getValue(); day <= to.getValue(); day++) {
            LocationShareWindow window = new LocationShareWindow();
            window.setTransporter(t);
            window.setDayOfWeek(day);
            window.setStartTime(start);
            window.setEndTime(end);
            t.getLocationShareWindows().add(window);
        }
    }

    private Enrollment enrollment(TransporterProfile t, Dependent d, String fee, FinanceStatus status) {
        Enrollment e = new Enrollment();
        e.setTransporter(t);
        e.setDependent(d);
        e.setMonthlyFee(new BigDecimal(fee));
        e.setFinanceStatus(status);
        e.setActive(true);
        return e;
    }

    /**
     * Sete meses de mensalidade: os seis anteriores sempre pagos e o mês corrente
     * no estado pedido (pago, em aberto ou atrasado). Nos meses fechados a data de
     * pagamento alterna entre o começo e o fim do mês — sem isso, a comparação com
     * o "período anterior" do painel financeiro cairia sempre em uma janela vazia.
     */
    private void payments(Enrollment enrollment, String amount, PaymentStatus currentStatus,
                          int dueDay, Integer paidDay) {
        YearMonth current = YearMonth.now();
        int base = paidDay == null ? dueDay - 2 : paidDay;
        for (int back = 6; back >= 1; back--) {
            YearMonth month = current.minusMonths(back);
            addPayment(enrollment, month, amount, PaymentStatus.PAID, dueDay,
                    back % 2 == 0 ? base : base + 20);
        }
        addPayment(enrollment, current, amount, currentStatus, dueDay,
                currentStatus == PaymentStatus.PAID ? paidDay : null);
    }

    private void addPayment(Enrollment enrollment, YearMonth month, String amount,
                            PaymentStatus status, int dueDay, Integer paidDay) {
        Payment payment = new Payment();
        payment.setEnrollment(enrollment);
        payment.setReferenceMonth(month.toString());
        payment.setAmount(new BigDecimal(amount));
        payment.setStatus(status);
        payment.setKind(PaymentKind.MENSALIDADE);
        payment.setDueDate(month.atDay(Math.min(dueDay, month.lengthOfMonth())));
        if (status == PaymentStatus.PAID && paidDay != null) {
            payment.setPaidAt(month.atDay(Math.min(Math.max(paidDay, 1), month.lengthOfMonth())));
        }
        enrollment.getPayments().add(payment);
    }

    private RouteStop stop(TransporterProfile t, Dependent dependent, String label, String address,
                           RouteStopStatus status, int position, double lat, double lon) {
        RouteStop stop = new RouteStop();
        stop.setTransporter(t);
        stop.setDependent(dependent);
        stop.setLabel(label);
        stop.setAddress(address);
        stop.setStatus(status);
        stop.setPosition(position);
        stop.setLatitude(lat);
        stop.setLongitude(lon);
        return stop;
    }

    private Expense expense(TransporterProfile t, LocalDate date, String category,
                            String description, String amount) {
        Expense expense = new Expense();
        expense.setTransporter(t);
        expense.setDate(date);
        expense.setCategory(category);
        expense.setDescription(description);
        expense.setAmount(new BigDecimal(amount));
        return expense;
    }

    private FuelEntry fuelEntry(TransporterProfile t, LocalDate date, double liters, BigDecimal amount) {
        FuelEntry entry = new FuelEntry();
        entry.setTransporter(t);
        entry.setDate(date);
        entry.setLiters(liters);
        entry.setAmount(amount);
        return entry;
    }

    private DailyDistance dailyDistance(TransporterProfile t, LocalDate date, double km) {
        DailyDistance distance = new DailyDistance();
        distance.setTransporter(t);
        distance.setDate(date);
        distance.setKm(km);
        return distance;
    }

    private ManualRevenue manualRevenue(TransporterProfile t, YearMonth month, String amount) {
        ManualRevenue revenue = new ManualRevenue();
        revenue.setTransporter(t);
        revenue.setReferenceMonth(month.toString());
        revenue.setAmount(new BigDecimal(amount));
        return revenue;
    }

    private Notice notice(TransporterProfile t, String title, String message,
                          NoticePriority priority, int recipients, Instant publishAt) {
        Notice notice = new Notice();
        notice.setTransporter(t);
        notice.setTitle(title);
        notice.setMessage(message);
        notice.setPriority(priority);
        notice.setRecipientsCount(recipients);
        notice.setPublishAt(publishAt);
        notice.setAllowComments(true);
        notice.setAudienceAll(true);
        return notice;
    }

    private NoticeComment noticeComment(Notice notice, User author, String text) {
        NoticeComment comment = new NoticeComment();
        comment.setNotice(notice);
        comment.setAuthor(author);
        comment.setText(text);
        return comment;
    }

    private NoticeReaction noticeReaction(Notice notice, User user, String emoji) {
        NoticeReaction reaction = new NoticeReaction();
        reaction.setNotice(notice);
        reaction.setUser(user);
        reaction.setEmoji(emoji);
        return reaction;
    }

    private HireRequest hireRequest(GuardianProfile guardian, TransporterProfile transporter,
                                    Dependent dependent, HireStatus status, BigDecimal proposedFee) {
        HireRequest request = new HireRequest();
        request.setGuardian(guardian);
        request.setTransporter(transporter);
        request.setDependent(dependent);
        request.setStatus(status);
        request.setProposedFee(proposedFee);
        request.setExpiresAt(Instant.now().plus(HireRequest.TTL_DAYS, ChronoUnit.DAYS));
        return request;
    }

    private void paymentMethod(GuardianProfile guardian, PaymentMethodType type,
                               String brand, String last4) {
        PaymentMethod method = new PaymentMethod();
        method.setGuardian(guardian);
        method.setType(type);
        method.setBrand(brand);
        method.setLast4(last4);
        method.setGatewayToken("demo_tok_" + UUID.randomUUID());
        method.setActive(true);
        paymentMethodRepository.save(method);
    }

    private void consent(User user) {
        UserConsent consent = new UserConsent();
        consent.setUser(user);
        consent.setType(ConsentType.PRIVACY_AND_TERMS);
        consent.setDocumentVersion(LegalDocuments.PRIVACY_TERMS_VERSION);
        consent.setAcceptedAt(Instant.now().minus(30, ChronoUnit.DAYS));
        userConsentRepository.save(consent);
    }

    /**
     * Marca o guia de funcionalidades como já visto, para que ele não abra sozinho
     * no meio da gravação. Para exibir o guia, apague a linha do usuário em
     * {@code onboarding_progress}.
     */
    private void onboardingSeen(User user) {
        OnboardingProgress progress = new OnboardingProgress();
        progress.setUser(user);
        progress.setLastSeenVersion(1);
        onboardingProgressRepository.save(progress);
    }

    private void msg(Conversation conversation, User sender, String text,
                     int daysAgo, int hour, int minute) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setText(text);
        message.setType(MessageType.TEXT);
        message.setSentAt(instant(daysAgo, hour, minute));
        conversation.getMessages().add(message);
    }

    private void msgImage(Conversation conversation, User sender, String caption, String photo,
                          int daysAgo, int hour, int minute) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setText(caption);
        message.setType(MessageType.IMAGE);
        message.setMediaUrl(MEDIA + photo);
        message.setSentAt(instant(daysAgo, hour, minute));
        conversation.getMessages().add(message);
    }

    /** Instante em "hoje menos N dias", no horário local informado. */
    private Instant instant(int daysAgo, int hour, int minute) {
        return LocalDate.now().minusDays(daysAgo)
                .atTime(hour, minute)
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant();
    }

    /** Vincula os dependentes ao catálogo de escolas (casando pelo nome). */
    private void linkSchoolRefs() {
        List<School> schools = schoolRepository.findAll();
        for (GuardianProfile guardian : guardianRepository.findAll()) {
            for (Dependent dependent : guardian.getDependents()) {
                if (dependent.getSchoolRef() != null || dependent.getSchool() == null) {
                    continue;
                }
                schools.stream()
                        .filter(s -> s.getName().equalsIgnoreCase(dependent.getSchool().trim()))
                        .findFirst()
                        .ifPresent(dependent::setSchoolRef);
            }
            guardianRepository.save(guardian);
        }
    }
}
