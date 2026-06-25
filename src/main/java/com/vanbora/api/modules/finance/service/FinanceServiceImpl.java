package com.vanbora.api.modules.finance.service;

import com.vanbora.api.modules.finance.domain.DailyDistance;
import com.vanbora.api.modules.finance.domain.Expense;
import com.vanbora.api.modules.finance.domain.FuelEntry;
import com.vanbora.api.modules.finance.dto.CreateExpenseRequest;
import com.vanbora.api.modules.finance.dto.CreateFuelRequest;
import com.vanbora.api.modules.finance.dto.ExpenseResponse;
import com.vanbora.api.modules.finance.dto.FinanceReportResponse;
import com.vanbora.api.modules.finance.dto.FinanceReportResponse.ReportLine;
import com.vanbora.api.modules.finance.dto.FinanceSettingsRequest;
import com.vanbora.api.modules.finance.dto.FinanceSummaryResponse;
import com.vanbora.api.modules.finance.dto.FinanceSummaryResponse.CategoryTotal;
import com.vanbora.api.modules.finance.dto.FinanceSummaryResponse.Consumption;
import com.vanbora.api.modules.finance.dto.FinanceSummaryResponse.Goal;
import com.vanbora.api.modules.finance.dto.FinanceSummaryResponse.Maintenance;
import com.vanbora.api.modules.finance.dto.FinanceSummaryResponse.MonthlyRevenue;
import com.vanbora.api.modules.finance.dto.FuelEntryResponse;
import com.vanbora.api.modules.finance.dto.SuggestionResponse;
import com.vanbora.api.modules.finance.repository.DailyDistanceRepository;
import com.vanbora.api.modules.finance.repository.ExpenseRepository;
import com.vanbora.api.modules.finance.repository.FuelEntryRepository;
import com.vanbora.api.modules.payment.domain.Payment;
import com.vanbora.api.modules.payment.repository.PaymentRepository;
import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import com.vanbora.api.modules.transporter.repository.TransporterProfileRepository;
import com.vanbora.api.shared.enums.PaymentStatus;
import com.vanbora.api.shared.exception.BusinessException;
import com.vanbora.api.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Painel financeiro: receitas, despesas, km, consumo, metas, manutenção e sugestões. */
@Service
@Transactional(readOnly = true)
public class FinanceServiceImpl implements FinanceService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final int MONTHS_IN_CHART = 5;
    private static final String FUEL_CATEGORY = "Combustível";
    // Defaults para sugestões (parametrizáveis no futuro).
    private static final double DEFAULT_KM_PER_LITER = 8.0;
    private static final BigDecimal DEFAULT_FUEL_PRICE = new BigDecimal("6.00");

    private final PaymentRepository paymentRepository;
    private final TransporterProfileRepository transporterRepository;
    private final ExpenseRepository expenseRepository;
    private final FuelEntryRepository fuelRepository;
    private final DailyDistanceRepository distanceRepository;

    public FinanceServiceImpl(PaymentRepository paymentRepository,
                              TransporterProfileRepository transporterRepository,
                              ExpenseRepository expenseRepository,
                              FuelEntryRepository fuelRepository,
                              DailyDistanceRepository distanceRepository) {
        this.paymentRepository = paymentRepository;
        this.transporterRepository = transporterRepository;
        this.expenseRepository = expenseRepository;
        this.fuelRepository = fuelRepository;
        this.distanceRepository = distanceRepository;
    }

    @Override
    public FinanceSummaryResponse getSummary(Long userId, LocalDate from, LocalDate to) {
        TransporterProfile t = resolve(userId);
        Long id = t.getId();
        LocalDate start = from != null ? from : YearMonth.now().atDay(1);
        LocalDate end = to != null ? to : LocalDate.now();

        List<Payment> payments = paymentRepository.findByEnrollmentTransporterId(id);
        BigDecimal received = receivedBetween(payments, start, end);
        BigDecimal pending = sumWhere(payments, p -> p.getStatus() == PaymentStatus.PENDING);
        BigDecimal overdue = sumWhere(payments, p -> p.getStatus() == PaymentStatus.OVERDUE);

        List<Expense> expenses = expenseRepository.findByTransporterIdAndDateBetweenOrderByDateDesc(id, start, end);
        List<FuelEntry> fuel = fuelRepository.findByTransporterIdAndDateBetweenOrderByDateDesc(id, start, end);
        BigDecimal expensesTotal = sumExpenses(expenses).add(sumFuel(fuel));
        BigDecimal balance = received.subtract(expensesTotal);

        double kmPeriod = sumKm(distanceRepository.findByTransporterIdAndDateBetween(id, start, end));
        double kmToday = distanceRepository.findByTransporterIdAndDate(id, LocalDate.now())
                .map(DailyDistance::getKm).orElse(0.0);
        double kmTotal = distanceRepository.totalKm(id);

        return new FinanceSummaryResponse(
                received, pending, overdue, expensesTotal, balance,
                round1(kmToday), round1(kmPeriod), round1(kmTotal),
                expensesByCategory(expenses, fuel),
                consumption(fuel, kmPeriod),
                goal(t, payments),
                maintenance(t, kmTotal),
                monthlyRevenue(payments));
    }

    @Override
    public FinanceReportResponse getReport(Long userId, LocalDate from, LocalDate to) {
        TransporterProfile t = resolve(userId);
        Long id = t.getId();
        LocalDate start = from != null ? from : YearMonth.now().atDay(1);
        LocalDate end = to != null ? to : LocalDate.now();

        List<Payment> payments = paymentRepository.findByEnrollmentTransporterId(id);
        List<ReportLine> income = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID && inRange(p.getPaidAt(), start, end))
                .map(p -> new ReportLine(p.getPaidAt(), "Mensalidade " + p.getReferenceMonth(), p.getAmount()))
                .toList();

        List<ReportLine> expenseLines = new ArrayList<>();
        expenseRepository.findByTransporterIdAndDateBetweenOrderByDateDesc(id, start, end)
                .forEach(e -> expenseLines.add(new ReportLine(e.getDate(),
                        e.getCategory() + (e.getDescription() != null ? " - " + e.getDescription() : ""),
                        e.getAmount())));
        fuelRepository.findByTransporterIdAndDateBetweenOrderByDateDesc(id, start, end)
                .forEach(f -> expenseLines.add(new ReportLine(f.getDate(),
                        FUEL_CATEGORY + " (" + f.getLiters() + " L)", f.getAmount())));

        BigDecimal totalReceived = income.stream().map(ReportLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpenses = expenseLines.stream().map(ReportLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        double km = sumKm(distanceRepository.findByTransporterIdAndDateBetween(id, start, end));

        return new FinanceReportResponse(start, end, t.getUser().getName(),
                totalReceived, totalExpenses, totalReceived.subtract(totalExpenses), round1(km),
                income, expenseLines);
    }

    @Override
    public List<SuggestionResponse> getSuggestions(Long userId) {
        TransporterProfile t = resolve(userId);
        Long id = t.getId();
        List<SuggestionResponse> suggestions = new ArrayList<>();

        // Combustível estimado do mês com base no km rodado.
        double kmMonth = sumKm(distanceRepository.findByTransporterIdAndDateBetween(
                id, YearMonth.now().atDay(1), LocalDate.now()));
        if (kmMonth > 0) {
            BigDecimal estLiters = BigDecimal.valueOf(kmMonth / DEFAULT_KM_PER_LITER);
            BigDecimal estCost = estLiters.multiply(DEFAULT_FUEL_PRICE).setScale(2, RoundingMode.HALF_UP);
            suggestions.add(new SuggestionResponse(
                    "Combustível estimado do mês",
                    "Você rodou %.0f km. Estimativa de gasto: R$ %s".formatted(kmMonth, estCost),
                    FUEL_CATEGORY, estCost));
        }

        // Manutenção por km.
        if (t.getMaintenanceIntervalKm() != null && t.getMaintenanceIntervalKm() > 0) {
            double since = distanceRepository.totalKm(id) - (t.getLastMaintenanceKm() == null ? 0 : t.getLastMaintenanceKm());
            double remaining = t.getMaintenanceIntervalKm() - since;
            if (remaining <= 0) {
                suggestions.add(new SuggestionResponse("Revisão em atraso",
                        "Você já passou do intervalo de manutenção. Agende a revisão.", "Manutenção", null));
            } else if (remaining <= 300) {
                suggestions.add(new SuggestionResponse("Revisão próxima",
                        "Faltam %.0f km para a próxima manutenção.".formatted(remaining), "Manutenção", null));
            }
        } else {
            suggestions.add(new SuggestionResponse("Configure a manutenção",
                    "Defina o intervalo de manutenção (km) para receber lembretes.", null, null));
        }

        // Meta de receita.
        if (t.getMonthlyRevenueGoal() == null) {
            suggestions.add(new SuggestionResponse("Defina uma meta",
                    "Cadastre uma meta de receita mensal para acompanhar seu progresso.", null, null));
        }

        return suggestions;
    }

    // ----- Gastos -----

    @Override
    public List<ExpenseResponse> listExpenses(Long userId, LocalDate from, LocalDate to) {
        Long id = resolve(userId).getId();
        List<Expense> list = (from != null && to != null)
                ? expenseRepository.findByTransporterIdAndDateBetweenOrderByDateDesc(id, from, to)
                : expenseRepository.findByTransporterIdOrderByDateDesc(id);
        return list.stream().map(ExpenseResponse::from).toList();
    }

    @Override
    @Transactional
    public ExpenseResponse addExpense(Long userId, CreateExpenseRequest request) {
        TransporterProfile t = resolve(userId);
        Expense expense = new Expense();
        expense.setTransporter(t);
        apply(expense, request);
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    @Override
    @Transactional
    public ExpenseResponse updateExpense(Long userId, Long expenseId, CreateExpenseRequest request) {
        Long id = resolve(userId).getId();
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> ResourceNotFoundException.of("Gasto", expenseId));
        ensureOwner(expense.getTransporter().getId(), id);
        apply(expense, request);
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    @Override
    @Transactional
    public void deleteExpense(Long userId, Long expenseId) {
        Long id = resolve(userId).getId();
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> ResourceNotFoundException.of("Gasto", expenseId));
        ensureOwner(expense.getTransporter().getId(), id);
        expenseRepository.delete(expense);
    }

    // ----- Combustível -----

    @Override
    public List<FuelEntryResponse> listFuel(Long userId, LocalDate from, LocalDate to) {
        Long id = resolve(userId).getId();
        List<FuelEntry> list = (from != null && to != null)
                ? fuelRepository.findByTransporterIdAndDateBetweenOrderByDateDesc(id, from, to)
                : fuelRepository.findByTransporterIdOrderByDateDesc(id);
        return list.stream().map(FuelEntryResponse::from).toList();
    }

    @Override
    @Transactional
    public FuelEntryResponse addFuel(Long userId, CreateFuelRequest request) {
        TransporterProfile t = resolve(userId);
        FuelEntry entry = new FuelEntry();
        entry.setTransporter(t);
        entry.setDate(request.date() != null ? request.date() : LocalDate.now());
        entry.setLiters(request.liters());
        entry.setAmount(request.amount());
        return FuelEntryResponse.from(fuelRepository.save(entry));
    }

    @Override
    @Transactional
    public void deleteFuel(Long userId, Long fuelId) {
        Long id = resolve(userId).getId();
        FuelEntry entry = fuelRepository.findById(fuelId)
                .orElseThrow(() -> ResourceNotFoundException.of("Abastecimento", fuelId));
        ensureOwner(entry.getTransporter().getId(), id);
        fuelRepository.delete(entry);
    }

    // ----- Ajustes / manutenção -----

    @Override
    @Transactional
    public void updateSettings(Long userId, FinanceSettingsRequest request) {
        TransporterProfile t = resolve(userId);
        t.setMonthlyRevenueGoal(request.monthlyRevenueGoal());
        t.setMaintenanceIntervalKm(request.maintenanceIntervalKm());
        transporterRepository.save(t);
    }

    @Override
    @Transactional
    public void markMaintenanceDone(Long userId) {
        TransporterProfile t = resolve(userId);
        t.setLastMaintenanceKm(distanceRepository.totalKm(t.getId()));
        transporterRepository.save(t);
    }

    // ----- Helpers de cálculo -----

    private List<CategoryTotal> expensesByCategory(List<Expense> expenses, List<FuelEntry> fuel) {
        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        for (Expense e : expenses) {
            byCategory.merge(e.getCategory(), e.getAmount(), BigDecimal::add);
        }
        BigDecimal fuelTotal = sumFuel(fuel);
        if (fuelTotal.signum() > 0) {
            byCategory.merge(FUEL_CATEGORY, fuelTotal, BigDecimal::add);
        }
        return byCategory.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> new CategoryTotal(e.getKey(), e.getValue()))
                .toList();
    }

    private Consumption consumption(List<FuelEntry> fuel, double kmPeriod) {
        double liters = fuel.stream().mapToDouble(FuelEntry::getLiters).sum();
        BigDecimal fuelCost = sumFuel(fuel);
        Double kmPerLiter = liters > 0 ? round1(kmPeriod / liters) : null;
        BigDecimal costPerKm = kmPeriod > 0
                ? fuelCost.divide(BigDecimal.valueOf(kmPeriod), 2, RoundingMode.HALF_UP) : null;
        return new Consumption(round1(liters), kmPerLiter, costPerKm);
    }

    private Goal goal(TransporterProfile t, List<Payment> payments) {
        BigDecimal monthRevenue = receivedBetween(payments, YearMonth.now().atDay(1), LocalDate.now());
        BigDecimal monthlyGoal = t.getMonthlyRevenueGoal();
        double progress = (monthlyGoal != null && monthlyGoal.signum() > 0)
                ? Math.min(1.0, monthRevenue.doubleValue() / monthlyGoal.doubleValue()) : 0;
        return new Goal(monthlyGoal, monthRevenue, progress);
    }

    private Maintenance maintenance(TransporterProfile t, double kmTotal) {
        Integer interval = t.getMaintenanceIntervalKm();
        if (interval == null || interval <= 0) {
            return new Maintenance(null, null, null);
        }
        double since = kmTotal - (t.getLastMaintenanceKm() == null ? 0 : t.getLastMaintenanceKm());
        return new Maintenance(interval, round1(since), round1(interval - since));
    }

    private List<MonthlyRevenue> monthlyRevenue(List<Payment> payments) {
        Map<String, BigDecimal> byMonth = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .collect(Collectors.groupingBy(Payment::getReferenceMonth, TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)));
        return byMonth.entrySet().stream()
                .skip(Math.max(0, byMonth.size() - MONTHS_IN_CHART))
                .map(e -> new MonthlyRevenue(monthLabel(e.getKey()), e.getValue()))
                .toList();
    }

    private BigDecimal receivedBetween(List<Payment> payments, LocalDate start, LocalDate end) {
        return payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID && inRange(p.getPaidAt(), start, end))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean inRange(LocalDate date, LocalDate start, LocalDate end) {
        return date != null && !date.isBefore(start) && !date.isAfter(end);
    }

    private BigDecimal sumWhere(List<Payment> payments, java.util.function.Predicate<Payment> filter) {
        return payments.stream().filter(filter).map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumExpenses(List<Expense> expenses) {
        return expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumFuel(List<FuelEntry> fuel) {
        return fuel.stream().map(FuelEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private double sumKm(List<DailyDistance> distances) {
        return distances.stream().mapToDouble(DailyDistance::getKm).sum();
    }

    private void apply(Expense expense, CreateExpenseRequest request) {
        expense.setDate(request.date() != null ? request.date() : LocalDate.now());
        expense.setCategory(request.category().trim());
        expense.setDescription(request.description());
        expense.setAmount(request.amount());
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String monthLabel(String yearMonth) {
        return YearMonth.parse(yearMonth).getMonth()
                .getDisplayName(TextStyle.SHORT, PT_BR).toUpperCase(PT_BR).replace(".", "");
    }

    private void ensureOwner(Long ownerId, Long transporterId) {
        if (!ownerId.equals(transporterId)) {
            throw new BusinessException("Este registro não pertence ao transportador autenticado.");
        }
    }

    private TransporterProfile resolve(Long userId) {
        return transporterRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Perfil de transportador", userId));
    }
}
