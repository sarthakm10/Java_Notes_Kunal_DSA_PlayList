package com.notes.tasks.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * The API's error contract, through the whole stack (filters, security, MVC, validation, service).
 * The first test is the regression the capstone found: a 400 must say WHICH field is wrong.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErrorContractTest {

    @Autowired MockMvcTester mvc;

    @Test
    void invalidBodyIs400WithOneEntryPerField() {
        assertThat(mvc.post().uri("/api/projects/2/tasks").with(jwt().jwt(j -> j.subject("meera")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\",\"priority\":\"HIGH\"}"))
                .hasStatus(400)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .hasPathSatisfying("$.title", t -> assertThat(t).isEqualTo("Validation failed"))
                .hasPathSatisfying("$.errors[0].field", f -> assertThat(f).isEqualTo("title"))
                .hasPathSatisfying("$.errors[0].rejected", r -> assertThat(r).isEqualTo(""));
    }

    @Test
    void unknownProjectIs404ProblemDetail() {
        assertThat(mvc.post().uri("/api/projects/99/tasks").with(jwt().jwt(j -> j.subject("asha")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Lost task\"}"))
                .hasStatus(404)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .hasPathSatisfying("$.detail", d -> assertThat(d).isEqualTo("Project 99 does not exist"));
    }

    @Test
    void badSortPropertyIs400NotA500() {
        assertThat(mvc.get().uri("/api/tasks?sort=nonsense").with(jwt().jwt(j -> j.subject("asha"))))
                .hasStatus(400)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    }
}
