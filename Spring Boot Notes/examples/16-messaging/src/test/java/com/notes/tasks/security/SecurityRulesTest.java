package com.notes.tasks.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/** The real filter chains and method security, with fake users: no passwords or tokens needed. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityRulesTest {

    @Autowired MockMvcTester mvc;

    @Test
    void apiWithoutTokenIs401() {
        assertThat(mvc.get().uri("/api/tasks")).hasStatus(401);
    }

    @Test
    void userCanReadOwnTaskButNotOthers() {
        assertThat(mvc.get().uri("/api/tasks/1").with(jwt().jwt(j -> j.subject("ravi")))).hasStatusOk();
        assertThat(mvc.get().uri("/api/tasks/3").with(jwt().jwt(j -> j.subject("ravi")))).hasStatus(403);
    }

    @Test
    void onlyAdminsMayRunBulkJobs() {
        assertThat(mvc.post().uri("/api/tasks/complete-overdue").with(jwt().jwt(j -> j.subject("ravi"))))
                .hasStatus(403);
    }

    @Test
    @WithMockUser(username = "ravi")
    void browserPostWithoutCsrfTokenIs403() {
        assertThat(mvc.post().uri("/notes").param("text", "hi")).hasStatus(403);
        assertThat(mvc.post().uri("/notes").param("text", "hi").with(csrf()))
                .hasStatusOk()
                .hasContentTypeCompatibleWith(MediaType.TEXT_PLAIN)
                .bodyText().isEqualTo("saved for ravi: hi");
    }
}
