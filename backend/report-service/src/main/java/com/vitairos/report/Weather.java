package com.vitairos.report;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@RestController
class Weather {
    record Snapshot(double latitude, double longitude, Instant observedAt, double temperatureC,
                    int humidityPercent, double windSpeedKmh, int weatherCode) {
        public String getSource() { return "Open-Meteo"; }
        public String getSourceUrl() { return "https://open-meteo.com/"; }
    }
    private record Forecast(Current current) {}
    private record Current(Long time, Double temperature_2m, Integer relative_humidity_2m,
                           Double wind_speed_10m, Integer weather_code) {}

    private static final Logger log = LoggerFactory.getLogger(Weather.class);
    private final RestClient client;
    private final JdbcTemplate db;

    Weather(RestClient.Builder builder, @Value("${weather.url}") String url, JdbcTemplate db) {
        this.client = builder.baseUrl(url).build();
        this.db = db;
    }

    @GetMapping("/api/v1/weather/current")
    Snapshot current(@RequestParam double latitude, @RequestParam double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude) || Math.abs(latitude)>90 || Math.abs(longitude)>180)
            throw new ResponseStatusException(BAD_REQUEST, "Coordenadas inválidas");
        return lookup(latitude, longitude).orElseThrow(() -> new ResponseStatusException(SERVICE_UNAVAILABLE, "Clima no disponible"));
    }

    private Optional<Snapshot> lookup(double latitude, double longitude) {
        try {
            Forecast forecast = client.get().uri(uri -> uri.path("/v1/forecast")
                    .queryParam("latitude", latitude).queryParam("longitude", longitude)
                    .queryParam("current", "temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code")
                    .queryParam("timeformat", "unixtime").build()).retrieve().body(Forecast.class);
            Current c = forecast == null ? null : forecast.current();
            if (c != null && c.time() != null && c.temperature_2m() != null && c.relative_humidity_2m() != null
                    && c.wind_speed_10m() != null && c.weather_code() != null)
                return Optional.of(new Snapshot(latitude, longitude, Instant.ofEpochSecond(c.time()), c.temperature_2m(),
                        c.relative_humidity_2m(), c.wind_speed_10m(), c.weather_code()));
            log.warn("Open-Meteo devolvió una lectura incompleta");
        } catch (RestClientException e) {
            log.warn("Consulta a Open-Meteo fallida ({})", e.getClass().getSimpleName());
        }
        return Optional.empty();
    }

    void capture(String id, Catalog.Form form) {
        if (form.latitude() == null) return;
        lookup(form.latitude(), form.longitude()).ifPresent(w -> db.update("""
                INSERT INTO report_weather(report_id, latitude, longitude, observed_at, temperature_c,
                                           humidity_percent, wind_kmh, weather_code)
                VALUES (?,?,?,?,?,?,?,?)
                """, id, w.latitude(), w.longitude(), w.observedAt().atOffset(ZoneOffset.UTC),
                w.temperatureC(), w.humidityPercent(), w.windSpeedKmh(), w.weatherCode()));
    }

    Snapshot saved(String id) {
        return db.query("SELECT * FROM report_weather WHERE report_id=?", (r,n) ->
                new Snapshot(r.getDouble("latitude"), r.getDouble("longitude"),
                        r.getObject("observed_at", OffsetDateTime.class).toInstant(), r.getDouble("temperature_c"),
                        r.getInt("humidity_percent"), r.getDouble("wind_kmh"), r.getInt("weather_code")), id)
                .stream().findFirst().orElse(null);
    }
}
