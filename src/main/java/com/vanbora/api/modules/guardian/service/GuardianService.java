package com.vanbora.api.modules.guardian.service;

import com.vanbora.api.modules.guardian.dto.AttendanceDayResponse;
import com.vanbora.api.modules.guardian.dto.CreateDependentRequest;
import com.vanbora.api.modules.guardian.dto.DependentResponse;
import com.vanbora.api.modules.guardian.dto.GuardianDashboardResponse;
import com.vanbora.api.modules.guardian.dto.GuardianProfileResponse;
import com.vanbora.api.modules.guardian.dto.GuardianTrackingResponse;
import com.vanbora.api.modules.guardian.dto.PaymentScheduleItemResponse;
import com.vanbora.api.modules.guardian.dto.UpdateAddressRequest;
import com.vanbora.api.modules.payment.dto.PaymentResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/** Casos de uso do responsável. */
public interface GuardianService {

    GuardianProfileResponse getMyProfile(Long userId);

    /** Atualiza os endereços de embarque/entrega (e regeocodifica). */
    GuardianProfileResponse updateAddress(Long userId, UpdateAddressRequest request);

    /** Atualiza a descrição/biografia do responsável. */
    GuardianProfileResponse updateBio(Long userId, String bio);

    /** Define/atualiza a foto do responsável. */
    GuardianProfileResponse setMyPhoto(Long userId, MultipartFile file);

    /** Define/atualiza a foto de um dependente. */
    DependentResponse setDependentPhoto(Long userId, Long dependentId, MultipartFile file);

    List<DependentResponse> listDependents(Long userId);

    DependentResponse addDependent(Long userId, CreateDependentRequest request);

    /** Edita nome/escola de um dependente. */
    DependentResponse updateDependent(Long userId, Long dependentId, CreateDependentRequest request);

    /** Exclui (arquiva) um dependente. Bloqueia se houver transporte ativo. */
    void deleteDependent(Long userId, Long dependentId);

    /** Painel do dependente selecionado (dependentId null = primeiro dependente). */
    GuardianDashboardResponse getDashboard(Long userId, Long dependentId);

    List<PaymentResponse> listPayments(Long userId);

    /** Cronograma de pagamentos do dependente: histórico real + meses futuros projetados. */
    List<PaymentScheduleItemResponse> listPaymentSchedule(Long userId, Long dependentId);

    /** Próximos dias letivos do dependente com o status "vai / não vai" (tela Avisar falta). */
    List<AttendanceDayResponse> listAttendance(Long userId, Long dependentId);

    /** Presença (seg–sex) da semana que contém a data, para o dependente (navegação por semanas). */
    List<AttendanceDayResponse> getWeek(Long userId, Long dependentId, LocalDate date);

    /** Marca/desmarca falta de um dependente numa data e devolve a agenda atualizada. */
    List<AttendanceDayResponse> setGoing(Long userId, Long dependentId, LocalDate date, boolean going);

    /** Posição atual do transportador do dependente + paradas, para o mapa em tempo real. */
    GuardianTrackingResponse getTracking(Long userId, Long dependentId);
}
