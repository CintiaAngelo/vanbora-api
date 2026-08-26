package com.vanbora.api.modules.auth.dto;

import com.vanbora.api.modules.transporter.domain.VehicleAccessibilityFeature;
import com.vanbora.api.modules.transporter.domain.VehicleCharacteristic;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** Dados de cadastro do transportador (dados pessoais/veículo + área e preço). */
public record RegisterTransporterRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, message = "A senha deve ter ao menos 6 caracteres") String password,
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
