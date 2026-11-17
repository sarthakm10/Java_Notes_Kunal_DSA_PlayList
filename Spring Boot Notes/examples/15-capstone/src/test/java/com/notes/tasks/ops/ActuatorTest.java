package com.notes.tasks.ops;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActuatorTest {
    @Autowired MockMvcTester mvc;

    @Test
    void healthIsPublicButHidesDetails() {
        assertThat(mvc.get().uri("/actuator/health")).hasStatusOk()
                .bodyJson().doesNotHavePath("$.components");     // details only for ADMIN
    }

    @Test
    void metricsNeedAuthentication() {
        assertThat(mvc.get().uri("/actuator/metrics")).hasStatus(401);
    }

    @Test
    @WithMockUser(username = "ravi", roles = "USER")
    void metricsAreForAdminsOnly() {
        assertThat(mvc.get().uri("/actuator/metrics")).hasStatus(403);
    }

    @Test
    @WithMockUser(username = "asha", roles = "ADMIN")
    void adminSeesHealthDetailsAndCustomIndicator() {
        assertThat(mvc.get().uri("/actuator/health")).hasStatusOk()
                .bodyJson().extractingPath("$.components.backlog.status").isEqualTo("UP");
    }
}
