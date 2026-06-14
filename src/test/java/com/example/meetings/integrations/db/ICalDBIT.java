package com.example.meetings.integrations.db;

import com.example.meetings.model.Meeting;
import com.example.meetings.model.User;
import com.example.meetings.service.ICalService;
import com.example.meetings.repository.MeetingRepository;
import com.example.meetings.repository.UserRepository;
import com.example.meetings.service.MeetingService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static org.junit.jupiter.api.Assertions.*;

class ICalDBIT extends BaseDBSetupIT {

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private ICalService iCalService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Test
    @Transactional
    void shouldGenerateValidICalFromDatabaseData() {

        execute(sequenceOf(
                insertInto("users")
                        .columns("id", "username", "email", "password_hash", "ical_token")
                        .values(1, "alice", "a@test.com", "pass", "token1")
                        .build(),

                insertInto("meetings")
                        .columns("id", "title", "description", "start_time", "end_time", "organizer_id")
                        .values(10, "Test Meeting", "desc",
                                "2026-06-20 10:00:00",
                                "2026-06-20 11:00:00",
                                1)
                        .build(),

                insertInto("meeting_participants")
                        .columns("id", "meeting_id", "user_id", "status")
                        .values(100, 10, 1, "ACCEPTED")
                        .build()
        ));

        User user = userRepository.findByUsername("alice").orElseThrow();
        List<Meeting> meetings = meetingRepository.findCalendarMeetings(user);

        String ics = iCalService.render(user, meetings);

        assertTrue(ics.contains("BEGIN:VCALENDAR"));
        assertTrue(ics.contains("VERSION:2.0"));
        assertTrue(ics.contains("PRODID:-//meetings-app//EN"));
        assertTrue(ics.contains("END:VCALENDAR"));

        assertTrue(ics.contains("BEGIN:VEVENT"));
        assertTrue(ics.contains("END:VEVENT"));

        assertTrue(ics.contains("SUMMARY:Test Meeting"));
        assertTrue(ics.contains("DESCRIPTION:desc"));

        assertTrue(ics.contains("UID:meeting-10@meetings-app"));
        assertTrue(ics.contains("DTSTAMP:"));
        assertTrue(ics.contains("DTSTART:"));
        assertTrue(ics.contains("DTEND:"));

        assertTrue(ics.contains("ATTENDEE"));
        assertTrue(ics.contains("ORGANIZER"));

        assertTrue(ics.contains("alice"));
    }

    @Test
    void shouldGenerateValidICalForEmptyCalendar() {

        execute(sequenceOf(
                insertInto("users")
                        .columns("id", "username", "email", "password_hash", "ical_token")
                        .values(1, "alice", "a@test.com", "pass", "token1")
                        .build()
        ));

        User user = userRepository.findByUsername("alice").orElseThrow();

        List<Meeting> meetings = meetingRepository.findCalendarMeetings(user);

        String ics = iCalService.render(user, meetings);

        assertTrue(ics.contains("BEGIN:VCALENDAR"));
        assertTrue(ics.contains("END:VCALENDAR"));

        // não deve ter eventos
        assertFalse(ics.contains("BEGIN:VEVENT"));
    }

    @Test
    @Transactional
    void shouldRenderMultipleMeetingsInICal() {

        execute(sequenceOf(
                insertInto("users")
                        .columns("id", "username", "email", "password_hash", "ical_token")
                        .values(1, "alice", "a@test.com", "pass", "token1")
                        .build(),

                insertInto("meetings")
                        .columns("id", "title", "start_time", "end_time", "organizer_id")
                        .values(10, "M1", "2026-06-20 10:00:00", "2026-06-20 11:00:00", 1)
                        .values(11, "M2", "2026-06-21 10:00:00", "2026-06-21 11:00:00", 1)
                        .build(),

                insertInto("meeting_participants")
                        .columns("id", "meeting_id", "user_id", "status")
                        .values(100, 10, 1, "ACCEPTED")
                        .values(101, 11, 1, "ACCEPTED")
                        .build()
        ));

        User user = userRepository.findByUsername("alice").orElseThrow();

        String ics = iCalService.render(user,
                meetingRepository.findCalendarMeetings(user));

        assertEquals(2,
                ics.split("BEGIN:VEVENT", -1).length - 1);
    }

    @Test
    @Transactional
    void shouldOrderEventsByStartTime() {

        execute(sequenceOf(
                insertInto("users")
                        .columns("id", "username", "email", "password_hash", "ical_token")
                        .values(1, "alice", "a@test.com", "pass", "token1")
                        .build(),

                insertInto("meetings")
                        .columns("id", "title", "start_time", "end_time", "organizer_id")
                        .values(10, "M1", "2026-06-22 10:00:00", "2026-06-22 11:00:00", 1)
                        .values(11, "M2", "2026-06-20 10:00:00", "2026-06-20 11:00:00", 1)
                        .build(),

                insertInto("meeting_participants")
                        .columns("id", "meeting_id", "user_id", "status")
                        .values(100, 10, 1, "ACCEPTED")
                        .values(101, 11, 1, "ACCEPTED")
                        .build()
        ));

        User user = userRepository.findByUsername("alice").orElseThrow();

        String ics = iCalService.render(user,
                meetingRepository.findCalendarMeetings(user));

        int firstM2 = ics.indexOf("M2");
        int firstM1 = ics.indexOf("M1");

        assertTrue(firstM2 < firstM1);
    }

}