package com.notes.tasks;

import com.notes.tasks.dto.TaskDtos.TaskResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The whole application on a random port, real HTTP, real PostgreSQL (schema tracker16_test).
 * Capstone: file 10's test, back after file 11 dropped it, now logging in like a real client:
 * HTTP Basic -> POST /api/auth/token -> "Authorization: Bearer ..." on every call.
 * Nothing is rolled back here (the server commits), so the test cleans up after itself.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
class TaskApiIntegrationTest {

    @Autowired RestTestClient client;

    private String tokenFor(String user, String password) {
        Map<?, ?> body = client.post().uri("/api/auth/token")
                .headers(h -> h.setBasicAuth(user, password))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult().getResponseBody();
        assertThat(body).isNotNull();
        return (String) body.get("access_token");
    }

    @Test
    void createReadDeleteRoundTrip() {
        String meera = tokenFor("meera", "meera-secret");
        String asha = tokenFor("asha", "asha-secret");

        TaskResponse created = client.post().uri("/api/projects/2/tasks")
                .headers(h -> h.setBearerAuth(meera))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"title": "Write integration tests", "priority": "HIGH", "assigneeId": 3, "tags": ["testing"]}""")
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueMatches("Location", ".*/api/tasks/\\d+")
                .expectBody(TaskResponse.class)
                .returnResult().getResponseBody();

        assertThat(created).isNotNull();
        assertThat(created.project()).isEqualTo("Website");
        assertThat(created.tags()).containsExactly("testing");
        assertThat(created.createdBy()).isEqualTo("meera");         // the logged-in user (file 11)

        client.get().uri("/api/tasks/{id}", created.id())
                .headers(h -> h.setBearerAuth(meera))                // assigned to meera: she may read it
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.title").isEqualTo("Write integration tests");

        client.delete().uri("/api/tasks/{id}", created.id())
                .headers(h -> h.setBearerAuth(meera))                // delete is ADMIN only
                .exchange().expectStatus().isForbidden();
        client.delete().uri("/api/tasks/{id}", created.id())
                .headers(h -> h.setBearerAuth(asha))
                .exchange().expectStatus().isNoContent();
        client.get().uri("/api/tasks/{id}", created.id())
                .headers(h -> h.setBearerAuth(asha))
                .exchange().expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void searchPagesThroughTheBacklog() {
        String asha = tokenFor("asha", "asha-secret");
        client.get().uri("/api/tasks?size=4&sort=dueDate")
                .headers(h -> h.setBearerAuth(asha))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(4)
                .jsonPath("$.totalElements").isEqualTo(12)
                .jsonPath("$.totalPages").isEqualTo(3)
                .jsonPath("$.content[0].title").isEqualTo("Design the schema");
    }

    @Test
    void withoutATokenEverythingIs401() {
        client.get().uri("/api/tasks").exchange().expectStatus().isUnauthorized();
    }
}
