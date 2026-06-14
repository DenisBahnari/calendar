package com.example.meetings.integrations.db;

import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.User;
import com.example.meetings.repository.MeetingRepository;
import com.example.meetings.repository.UserRepository;
import com.example.meetings.service.MeetingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static org.junit.jupiter.api.Assertions.*;

class MeetingDBIT extends BaseDBSetupIT {

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldPersistMeetingAndRetrieveIt() {

        execute(sequenceOf(
                insertInto("users")
                        .columns("id", "username", "email", "password_hash", "ical_token")
                        .values(1, "alice", "a@test.com", "pass", "t1")
                        .build(),

                insertInto("meetings")
                        .columns("id", "title", "description", "start_time", "end_time", "organizer_id")
                        .values(10, "VVS Meeting", "test",
                                Instant.parse("2026-06-20T10:00:00Z"),
                                Instant.parse("2026-06-20T11:00:00Z"),
                                1)
                        .build()
        ));

        var meetings = meetingRepository.findAll();

        assertEquals(1, meetings.size());
        assertEquals("VVS Meeting", meetings.get(0).getTitle());
    }

    @Test
    void shouldFindMeetingsForUserCalendar() {

        execute(sequenceOf(
                insertInto("users")
                        .columns("id", "username", "email", "password_hash", "ical_token")
                        .values(1, "alice", "a@test.com", "pass", "t1")
                        .build(),

                insertInto("meetings")
                        .columns("id", "title", "start_time", "end_time", "organizer_id")
                        .values(10, "M1",
                                "2026-06-20 10:00:00",
                                "2026-06-20 11:00:00",
                                1)
                        .build()
        ));

        var alice = userRepository.findByUsername("alice").get();

        var calendar = meetingRepository.findCalendarMeetings(alice);

        assertEquals(1, calendar.size());
    }

    @Test
    void shouldCreateMeetingWithOrganizerAcceptedAndInviteesPending() {

        execute(sequenceOf(
                insertInto("users")
                        .columns("id", "username", "email", "password_hash", "ical_token")
                        .values(1, "alice", "a@test.com", "pass", "t1")
                        .values(2, "bob", "b@test.com", "pass", "t2")
                        .build()
        ));

        User alice = userRepository.findByUsername("alice").orElseThrow();

        Meeting meeting = meetingService.propose(
                alice,
                "Team Meeting",
                "desc",
                Instant.parse("2026-06-20T10:00:00Z"),
                Instant.parse("2026-06-20T11:00:00Z"),
                List.of("bob")
        );

        assertNotNull(meeting.getId());

        assertTrue(meeting.getParticipants().stream()
                .anyMatch(p -> p.getUser().getUsername().equals("alice")
                        && p.getStatus().name().equals("ACCEPTED")));

        assertTrue(meeting.getParticipants().stream()
                .anyMatch(p -> p.getUser().getUsername().equals("bob")
                        && p.getStatus().name().equals("PENDING")));
    }

    @Test
    void shouldRejectUnknownInvitee() {

        execute(sequenceOf(
                insertInto("users")
                        .columns("id", "username", "email", "password_hash", "ical_token")
                        .values(1, "alice", "a@test.com", "pass", "t1")
                        .build()
        ));

        User alice = userRepository.findByUsername("alice").orElseThrow();

        assertThrows(IllegalArgumentException.class, () ->
                meetingService.propose(
                        alice,
                        "Test",
                        "desc",
                        Instant.now(),
                        Instant.now().plusSeconds(3600),
                        List.of("doesnotexist")
                )
        );
    }

    @Test
    @Transactional
    void shouldAcceptInvitation() {

        execute(sequenceOf(
                insertInto("users")
                        .columns("id", "username", "email", "password_hash", "ical_token")
                        .values(1, "alice", "a@test.com", "pass", "t1")
                        .values(2, "bob", "b@test.com", "pass", "t2")
                        .build(),

                insertInto("meetings")
                        .columns("id", "title", "start_time", "end_time", "organizer_id")
                        .values(10, "M1",
                                "2026-06-20 10:00:00",
                                "2026-06-20 11:00:00",
                                1)
                        .build(),

                insertInto("meeting_participants")
                        .columns("id", "meeting_id", "user_id", "status")
                        .values(100, 10, 2, "PENDING")
                        .values(101, 10, 1, "ACCEPTED")
                        .build()
        ));

        User bob = userRepository.findByUsername("bob").orElseThrow();

        meetingService.respond(10L, bob, InviteStatus.ACCEPTED);

        Meeting m = meetingRepository.findById(10L).orElseThrow();

        assertTrue(m.getParticipants().stream()
                .anyMatch(p -> p.getUser().getUsername().equals("bob")
                        && p.getStatus() == InviteStatus.ACCEPTED));
    }

    @Test
    @Transactional
    void shouldDeclineInvitation() {

        execute(sequenceOf(
                insertInto("users")
                        .columns("id", "username", "email", "password_hash", "ical_token")
                        .values(1, "alice", "a@test.com", "pass", "t1")
                        .values(2, "bob", "b@test.com", "pass", "t2")
                        .build(),

                insertInto("meetings")
                        .columns("id", "title", "start_time", "end_time", "organizer_id")
                        .values(10, "M1",
                                "2026-06-20 10:00:00",
                                "2026-06-20 11:00:00",
                                1)
                        .build(),

                insertInto("meeting_participants")
                        .columns("id", "meeting_id", "user_id", "status")
                        .values(100, 10, 2, "PENDING")
                        .values(101, 10, 1, "ACCEPTED")
                        .build()
        ));

        User bob = userRepository.findByUsername("bob").orElseThrow();

        meetingService.respond(10L, bob, InviteStatus.DECLINED);

        Meeting m = meetingRepository.findById(10L).orElseThrow();

        assertTrue(m.getParticipants().stream()
                .anyMatch(p -> p.getUser().getUsername().equals("bob")
                        && p.getStatus() == InviteStatus.DECLINED));
    }

    @Test
    void shouldDetectOverlappingMeetings() {

        execute(sequenceOf(
                insertInto("users")
                        .columns("id", "username", "email", "password_hash", "ical_token")
                        .values(1, "alice", "a@test.com", "pass", "t1")
                        .build(),

                insertInto("meetings")
                        .columns("id", "title", "start_time", "end_time", "organizer_id")
                        .values(10, "M1",
                                Instant.parse("2026-06-20T10:00:00Z"),
                                Instant.parse("2026-06-20T11:00:00Z"),
                                1)
                        .build()
        ));

        User alice = userRepository.findByUsername("alice").orElseThrow();

        var overlaps = meetingRepository.findOverlapping(
                alice,
                Instant.parse("2026-06-20T10:30:00Z"),
                Instant.parse("2026-06-20T11:30:00Z")
        );

        assertEquals(1, overlaps.size());
    }

    @Test
    @Transactional
    void shouldBeTentativeWhenNotAllAccepted() {

        execute(sequenceOf(
                insertInto("users")
                        .columns("id", "username", "email", "password_hash", "ical_token")
                        .values(1, "alice", "a@test.com", "pass", "t1")
                        .values(2, "bob", "b@test.com", "pass", "t2")
                        .build(),

                insertInto("meetings")
                        .columns("id", "title", "start_time", "end_time", "organizer_id")
                        .values(10, "M1",
                                "2026-06-20 10:00:00",
                                "2026-06-20 11:00:00",
                                1)
                        .build(),

                insertInto("meeting_participants")
                        .columns("id", "meeting_id", "user_id", "status")
                        .values(100, 10, 1, "ACCEPTED")
                        .values(101, 10, 2, "PENDING")
                        .build()
        ));

        Meeting m = meetingRepository.findById(10L).orElseThrow();

        assertFalse(m.isConfirmed());
    }

    @Test
    void shouldRejectInvalidMeetingTimes() {

        execute(sequenceOf(
                insertInto("users")
                        .columns("id", "username", "email", "password_hash", "ical_token")
                        .values(1, "alice", "a@test.com", "pass", "t1")
                        .build()
        ));

        User alice = userRepository.findByUsername("alice").orElseThrow();

        assertThrows(IllegalArgumentException.class, () ->
                meetingService.propose(
                        alice,
                        "bad",
                        "desc",
                        Instant.parse("2026-06-20T11:00:00Z"),
                        Instant.parse("2026-06-20T10:00:00Z"),
                        List.of()
                )
        );
    }

    @Test
    void shouldIgnoreDuplicateInviteesAndOrganizer() {

        execute(sequenceOf(
                insertInto("users")
                        .columns("id", "username", "email", "password_hash", "ical_token")
                        .values(1, "alice", "a@test.com", "pass", "t1")
                        .values(2, "bob", "b@test.com", "pass", "t2")
                        .build()
        ));

        User alice = userRepository.findByUsername("alice").orElseThrow();

        Meeting meeting = meetingService.propose(
                alice,
                "Team Meeting",
                "desc",
                Instant.parse("2026-06-20T10:00:00Z"),
                Instant.parse("2026-06-20T11:00:00Z"),
                List.of("bob", "bob", "alice", "bob")
        );

        long bobCount = meeting.getParticipants().stream()
                .filter(p -> p.getUser().getUsername().equals("bob"))
                .count();

        long aliceCount = meeting.getParticipants().stream()
                .filter(p -> p.getUser().getUsername().equals("alice"))
                .count();

        assertEquals(1, bobCount);
        assertEquals(1, aliceCount);
    }

}