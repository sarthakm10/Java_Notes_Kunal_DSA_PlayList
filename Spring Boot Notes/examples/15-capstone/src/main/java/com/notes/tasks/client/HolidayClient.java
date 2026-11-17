package com.notes.tasks.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/** Calls another HTTP service (a public-holiday API) with RestClient: fluent, synchronous. */
@Component
public class HolidayClient {
    public record Holiday(LocalDate date, String name) {}

    private final RestClient http;

    public HolidayClient(RestClient.Builder builder,                    // Boot's builder: Jackson, observations
                         @Value("${app.holidays.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(1));               // never wait forever on another service
        factory.setReadTimeout(Duration.ofSeconds(1));
        this.http = builder
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    public List<Holiday> holidays(int year) {
        return http.get()
                .uri("/holidays/{year}", year)                           // URI template, value encoded for us
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new HolidayServiceException("holiday service answered " + res.getStatusCode());
                })
                .body(new ParameterizedTypeReference<List<Holiday>>() {}); // JSON array -> List<Holiday>
    }

    public static class HolidayServiceException extends RuntimeException {
        public HolidayServiceException(String message) {
            super(message);
        }
    }
}
