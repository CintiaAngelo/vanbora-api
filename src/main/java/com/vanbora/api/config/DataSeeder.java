package com.vanbora.api.config;

import com.vanbora.api.modules.chat.domain.Conversation;
import com.vanbora.api.modules.chat.domain.Message;
import com.vanbora.api.modules.chat.repository.ConversationRepository;
import com.vanbora.api.modules.enrollment.domain.Attendance;
import com.vanbora.api.modules.enrollment.domain.Enrollment;
import com.vanbora.api.modules.enrollment.repository.EnrollmentRepository;
import com.vanbora.api.modules.guardian.domain.Dependent;
import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.guardian.repository.GuardianProfileRepository;
import com.vanbora.api.modules.hire.domain.HireRequest;
import com.vanbora.api.modules.hire.repository.HireRequestRepository;
import com.vanbora.api.modules.notice.domain.Notice;
import com.vanbora.api.modules.notice.repository.NoticeRepository;
import com.vanbora.api.modules.payment.domain.Payment;
import com.vanbora.api.modules.route.domain.RouteStop;
import com.vanbora.api.modules.route.repository.RouteStopRepository;
import com.vanbora.api.modules.transporter.domain.Helper;
import com.vanbora.api.modules.transporter.domain.PriceZone;
import com.vanbora.api.modules.transporter.domain.Review;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.modules.user.repository.UserRepository;
import com.vanbora.api.shared.enums.FinanceStatus;
import com.vanbora.api.shared.enums.HireStatus;
import com.vanbora.api.shared.enums.PaymentStatus;
import com.vanbora.api.shared.enums.RouteStopStatus;
import com.vanbora.api.shared.enums.UserRole;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

    @Bean
    ApplicationRunner seedDemoData(
            @Value("${vanbora.seed-demo-data:true}") boolean enabled,
            UserRepository userRepository,
            GuardianProfileRepository guardianRepository,
            TransporterProfileRepository transporterRepository,
            EnrollmentRepository enrollmentRepository,
            NoticeRepository noticeRepository,
            RouteStopRepository routeStopRepository,
            ConversationRepository conversationRepository,
            HireRequestRepository hireRequestRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            if (!enabled || userRepository.count() > 0) {
                return;
            }

            String password = passwordEncoder.encode(DEFAULT_PASSWORD);

            // ----- Transportador principal: Roberto -----
            TransporterProfile roberto = buildTransporter(
                    userRepository, password, "Roberto Almeida", "roberto@vanbora.com",
                    "ABC-1D34", "••• ••• 1234", 15, 5, new BigDecimal("4.90"), 124,
                    new BigDecimal("350.00"), 3,
                    Set.of("Colégio Objetivo", "Escola Adventista", "COC"),
                    Set.of("Centro", "Vila Nova", "Jd. América"));
            addHelper(roberto, "Carlos Mendes", "Monitor");
            addReview(roberto, "Juliana S.", 5,
                    "Excelente profissional, muito pontual e cuidadoso com as crianças.");
            addReview(roberto, "Marcos T.", 5,
                    "Sempre avisa com antecedência se vai atrasar. Van sempre limpa e organizada.");
            addPriceZone(roberto, "Premium", "Colégio Objetivo", new BigDecimal("520.00"),
                    Set.of("Jardins", "Moema"));
            addPriceZone(roberto, "Standard", "Colégio Objetivo", new BigDecimal("380.00"),
                    Set.of("Saúde", "Ipiranga"));
            transporterRepository.save(roberto);

            // ----- Outros transportadores (resultados de busca) -----
            TransporterProfile joao = buildTransporter(
                    userRepository, password, "João Silva", "joao@vanbora.com",
                    "JKL-2M45", "••• ••• 2222", 12, 6, new BigDecimal("4.80"), 3,
                    new BigDecimal("340.00"), 3,
                    Set.of("Colégio São Paulo"), Set.of("Ipiranga"));
            transporterRepository.save(joao);

            TransporterProfile maria = buildTransporter(
                    userRepository, password, "Maria Oliveira", "maria@vanbora.com",
                    "MNO-3P67", "••• ••• 3333", 10, 4, new BigDecimal("4.20"), 1,
                    new BigDecimal("280.00"), 1,
                    Set.of("Colégio São Paulo"), Set.of("Ipiranga"));
            transporterRepository.save(maria);

            // ----- Responsáveis -----
            GuardianProfile mariana = buildGuardian(
                    userRepository, password, "Mariana Costa", "mariana@vanbora.com",
                    "São Paulo", "Centro");
            Dependent lucas = addDependent(mariana, "Lucas Costa", "Colégio Objetivo");
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
            seedWeek(lucasEnrollment, true, true, false, false, true);
            seedPaymentHistory(lucasEnrollment);
            enrollmentRepository.save(lucasEnrollment);

            Enrollment pedroEnrollment = buildEnrollment(roberto, pedro,
                    new BigDecimal("350.00"), FinanceStatus.PENDENTE);
            seedWeek(pedroEnrollment, true, true, true, true, true);
            enrollmentRepository.save(pedroEnrollment);

            Enrollment anaEnrollment = buildEnrollment(roberto, ana,
                    new BigDecimal("350.00"), FinanceStatus.ATRASADO);
            seedWeek(anaEnrollment, true, false, true, false, true);
            enrollmentRepository.save(anaEnrollment);

            // ----- Solicitação de contratação pendente -----
            HireRequest pending = new HireRequest();
            pending.setGuardian(juliana);
            pending.setTransporter(roberto);
            pending.setDependent(pedro);
            pending.setStatus(HireStatus.PENDING);
            hireRequestRepository.save(pending);

            // ----- Avisos -----
            noticeRepository.saveAll(List.of(
                    notice(roberto, "Amanhã o veículo sairá 10 min mais cedo devido a obras na "
                            + "avenida principal. Por favor, estejam prontos.", 3),
                    notice(roberto, "A mensalidade de Maio já está disponível para pagamento no "
                            + "aplicativo. Evite juros e pague até o dia 10.", 3),
                    notice(roberto, "Não haverá transporte na próxima sexta-feira (feriado nacional).", 3)));

            // ----- Rota do dia -----
            routeStopRepository.saveAll(List.of(
                    routeStop(roberto, "Lucas Costa", "Rua das Flores, 123 - Centro",
                            RouteStopStatus.GOING, 1),
                    routeStop(roberto, "Pedro Silva", "Av. Brasil, 456 - Vila Mariana",
                            RouteStopStatus.NOT_GOING, 2),
                    routeStop(roberto, "Colégio Objetivo", "Rua do Parque, 789 - Centro",
                            RouteStopStatus.SCHOOL, 3)));

            // ----- Conversa -----
            Conversation conversation = new Conversation();
            conversation.setGuardian(mariana);
            conversation.setTransporter(roberto);
            addMessage(conversation, roberto.getUser(), "Bom dia! Já estou a caminho, chego em 5 minutos.", 50);
            addMessage(conversation, mariana.getUser(), "Bom dia, Roberto! O Lucas já está descendo.", 48);
            addMessage(conversation, roberto.getUser(), "Já estamos a caminho da escola!", 20);
            conversationRepository.save(conversation);
        };
    }

    // ----- Helpers de construção -----

    private TransporterProfile buildTransporter(
            UserRepository userRepository, String password, String name, String email,
            String plate, String cnh, int capacity, int years, BigDecimal rating, int reviews,
            BigDecimal baseFee, int seats, Set<String> schools, Set<String> neighborhoods) {
        User user = userRepository.save(user(name, email, password, UserRole.TRANSPORTER));
        TransporterProfile t = new TransporterProfile();
        t.setUser(user);
        t.setPlate(plate);
        t.setCnh(cnh);
        t.setDocument("000.000.000-00");
        t.setCapacity(capacity);
        t.setYearsExperience(years);
        t.setRatingAvg(rating);
        t.setReviewsCount(reviews);
        t.setBaseMonthlyFee(baseFee);
        t.setAvailableSeats(seats);
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

    private void seedWeek(Enrollment enrollment, boolean mon, boolean tue, boolean wed,
                          boolean thu, boolean fri) {
        boolean[] presence = {mon, tue, wed, thu, fri};
        DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY};
        for (int i = 0; i < days.length; i++) {
            Attendance attendance = new Attendance();
            attendance.setEnrollment(enrollment);
            attendance.setDayOfWeek(days[i]);
            attendance.setPresent(presence[i]);
            enrollment.getAttendance().add(attendance);
        }
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

    private Notice notice(TransporterProfile transporter, String message, int recipients) {
        Notice notice = new Notice();
        notice.setTransporter(transporter);
        notice.setMessage(message);
        notice.setRecipientsCount(recipients);
        return notice;
    }

    private RouteStop routeStop(TransporterProfile transporter, String label, String address,
                                RouteStopStatus status, int position) {
        RouteStop stop = new RouteStop();
        stop.setTransporter(transporter);
        stop.setLabel(label);
        stop.setAddress(address);
        stop.setStatus(status);
        stop.setPosition(position);
        return stop;
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
