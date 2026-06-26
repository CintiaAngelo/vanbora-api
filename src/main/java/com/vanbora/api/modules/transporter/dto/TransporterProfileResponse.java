package com.vanbora.api.modules.transporter.dto;

import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import java.math.BigDecimal;
import java.util.List;

/** Perfil próprio do transportador (tela "Meu Perfil"). */
public record TransporterProfileResponse(
        Long id,
        String name,
        String email,
        String phone,
        String photoUrl,
        String cnh,
        String plate,
        BigDecimal baseMonthlyFee,
        boolean acceptsProposals,
        List<String> schools,
        List<String> neighborhoods,
        List<HelperResponse> helpers
) {
    public static TransporterProfileResponse from(TransporterProfile t, List<HelperResponse> helpers) {
        return new TransporterProfileResponse(
                t.getId(),
                t.getUser().getName(),
                t.getUser().getEmail(),
                t.getUser().getPhone(),
                t.getPhotoUrl(),
                t.getCnh(),
                t.getPlate(),
                t.getBaseMonthlyFee(),
                t.isAcceptsProposals(),
                List.copyOf(t.getSchools()),
                List.copyOf(t.getNeighborhoods()),
                helpers);
    }
}
