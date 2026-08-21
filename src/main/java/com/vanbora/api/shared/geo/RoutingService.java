package com.vanbora.api.shared.geo;

import java.util.List;
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
 * Roteamento real (seguindo ruas) via OpenRouteService (gratuito, baseado em OSM).
 * Best-effort: sem chave configurada, ou qualquer falha, devolve vazio — quem chama
 * cai para o desenho em linha reta (comportamento anterior), nunca quebra a tela.
 * Resultados ficam em cache curto em memória para não estourar o limite gratuito
 * diário com o polling do rastreamento do responsável (a cada 5s).
 */
@Service
public class RoutingService {

    private static final Logger log = LoggerFactory.getLogger(RoutingService.class);
    private static final long CACHE_TTL_MILLIS = 45_000;

    private final RestClient client;
    private final String apiKey;
    private final ConcurrentMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public RoutingService(
            @Value("${vanbora.routing.ors.base-url:https://api.openrouteservice.org}") String baseUrl,
            @Value("${vanbora.routing.ors.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * Geometria da rota (lista de [lat, lon]) seguindo ruas, conectando os pontos na
     * ordem dada. Vazio se não houver chave configurada, a chamada falhar, ou houver
     * menos de 2 pontos.
     */
    public Optional<List<double[]>> route(List<double[]> waypointsLatLon) {
        if (!StringUtils.hasText(apiKey) || waypointsLatLon == null || waypointsLatLon.size() < 2) {
            return Optional.empty();
        }
        String key = cacheKey(waypointsLatLon);
        CacheEntry cached = cache.get(key);
        if (cached != null && cached.expiresAt() > System.currentTimeMillis()) {
            return Optional.of(cached.geometry());
        }
        try {
            // ORS espera coordenadas [longitude, latitude] — inverso do resto do sistema.
            List<List<Double>> coordinates = waypointsLatLon.stream()
                    .map(p -> List.of(p[1], p[0]))
                    .toList();
            OrsResponse response = client.post()
                    .uri("/v2/directions/driving-car/geojson")
                    .header("Authorization", apiKey)
                    .body(new OrsRequest(coordinates))
                    .retrieve()
                    .body(OrsResponse.class);
            if (response == null || response.features() == null || response.features().isEmpty()) {
                return Optional.empty();
            }
            List<List<Double>> coords = response.features().get(0).geometry().coordinates();
            List<double[]> geometry = coords.stream()
                    .map(c -> new double[] {c.get(1), c.get(0)}) // volta para [lat, lon]
                    .toList();
            cache.put(key, new CacheEntry(geometry, System.currentTimeMillis() + CACHE_TTL_MILLIS));
            return Optional.of(geometry);
        } catch (Exception e) {
            log.warn("Roteamento (ORS) falhou, caindo para linha reta: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Chave de cache: o ponto de partida (posição ao vivo da van) é arredondado a
     * ~11m — pequenos jitters de GPS reaproveitam a mesma rota já calculada. Os
     * demais pontos (paradas fixas) entram exatos.
     */
    private String cacheKey(List<double[]> waypoints) {
        StringBuilder sb = new StringBuilder();
        double[] start = waypoints.get(0);
        sb.append(round(start[0])).append(',').append(round(start[1]));
        for (int i = 1; i < waypoints.size(); i++) {
            double[] p = waypoints.get(i);
            sb.append('|').append(p[0]).append(',').append(p[1]);
        }
        return sb.toString();
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private record CacheEntry(List<double[]> geometry, long expiresAt) {
    }

    private record OrsRequest(List<List<Double>> coordinates) {
    }

    private record OrsResponse(List<OrsFeature> features) {
    }

    private record OrsFeature(OrsGeometry geometry) {
    }

    private record OrsGeometry(List<List<Double>> coordinates) {
    }
}
