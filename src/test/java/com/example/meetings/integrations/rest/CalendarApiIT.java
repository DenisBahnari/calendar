package com.example.meetings.integrations.rest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CalendarApiIT extends BaseApiIT {

    @Test
    void calendarShouldRequireAuthentication() {

        given()
                .redirects().follow(false)

                .when()
                .get("/calendar")

                .then()
                .statusCode(302)
                .header("Location",
                        containsString("/login"));
    }

    @Test
    void authenticatedUserShouldAccessCalendar() {

        createUser(
                "john",
                "john@test.com",
                "secret");

        login(
                "john",
                "secret");

        given()
                .filter(cookieFilter)

                .when()
                .get("/calendar")

                .then()
                .statusCode(200);
    }
}