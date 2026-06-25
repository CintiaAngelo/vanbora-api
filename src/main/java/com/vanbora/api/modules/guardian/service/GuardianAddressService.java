package com.vanbora.api.modules.guardian.service;

import com.vanbora.api.modules.guardian.domain.GuardianProfile;
import com.vanbora.api.modules.guardian.dto.AddressInput;
import com.vanbora.api.shared.geo.GeocodingService;
import org.springframework.stereotype.Service;

/**
 * Aplica os endereços de embarque/entrega ao perfil do responsável, geocodificando
 * cada um (best-effort). Reutilizado no cadastro e na edição de endereço.
 */
@Service
public class GuardianAddressService {

    private final GeocodingService geocodingService;

    public GuardianAddressService(GeocodingService geocodingService) {
        this.geocodingService = geocodingService;
    }

    public void apply(GuardianProfile guardian, AddressInput pickup,
                      boolean deliverySameAsPickup, AddressInput delivery) {
        if (pickup != null) {
            guardian.setCep(pickup.cep());
            guardian.setStreet(pickup.street());
            guardian.setNumber(pickup.number());
            guardian.setNeighborhood(pickup.neighborhood());
            guardian.setCity(pickup.city());
            geocodingService.geocodeBrazil(
                            pickup.street(), pickup.number(), pickup.neighborhood(), pickup.city())
                    .ifPresent(coord -> {
                        guardian.setLatitude(coord[0]);
                        guardian.setLongitude(coord[1]);
                    });
        }

        if (deliverySameAsPickup || delivery == null) {
            // Entrega igual ao embarque: copia tudo, inclusive as coordenadas já geocodificadas.
            guardian.setDeliveryCep(guardian.getCep());
            guardian.setDeliveryStreet(guardian.getStreet());
            guardian.setDeliveryNumber(guardian.getNumber());
            guardian.setDeliveryNeighborhood(guardian.getNeighborhood());
            guardian.setDeliveryCity(guardian.getCity());
            guardian.setDeliveryLatitude(guardian.getLatitude());
            guardian.setDeliveryLongitude(guardian.getLongitude());
        } else {
            guardian.setDeliveryCep(delivery.cep());
            guardian.setDeliveryStreet(delivery.street());
            guardian.setDeliveryNumber(delivery.number());
            guardian.setDeliveryNeighborhood(delivery.neighborhood());
            guardian.setDeliveryCity(delivery.city());
            geocodingService.geocodeBrazil(
                            delivery.street(), delivery.number(), delivery.neighborhood(), delivery.city())
                    .ifPresent(coord -> {
                        guardian.setDeliveryLatitude(coord[0]);
                        guardian.setDeliveryLongitude(coord[1]);
                    });
        }
    }
}
