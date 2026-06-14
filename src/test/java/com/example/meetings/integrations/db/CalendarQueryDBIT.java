package com.example.meetings.integrations.db;

import com.example.meetings.repository.MeetingRepository;
import com.example.meetings.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static org.junit.jupiter.api.Assertions.*;

class CalendarQueryDBIT extends BaseDBSetupIT {

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldExcludeDeclinedFromCalendar() {

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
                        .values(101, 10, 2, "DECLINED")
                        .build()
        ));

        var bob = userRepository.findByUsername("bob").get();

        var calendar = meetingRepository.findCalendarMeetings(bob);

        assertTrue(calendar.isEmpty());
    }

    @Test
    void shouldIncludeOrganizerAlways() {

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
    void shouldReturnEmptyCalendarWhenNoMeetingsExist() {

        execute(sequenceOf(
                insertInto("users")
                        .columns("id", "username", "email", "password_hash", "ical_token")
                        .values(1, "alice", "a@test.com", "pass", "t1")
                        .build()
        ));

        var alice = userRepository.findByUsername("alice").get();

        var calendar = meetingRepository.findCalendarMeetings(alice);

        assertTrue(calendar.isEmpty());
    }

    @Test
    void shouldIncludePendingAndAcceptedInCalendar() {

        execute(sequenceOf(
                insertInto("users")
                        .columns("id", "username", "email", "password_hash", "ical_token")
                        .values(1, "alice", "a@test.com", "pass", "t1")
                        .values(2, "bob", "b@test.com", "pass", "t2")
                        .build(),

                insertInto("meetings")
                        .columns("id", "title", "start_time", "end_time", "organizer_id")
                        .values(10, "M1", "2026-06-20 10:00:00", "2026-06-20 11:00:00", 1)
                        .build(),

                insertInto("meeting_participants")
                        .columns("id", "meeting_id", "user_id", "status")
                        .values(100, 10, 1, "ACCEPTED")
                        .values(101, 10, 2, "PENDING")
                        .build()
        ));

        var bob = userRepository.findByUsername("bob").orElseThrow();

        var calendar = meetingRepository.findCalendarMeetings(bob);

        assertEquals(1, calendar.size());
    }

    @Test
    void shouldReturnOnlyValidMeetingsAcrossMultipleStatuses() {

        execute(sequenceOf(
                insertInto("users")
                        .columns("id", "username", "email", "password_hash", "ical_token")
                        .values(1, "alice", "a@test.com", "pass", "t1")
                        .values(2, "bob", "b@test.com", "pass", "t2")
                        .build(),

                insertInto("meetings")
                        .columns("id", "title", "start_time", "end_time", "organizer_id")
                        .values(10, "M1", "2026-06-20 10:00:00", "2026-06-20 11:00:00", 1)
                        .values(11, "M2", "2026-06-21 10:00:00", "2026-06-21 11:00:00", 1)
                        .values(12, "M3", "2026-06-22 10:00:00", "2026-06-22 11:00:00", 1)
                        .build(),

                insertInto("meeting_participants")
                        .columns("id", "meeting_id", "user_id", "status")
                        .values(100, 10, 2, "ACCEPTED")
                        .values(101, 11, 2, "DECLINED")
                        .values(102, 12, 2, "PENDING")
                        .build()
        ));

        var bob = userRepository.findByUsername("bob").orElseThrow();

        var calendar = meetingRepository.findCalendarMeetings(bob);

        assertEquals(2, calendar.size()); // ACCEPTED + PENDING
        assertTrue(calendar.stream().anyMatch(m -> m.getId() == 10L));
        assertTrue(calendar.stream().anyMatch(m -> m.getId() == 12L));
    }

}