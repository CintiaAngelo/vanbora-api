package com.vanbora.api.modules.auth.dto;

import com.vanbora.api.modules.transporter.domain.VehicleAccessibilityFeature;
import com.vanbora.api.modules.transporter.domain.VehicleCharacteristic;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;

/** Dados de cadastro do transportador (dados pessoais/veículo + área e preço). */
public record RegisterTransporterRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        /**
         * Senha. Obrigatória (mínimo 6 caracteres) no cadastro comum; ignorada
         * quando vem um {@code socialTicket} — a validação é feita no serviço,
         * porque depende do outro campo.
         */
        String password,
        /** Ticket do login social (Google/Facebook), quando o cadastro veio de lá. */
        String socialTicket,
        @NotBlank String phone,
        String document,
        String cnh,
        String plate,
        List<String> schools,
        List<String> neighborhoods,
        BigDecimal baseMonthlyFee,
        /** Opcional: características do veículo já selecionadas na etapa "Seu veículo" do cadastro. */
        List<VehicleCharacteristic> vehicleCharacteristics,
        /** Opcional: acessibilidade já selecionada na etapa "Seu veículo" do cadastro. */
        List<VehicleAccessibilityFeature> vehicleAccessibilityFeatures,
        /** Aceite da Política de Privacidade + Termos de Uso (LGPD). Obrigatório. */
        boolean acceptedTerms
) {
}
