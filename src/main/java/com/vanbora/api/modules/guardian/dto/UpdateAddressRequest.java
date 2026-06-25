package com.vanbora.api.modules.guardian.dto;

/** Atualização dos endereços de embarque e entrega do responsável. */
public record UpdateAddressRequest(
        AddressInput pickup,
        boolean deliverySameAsPickup,
        AddressInput delivery
) {
}
