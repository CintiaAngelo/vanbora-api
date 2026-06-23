package com.vanbora.api.modules.transporter.dto;

import com.vanbora.api.modules.transporter.domain.TransporterProfile;
import java.util.List;

/** Perfil próprio do transportador (tela "Meu Perfil"). */
public record TransporterProfileResponse(
        Long id,
        String name,
        String email,
        String cnh,
        String plate,
        Integer capacity,
        List<String> schools,
        List<String> neighborhoods,
        List<HelperResponse> helpers
) {
    public static TransporterProfileResponse from(TransporterProfile t, List<HelperResponse> helpers) {
        return new TransporterProfileResponse(
                t.getId(),
                t.getUser().getName(),
                t.getUser().getEmail(),
                t.getCnh(),
                t.getPlate(),
                t.getCapacity(),
                List.copyOf(t.getNeighborhoods()),
                List.copyOf(t.getSchools()),
                helpers);
    }
}
