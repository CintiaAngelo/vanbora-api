package com.vanbora.api.modules.transporter.service;

import com.vanbora.api.modules.transporter.dto.LocationSharingResponse;
import com.vanbora.api.modules.transporter.dto.TransporterLocationResponse;
import com.vanbora.api.modules.transporter.dto.UpdateLocationRequest;
import com.vanbora.api.modules.transporter.dto.UpdateLocationSharingRequest;

/** Atualização da posição GPS e da configuração de compartilhamento do transportador. */
public interface TransporterLocationService {

    TransporterLocationResponse updateLocation(Long userId, UpdateLocationRequest request);

    /** Configuração atual do compartilhamento (interruptor + janelas). */
    LocationSharingResponse getSharing(Long userId);

    /** Atualiza o interruptor mestre e as janelas de compartilhamento. */
    LocationSharingResponse updateSharing(Long userId, UpdateLocationSharingRequest request);
}
