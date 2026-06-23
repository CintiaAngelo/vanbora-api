package com.vanbora.api.modules.finance.service;

import com.vanbora.api.modules.finance.dto.FinanceSummaryResponse;
import com.vanbora.api.modules.finance.dto.FinanceSummaryResponse.MonthlyRevenue;
import com.vanbora.api.modules.payment.domain.Payment;
import com.vanbora.api.modules.payment.repository.PaymentRepository;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.shared.enums.PaymentStatus;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Calcula o resumo financeiro a partir das mensalidades das matrículas do transportador. */
@Service
@Transactional(readOnly = true)
public class FinanceServiceImpl implements FinanceService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final int MONTHS_IN_CHART = 5;

    private final PaymentRepository paymentRepository;
    private final TransporterProfileRepository transporterRepository;

    public FinanceServiceImpl(PaymentRepository paymentRepository,
                              TransporterProfileRepository transporterRepository) {
        this.paymentRepository = paymentRepository;
        this.transporterRepository = transporterRepository;
    }

    @Override
    public FinanceSummaryResponse getSummary(Long transporterUserId) {
        TransporterProfile transporter = transporterRepository.findByUserId(transporterUserId)
                .orElseThrow(() -> ResourceNotFoundException.of("Perfil de transportador", transporterUserId));

        List<Payment> payments = paymentRepository.findByEnrollmentTransporterId(transporter.getId());

        String currentMonth = YearMonth.now().toString();
        BigDecimal received = sumWhere(payments,
                p -> p.getStatus() == PaymentStatus.PAID && currentMonth.equals(p.getReferenceMonth()));
        BigDecimal pending = sumWhere(payments, p -> p.getStatus() == PaymentStatus.PENDING);
        BigDecimal overdue = sumWhere(payments, p -> p.getStatus() == PaymentStatus.OVERDUE);

        return new FinanceSummaryResponse(received, pending, overdue, monthlyRevenue(payments));
    }

    private BigDecimal sumWhere(List<Payment> payments, java.util.function.Predicate<Payment> filter) {
        return payments.stream()
                .filter(filter)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<MonthlyRevenue> monthlyRevenue(List<Payment> payments) {
        Map<String, BigDecimal> byMonth = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .collect(Collectors.groupingBy(
                        Payment::getReferenceMonth,
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)));

        return byMonth.entrySet().stream()
                .skip(Math.max(0, byMonth.size() - MONTHS_IN_CHART))
                .map(entry -> new MonthlyRevenue(monthLabel(entry.getKey()), entry.getValue()))
                .toList();
    }

    private String monthLabel(String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        return ym.getMonth()
                .getDisplayName(TextStyle.SHORT, PT_BR)
                .toUpperCase(PT_BR)
                .replace(".", "");
    }
}
