package com.vanbora.api.modules.guardian.dto;

import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import java.util.List;

/** Perfil do responsável (tela "Meu Perfil"). */
public record GuardianProfileResponse(
        Long id,
        String name,
        String email,
        String phone,
        String city,
        String neighborhood,
        List<DependentResponse> dependents
) {
    public static GuardianProfileResponse from(GuardianProfile profile, List<DependentResponse> dependents) {
        return new GuardianProfileResponse(
                profile.getId(),
                profile.getUser().getName(),
                profile.getUser().getEmail(),
                profile.getUser().getPhone(),
                profile.getCity(),
                profile.getNeighborhood(),
                dependents);
    }
}
