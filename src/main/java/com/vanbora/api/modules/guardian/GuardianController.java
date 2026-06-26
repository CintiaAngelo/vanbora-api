package com.vanbora.api.modules.guardian;

import com.vanbora.api.modules.guardian.dto.AttendanceDayResponse;
import com.vanbora.api.modules.guardian.dto.CreateDependentRequest;
import com.vanbora.api.modules.guardian.dto.DependentResponse;
import com.vanbora.api.modules.guardian.dto.GuardianDashboardResponse;
import com.vanbora.api.modules.guardian.dto.GuardianProfileResponse;
import com.vanbora.api.modules.guardian.dto.GuardianTrackingResponse;
import com.vanbora.api.modules.guardian.dto.PaymentScheduleItemResponse;
import com.vanbora.api.modules.guardian.dto.SetAttendanceRequest;
import com.vanbora.api.modules.guardian.dto.UpdateAddressRequest;
import com.vanbora.api.modules.guardian.service.GuardianService;
import com.vanbora.api.modules.payment.dto.PaymentResponse;
import com.vanbora.api.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints do responsável autenticado. */
@RestController
@RequestMapping("/api/guardians/me")
@PreAuthorize("hasRole('GUARDIAN')")
public class GuardianController {

    private final GuardianService guardianService;
    private final CurrentUserProvider currentUserProvider;

    public GuardianController(GuardianService guardianService, CurrentUserProvider currentUserProvider) {
        this.guardianService = guardianService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public GuardianProfileResponse profile() {
        return guardianService.getMyProfile(userId());
    }

    /** Atualiza os endereços de embarque/entrega (e regeocodifica). */
    @PutMapping("/address")
    public GuardianProfileResponse updateAddress(@Valid @RequestBody UpdateAddressRequest request) {
        return guardianService.updateAddress(userId(), request);
    }

    @GetMapping("/dashboard")
    public GuardianDashboardResponse dashboard(@RequestParam(required = false) Long dependentId) {
        return guardianService.getDashboard(userId(), dependentId);
    }

    @GetMapping("/dependents")
    public List<DependentResponse> dependents() {
        return guardianService.listDependents(userId());
    }

    @PostMapping("/dependents")
    public ResponseEntity<DependentResponse> addDependent(@Valid @RequestBody CreateDependentRequest request) {
        DependentResponse response = guardianService.addDependent(userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/dependents/{id}")
    public DependentResponse updateDependent(
            @PathVariable Long id, @Valid @RequestBody CreateDependentRequest request) {
        return guardianService.updateDependent(userId(), id, request);
    }

    @DeleteMapping("/dependents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDependent(@PathVariable Long id) {
        guardianService.deleteDependent(userId(), id);
    }

    @GetMapping("/payments")
    public List<PaymentResponse> payments() {
        return guardianService.listPayments(userId());
    }

    /** Cronograma de pagamentos: histórico real + meses futuros projetados. */
    @GetMapping("/payments/schedule")
    public List<PaymentScheduleItemResponse> paymentSchedule(
            @RequestParam(required = false) Long dependentId) {
        return guardianService.listPaymentSchedule(userId(), dependentId);
    }

    /** Próximos dias letivos com "vai / não vai" (tela Avisar falta). */
    @GetMapping("/attendance")
    public List<AttendanceDayResponse> attendance(@RequestParam(required = false) Long dependentId) {
        return guardianService.listAttendance(userId(), dependentId);
    }

    /** Presença (seg–sex) da semana que contém a data — navegação por semanas. */
    @GetMapping("/attendance/week")
    public List<AttendanceDayResponse> attendanceWeek(
            @RequestParam(required = false) Long dependentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return guardianService.getWeek(userId(), dependentId, date);
    }

    /** Marca/desmarca falta em uma data (going=false ⇒ falta). */
    @PutMapping("/attendance/{date}")
    public List<AttendanceDayResponse> setAttendance(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long dependentId,
            @RequestBody SetAttendanceRequest request) {
        return guardianService.setGoing(userId(), dependentId, date, request.going());
    }

    /** Mapa em tempo real: posição atual do transportador do dependente. */
    @GetMapping("/tracking")
    public GuardianTrackingResponse tracking(@RequestParam(required = false) Long dependentId) {
        return guardianService.getTracking(userId(), dependentId);
    }

    private Long userId() {
        return currentUserProvider.requireUserId();
    }
}
