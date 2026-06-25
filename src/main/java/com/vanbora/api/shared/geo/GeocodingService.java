package com.vanbora.api.shared.geo;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Geocodificação de endereços via Nominatim/OpenStreetMap (gratuito, sem chave).
 * Best-effort: qualquer falha devolve vazio (o recurso continua, só sem o pino no mapa).
 * Resultados são cacheados em memória para respeitar o limite de uso do Nominatim.
 */
@Service
public class GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);

    private final RestClient client;
    private final ConcurrentMap<String, double[]> cache = new ConcurrentHashMap<>();

    public GeocodingService(
            @Value("${vanbora.geocoding.nominatim-url:https://nominatim.openstreetmap.org}") String baseUrl,
            @Value("${vanbora.geocoding.user-agent:VanBora/1.0 (vanbora2026@gmail.com)}") String userAgent) {
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", userAgent)
                .build();
    }

    /** Geocodifica uma consulta livre. Devolve {lat, lon} ou vazio. */
    public Optional<double[]> geocode(String query) {
        if (!StringUtils.hasText(query)) {
            return Optional.empty();
        }
        String key = query.trim().toLowerCase();
        double[] cached = cache.get(key);
        if (cached != null) {
            return Optional.of(cached);
        }
        try {
            NominatimResult[] results = client.get()
                    .uri(uri -> uri.path("/search")
                            .queryParam("q", query)
                            .queryParam("format", "json")
                            .queryParam("limit", 1)
                            .build())
                    .retrieve()
                    .body(NominatimResult[].class);
            if (results != null && results.length > 0 && results[0].lat() != null) {
                double[] coord = {Double.parseDouble(results[0].lat()), Double.parseDouble(results[0].lon())};
                cache.put(key, coord);
                return Optional.of(coord);
            }
        } catch (Exception e) {
            log.warn("Geocodificação falhou para '{}': {}", query, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Geocodifica um endereço brasileiro, com fallback progressivo:
     * rua+número+bairro+cidade → bairro+cidade → cidade.
     */
    public Optional<double[]> geocodeBrazil(String street, String number, String neighborhood, String city) {
        Optional<double[]> full = geocode(join(street, number, neighborhood, city, "Brasil"));
        if (full.isPresent()) {
            return full;
        }
        if (StringUtils.hasText(neighborhood) || StringUtils.hasText(city)) {
            Optional<double[]> area = geocode(join(neighborhood, city, "Brasil"));
            if (area.isPresent()) {
                return area;
            }
        }
        return StringUtils.hasText(city) ? geocode(join(city, "Brasil")) : Optional.empty();
    }

    private String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(part.trim());
            }
        }
        return sb.toString();
    }

    /** Linha mínima do retorno do Nominatim (demais campos são ignorados). */
    private record NominatimResult(String lat, String lon) {}
}
