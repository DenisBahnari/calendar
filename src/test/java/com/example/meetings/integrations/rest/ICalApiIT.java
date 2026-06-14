package com.example.meetings.integrations.rest;

import com.example.meetings.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class ICalApiIT extends BaseApiIT {

    @Test
    void shouldRedirectToLoginForUnknownToken() {

        given()
                .redirects().follow(false)

                .when()
                .get("/ical/invalid-token.ics")

                .then()
                .statusCode(302)
                .header("Location", containsString("/login"));
    }


    @Test
    void shouldReturnCalendarFeed() {

        User user = createUser(
                "john",
                "john@test.com",
                "secret");

        given()

                .when()
                .get("/ical/" + user.getIcalToken() + ".ics")

                .then()
                .statusCode(200)
                .contentType(containsString("text/calendar"))
                .body(containsString("BEGIN:VCALENDAR"))
                .body(containsString("END:VCALENDAR"));
    }
}