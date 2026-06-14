package com.example.meetings.integrations.rest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthApiIT extends BaseApiIT {

    @Test
    void loginPageShouldBeAccessible() {
        given()
                .when()
                .get("/login")
                .then()
                .statusCode(200);
    }

    @Test
    void registerPageShouldBeAccessible() {
        given()
                .when()
                .get("/register")
                .then()
                .statusCode(200);
    }

    @Test
    void rootShouldRedirectToCalendar() {
        given()
                .redirects().follow(false)
                .when()
                .get("/")
                .then()
                .statusCode(302)
                .header("Location", containsString("/calendar"));
    }

    @Test
    void shouldRegisterNewUser() {
        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("username", "john")
                .formParam("email", "john@test.com")
                .formParam("password", "secret")

                .when()
                .post("/register")

                .then()
                .statusCode(302)
                .header("Location", containsString("/login"));
    }

    @Test
    void shouldRejectDuplicateUsername() {
        createUser("john", "john@test.com", "secret");

        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("username", "john")
                .formParam("email", "other@test.com")
                .formParam("password", "secret")
                .redirects().follow(false)

                .when()
                .post("/register")

                .then()
                .log().all()
                .statusCode(200);
    }
}
