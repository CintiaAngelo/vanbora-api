package com.vanbora.api.modules.transporter.service;

import com.vanbora.api.modules.transporter.dto.TransporterLocationResponse;
import com.vanbora.api.modules.transporter.dto.UpdateLocationRequest;

/** Atualização da posição GPS do transportador. */
public interface TransporterLocationService {

    TransporterLocationResponse updateLocation(Long userId, UpdateLocationRequest request);
}
