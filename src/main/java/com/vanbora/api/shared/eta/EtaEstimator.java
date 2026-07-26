package com.vanbora.api.shared.eta;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * Estimativas de trajeto usadas na rota do transportador e no acompanhamento do
 * responsável. Converte distância em linha reta em distância "de rua" (fator ~1,3)
 * e em minutos, usando a velocidade média de uma van escolar urbana (~24 km/h).
 * Valores aproximados e centralizados aqui (DRY).
 */
@Component
public class EtaEstimator {

    private static final double ROAD_FACTOR = 1.3;
    private static final double AVG_SPEED_KMH = 24.0;
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    /** Distância "de rua" a partir da distância em linha reta. */
    public double roadKm(double straightKm) {
        return straightKm * ROAD_FACTOR;
    }

    /** Minutos previstos para percorrer uma distância (já "de rua"). */
    public int minutes(double km) {
        return (int) Math.round(km / AVG_SPEED_KMH * 60.0);
    }

    /** Horário previsto ("HH:mm") daqui a N minutos. */
    public String clockFromNow(int minutes) {
        return LocalTime.now().plusMinutes(minutes).format(HH_MM);
    }
}
