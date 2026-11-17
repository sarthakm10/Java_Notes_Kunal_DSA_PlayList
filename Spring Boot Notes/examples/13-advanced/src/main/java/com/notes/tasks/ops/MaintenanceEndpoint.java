package com.notes.tasks.ops;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * A custom Actuator endpoint at /actuator/maintenance.
 * GET shows the readiness state; POST {"enabled": true} takes the instance out of the load balancer
 * (readiness probe -> 503) without stopping it, e.g. before a deployment drains it.
 */
@Component
@Endpoint(id = "maintenance")
public class MaintenanceEndpoint {
    private final ApplicationEventPublisher events;
    private final ApplicationAvailability availability;

    public MaintenanceEndpoint(ApplicationEventPublisher events, ApplicationAvailability availability) {
        this.events = events;
        this.availability = availability;
    }

    @ReadOperation                                          // GET /actuator/maintenance
    public Map<String, Object> status() {
        return Map.of("readiness", availability.getReadinessState());
    }

    @WriteOperation                                         // POST /actuator/maintenance  {"enabled": true}
    public Map<String, Object> set(boolean enabled) {
        AvailabilityChangeEvent.publish(events, this,
                enabled ? ReadinessState.REFUSING_TRAFFIC : ReadinessState.ACCEPTING_TRAFFIC);
        return status();
    }
}
