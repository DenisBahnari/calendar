package com.example.meetings.integrations.rest;

import com.example.meetings.discover.AgendaLxProvider;
import com.example.meetings.discover.SeatGeekProvider;
import com.example.meetings.discover.TicketmasterProvider;
import com.example.meetings.model.User;
import com.example.meetings.repository.MeetingRepository;
import com.example.meetings.repository.UserRepository;
import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

@Import(TestSecurityConfig.class)
public abstract class BaseApiIT {

    @LocalServerPort
    protected int port;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected CookieFilter cookieFilter;

    @MockBean
    protected TicketmasterProvider ticketmasterProvider;

    @MockBean
    protected SeatGeekProvider seatGeekProvider;

    @MockBean
    protected AgendaLxProvider agendaLxProvider;

    protected RequestSpecification auth() {
        return RestAssured.given().filter(cookieFilter);
    }

    @Autowired
    protected MeetingRepository meetingRepository;

    @BeforeEach
    void setupBase() {

        RestAssured.port = port;

        cookieFilter = new CookieFilter();

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        meetingRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected User createUser(String username, String email, String password) {
        User user = new User(
                username,
                email,
                passwordEncoder.encode(password)
        );
        return userRepository.save(user);
    }

    protected void login(String username, String password) {

        RestAssured
                .given()
                .filter(cookieFilter)
                .redirects().follow(false)
                .log().all()

                .contentType("application/x-www-form-urlencoded")
                .formParam("username", username)
                .formParam("password", password)

                .when()
                .post("/login")

                .then()
                .log().all();
    }
}