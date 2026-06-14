package com.example.meetings.integrations.rest;

import com.example.meetings.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DiscoveryApiIT extends BaseApiIT {

    @Test
    void shouldRequireAuthentication() {

        given()
                .redirects().follow(false)
                .when()
                .get("/discover")
                .then()
                .statusCode(302)
                .header("Location", containsString("/login"));
    }

    @Test
    void shouldLoadDiscoverPageWhenAuthenticated() {

        createUser("alice", "alice@test.com", "secret");
        login("alice", "secret");

        auth()
                .when()
                .get("/discover")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldReturnEmptyResultsWhenNoQuery() {

        createUser("alice", "alice@test.com", "secret");
        login("alice", "secret");

        auth()
                .when()
                .get("/discover")
                .then()
                .statusCode(200)
                .body(not(containsString("results")));
    }

    @Test
    void shouldAcceptQueryParamEvenWithoutResults() {

        createUser("alice", "alice@test.com", "secret");
        login("alice", "secret");

        auth()
                .when()
                .get("/discover?q=jazz")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldCopyDiscoveredEventAndRedirectToCalendar() {

        createUser("alice", "alice@test.com", "secret");
        login("alice", "secret");

        auth()
                .contentType("application/x-www-form-urlencoded")
                .formParam("source", "Ticketmaster")
                .formParam("externalId", "abc123")
                .formParam("title", "Jazz Night")
                .formParam("description", "Live music")
                .formParam("start", "2027-06-20T20:00:00Z")
                .formParam("end", "2027-06-20T21:00:00Z")
                .formParam("url", "https://ticketmaster.com/jazz")
                .formParam("venue", "Lisbon Arena")
                .when()
                .post("/discover/copy")
                .then()
                .statusCode(302)
                .header("Location", containsString("/calendar"));
    }
}