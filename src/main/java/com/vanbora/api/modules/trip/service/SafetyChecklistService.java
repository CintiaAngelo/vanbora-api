package com.vanbora.api.modules.trip.service;

import com.vanbora.api.modules.trip.dto.GuardianSafetyCheckResponse;
import com.vanbora.api.modules.trip.dto.SafetyChecklistResponse;
import com.vanbora.api.modules.user.domain.User;
import com.vanbora.api.shared.enums.ChecklistStatus;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/** Casos de uso do checklist de segurança pós-percurso. */
public interface SafetyChecklistService {

    /** Checklists visíveis ao profissional (opcionalmente filtrados por status). */
    List<SafetyChecklistResponse> list(User user, ChecklistStatus status);

    /** Detalhe (com fotos assinadas enquanto disponíveis). */
    SafetyChecklistResponse get(User user, Long checklistId);

    /**
     * Conclui o checklist: exige todos os itens confirmados e ao menos uma foto do interior
     * da van. Se outro profissional concluir primeiro, lança {@link ChecklistAlreadyCompletedException}
     * (409) e nada deste envio é gravado.
     */
    SafetyChecklistResponse complete(User user, Long checklistId, List<String> confirmedItems,
                                     List<MultipartFile> photos);

    /** Remove os arquivos das fotos com retenção vencida; o checklist permanece. Idempotente. */
    int purgeExpiredPhotos();

    /** [Responsável] Conferências concluídas recentemente nos percursos do(s) filho(s). */
    List<GuardianSafetyCheckResponse> recentForGuardian(User guardianUser, Long dependentId);
}
