package com.vanbora.api.modules.guardian.dto;

import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import java.util.List;
import java.util.Objects;

/** Perfil do responsável (tela "Meu Perfil"). */
public record GuardianProfileResponse(
        Long id,
        String name,
        String email,
        String phone,
        String photoUrl,
        String bio,
        String city,
        String neighborhood,
        AddressResponse pickup,
        AddressResponse delivery,
        boolean deliverySameAsPickup,
        List<DependentResponse> dependents
) {
    /** Endereço com coordenadas, exibido/editado no app. */
    public record AddressResponse(
            String cep,
            String street,
            String number,
            String neighborhood,
            String city,
            Double latitude,
            Double longitude
    ) {
    }

    public static GuardianProfileResponse from(GuardianProfile p, List<DependentResponse> dependents) {
        AddressResponse pickup = new AddressResponse(
                p.getCep(), p.getStreet(), p.getNumber(), p.getNeighborhood(), p.getCity(),
                p.getLatitude(), p.getLongitude());
        AddressResponse delivery = new AddressResponse(
                p.getDeliveryCep(), p.getDeliveryStreet(), p.getDeliveryNumber(),
                p.getDeliveryNeighborhood(), p.getDeliveryCity(),
                p.getDeliveryLatitude(), p.getDeliveryLongitude());
        boolean same = Objects.equals(p.getCep(), p.getDeliveryCep())
                && Objects.equals(p.getStreet(), p.getDeliveryStreet())
                && Objects.equals(p.getNumber(), p.getDeliveryNumber());

        return new GuardianProfileResponse(
                p.getId(),
                p.getUser().getName(),
                p.getUser().getEmail(),
                p.getUser().getPhone(),
                p.getPhotoUrl(),
                p.getBio(),
                p.getCity(),
                p.getNeighborhood(),
                pickup,
                delivery,
                same,
                dependents);
    }
}
