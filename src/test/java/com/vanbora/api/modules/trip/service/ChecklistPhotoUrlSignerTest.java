package com.vanbora.api.modules.trip.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

/** As fotos do checklist só saem com URL assinada, presa ao id da foto e de curta duração. */
class ChecklistPhotoUrlSignerTest {

    private static final Instant NOW = Instant.parse("2026-09-24T18:42:00Z");

    private ChecklistPhotoUrlSigner at(Instant instant) {
        return new ChecklistPhotoUrlSigner("segredo-de-teste-0123456789", Clock.fixed(instant, ZoneOffset.UTC));
    }

    private static String param(String path, String name) {
        return UriComponentsBuilder.fromUriString(path).build().getQueryParams().getFirst(name);
    }

    @Test
    void urlAssinadaValeParaAFotoDentroDoPrazo() {
        String path = at(NOW).signedPath(5L);
        long exp = Long.parseLong(param(path, "exp"));

        assertThat(path).startsWith("/api/media/checklist-photos/5?");
        assertThat(at(NOW.plusSeconds(60)).isValid(5L, exp, param(path, "sig"))).isTrue();
    }

    @Test
    void recusaAssinaturaDeOutraFotoAdulteradaOuExpirada() {
        String path = at(NOW).signedPath(5L);
        long exp = Long.parseLong(param(path, "exp"));
        String sig = param(path, "sig");

        assertThat(at(NOW).isValid(6L, exp, sig)).isFalse(); // trocar o id não funciona
        assertThat(at(NOW).isValid(5L, exp + 3600, sig)).isFalse(); // estender o prazo não funciona
        assertThat(at(NOW).isValid(5L, exp, sig + "x")).isFalse();
        assertThat(at(NOW.plus(ChecklistPhotoUrlSigner.TTL).plusSeconds(1)).isValid(5L, exp, sig)).isFalse();
        assertThat(new ChecklistPhotoUrlSigner("outro-segredo", Clock.fixed(NOW, ZoneOffset.UTC))
                .isValid(5L, exp, sig)).isFalse();
    }
}
