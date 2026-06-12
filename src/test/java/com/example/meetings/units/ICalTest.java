package com.example.meetings.units;

import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import com.example.meetings.service.ICalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ICalTest {

    @InjectMocks
    ICalService service;

    @Test
    void shouldGenerateCalendar() {
        User user = new User("john","john@gmail.com","hash");
        String result =
                service.render(user, List.of());

        assertTrue(result.contains("BEGIN:VCALENDAR"));
        assertTrue(result.contains("END:VCALENDAR"));
    }

    @Test
    void confirmedMeetingShouldAppearAsConfirmed() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(60);
        User user = new User("john","john@gmail.com","hash");

        Meeting meeting = new Meeting("title", "dec", start, end, user);
        meeting.addParticipant(new MeetingParticipant(meeting, user, InviteStatus.ACCEPTED));
        String result = service.render(user,List.of(meeting));

        assertTrue(result.contains("STATUS:CONFIRMED"));
    }

    @Test
    void tentativeMeetingShouldAppearAsTentative() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(60);
        User user = new User("john","john@gmail.com","hash");
        User user2 = new User("john2","john2@gmail.com","hash2");

        Meeting meeting = new Meeting("title", "dec", start, end, user);
        meeting.addParticipant(new MeetingParticipant(meeting, user, InviteStatus.ACCEPTED));
        MeetingParticipant mp = new MeetingParticipant(meeting, user2, InviteStatus.PENDING);
        meeting.addParticipant(mp);
        String result = service.render(user,List.of(meeting));

        assertTrue(result.contains("STATUS:TENTATIVE"));
    }

}
