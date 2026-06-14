package com.example.meetings.integrations.rest;

import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MeetingApiIT extends BaseApiIT {

    @Test
    void meetingFormShouldRequireAuthentication() {

        given()
                .redirects().follow(false)
                .when()
                .get("/meetings/new")
                .then()
                .statusCode(302)
                .header("Location", containsString("/login"));
    }

    @Test
    void authenticatedUserShouldOpenMeetingForm() {

        createUser("john", "john@test.com", "secret");
        login("john", "secret");

        auth()
                .when()
                .get("/meetings/new")
                .then()
                .log().all()
                .statusCode(200);
    }

    @Test
    void shouldCreateMeeting() {

        createUser("john", "john@test.com", "secret");
        login("john", "secret");

        auth()
                .contentType("application/x-www-form-urlencoded")
                .formParam("title", "VVS Meeting")
                .formParam("description", "integration test")
                .formParam("start", "2026-06-20T10:00")
                .formParam("end", "2026-06-20T11:00")
                .formParam("invitees", "")
                .when()
                .post("/meetings/new")
                .then()
                .log().all()
                .statusCode(302)
                .header("Location", containsString("/calendar"));
    }

    @Test
    void shouldRejectInvalidMeetingTimes() {

        createUser("john", "john@test.com", "secret");
        login("john", "secret");

        auth()
                .contentType("application/x-www-form-urlencoded")
                .formParam("title", "Bad Meeting")
                .formParam("description", "test")
                .formParam("start", "2026-06-20T12:00")
                .formParam("end", "2026-06-20T10:00")
                .formParam("invitees", "")
                .when()
                .post("/meetings/new")
                .then()
                .log().all()
                .statusCode(200)
                .body(containsString("End time must be after start time"));
    }

    @Test
    void shouldAcceptMeetingInvitation() {

        User alice = createUser("alice", "alice@test.com", "secret");
        User bob = createUser("bob", "bob@test.com", "secret");

        login("alice", "secret");

        Meeting meeting = meetingRepository.save(
                new Meeting(
                        "Test Meeting",
                        "desc",
                        Instant.now(),
                        Instant.now().plusSeconds(3600),
                        alice
                )
        );

        MeetingParticipant p = new MeetingParticipant(meeting, bob, InviteStatus.PENDING);
        meeting.addParticipant(p);
        meetingRepository.save(meeting);

        login("bob", "secret");

        auth()
                .contentType("application/x-www-form-urlencoded")
                .formParam("action", "accept")
                .when()
                .post("/meetings/" + meeting.getId() + "/respond")
                .then()
                .statusCode(302);
    }

    @Test
    void shouldDeclineMeetingInvitation() {

        User organizer = createUser("alice", "alice@test.com", "secret");
        User bob = createUser("bob", "bob@test.com", "secret");

        login("alice", "secret");

        Meeting meeting = meetingRepository.save(
                new Meeting(
                        "Test Meeting",
                        "desc",
                        Instant.now(),
                        Instant.now().plusSeconds(3600),
                        organizer
                )
        );

        // 👇 CRUCIAL: criar o convite
        MeetingParticipant participant = new MeetingParticipant(
                meeting,
                bob,
                InviteStatus.PENDING
        );

        meeting.addParticipant(participant);
        meetingRepository.save(meeting);

        login("bob", "secret");

        auth()
                .contentType("application/x-www-form-urlencoded")
                .formParam("action", "decline")
                .when()
                .post("/meetings/" + meeting.getId() + "/respond")
                .then()
                .statusCode(302)
                .header("Location", containsString("/calendar"));
    }

    @Test
    void respondShouldRequireAuthentication() {

        given()
                .redirects().follow(false)
                .formParam("action", "accept")
                .when()
                .post("/meetings/1/respond")
                .then()
                .statusCode(302)
                .header("Location", containsString("/login"));
    }
}