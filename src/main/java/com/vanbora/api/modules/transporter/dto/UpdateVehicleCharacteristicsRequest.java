package com.vanbora.api.modules.transporter.dto;

import com.vanbora.api.modules.transporter.domain.VehicleAccessibilityFeature;
import com.vanbora.api.modules.transporter.domain.VehicleCharacteristic;
import java.util.Set;

/** Substitui por completo o conjunto de características e de acessibilidade do veículo. */
public record UpdateVehicleCharacteristicsRequest(
        Set<VehicleCharacteristic> characteristics,
        Set<VehicleAccessibilityFeature> accessibilityFeatures
) {
}
