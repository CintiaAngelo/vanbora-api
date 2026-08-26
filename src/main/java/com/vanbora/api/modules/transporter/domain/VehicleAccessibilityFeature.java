package com.vanbora.api.modules.transporter.domain;

/**
 * Recursos de acessibilidade do veículo. Deliberadamente detalhado (em vez de um único
 * "aceita PCD" ambíguo) — {@link #TRANSPORTS_STUDENTS_WITH_DISABILITY} não implica que o
 * veículo é adaptado; são informações independentes.
 */
public enum VehicleAccessibilityFeature {
    WHEELCHAIR_ADAPTED,
    WHEELCHAIR_SPACE,
    ACCESSIBLE_BOARDING_EQUIPMENT,
    TRANSPORTS_STUDENTS_WITH_DISABILITY
}
