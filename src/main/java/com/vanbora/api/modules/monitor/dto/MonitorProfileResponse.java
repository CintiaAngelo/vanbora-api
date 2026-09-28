package com.vanbora.api.modules.monitor.dto;

import java.util.List;

/**
 * Perfil do monitor autenticado: os próprios dados + o essencial do transportador para quem
 * trabalha (nome e contato). Nada de financeiro, contratos ou configurações administrativas.
 */
public record MonitorProfileResponse(
        String name,
        String email,
        String phone,
        String photoUrl,
        String transporterName,
        String transporterPhone,
        String vehiclePlate,
        List<TripRef> trips
) {

    /** Percurso em que o monitor está escalado. */
    public record TripRef(Long id, String name) {
    }
}
