package com.example.meetings.units;

import com.example.meetings.discover.DiscoveredEvent;
import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import com.example.meetings.repository.MeetingParticipantRepository;
import com.example.meetings.repository.MeetingRepository;
import com.example.meetings.repository.UserRepository;
import com.example.meetings.service.MeetingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MeetingTest {

    @Mock
    MeetingParticipantRepository participantRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    MeetingRepository meetingRepository;

    @InjectMocks
    MeetingService service;

    @Test
    void shouldRejectMeetingWithInvalidDates() {
        Instant start = Instant.now();
        Instant end = start.minusSeconds(60);
        User user = new User("john", "john@gmail.com", "hashed");

        assertThrows(IllegalArgumentException.class,
                () -> service.propose(
                        user,
                        "Meeting",
                        "",
                        start,
                        end,
                        List.of()
                )
        );
    }

    @Test
    void shouldRejectUnknownInvitee() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(60);
        User user = new User("john", "john@gmail.com", "hashed");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.propose(
                        user,
                        "Meeting",
                        "",
                        start,
                        end,
                        List.of("alice")
                )
        );
    }

    @Test
    void organizerShouldAutoAcceptMeeting() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(60);
        User user = new User("john", "john@gmail.com", "hashed");
        when(meetingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Meeting meeting =
                service.propose(
                        user,
                        "Meeting",
                        "",
                        start,
                        end,
                        List.of()
                );

        assertEquals(InviteStatus.ACCEPTED, meeting.getParticipants().iterator().next().getStatus());
    }

    @Test
    void inviteesShouldBePending() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(60);
        User user = new User("john", "john@gmail.com", "hashed");
        User invitee = new User("alice", "alice@gmail.com", "hashed");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(invitee));
        when(meetingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Meeting meeting =
                service.propose(
                        user,
                        "Meeting",
                        "",
                        start,
                        end,
                        List.of("alice")
                );

        boolean pendingFound =
                meeting.getParticipants()
                        .stream()
                        .anyMatch(p -> p.getUser().equals(invitee)
                                        && p.getStatus() == InviteStatus.PENDING);

        assertTrue(pendingFound);
    }

    @Test
    void shouldRejectPendingResponse() {
        User user = new User("john", "john@gmail.com", "hashed");
        assertThrows(
                IllegalArgumentException.class,
                () -> service.respond(
                        (Long) 1L,
                        user,
                        InviteStatus.PENDING
                )
        );
    }

    @Test
    void shouldThrowWhenInviteDoesNotExist() {
        User user = new User("john", "john@gmail.com", "hashed");
        when(participantRepository.findByMeetingIdAndUserId(any(), any())).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.respond(
                        (Long) 1L,
                        user,
                        InviteStatus.ACCEPTED
                )
        );
    }

    @Test
    void shouldAcceptInvite() {

        User user = new User("john","john@gmail.com","hash");

        MeetingParticipant participant = mock(MeetingParticipant.class);

        when(participantRepository
                .findByMeetingIdAndUserId(any(), any()))
                .thenReturn(Optional.of(participant));

        service.respond(
                (Long) 1L,
                user,
                InviteStatus.ACCEPTED
        );

        verify(participant).setStatus(InviteStatus.ACCEPTED);
    }



    @Test
    void shouldDefaultEndTimeToTwoHours() {
        User user = new User("john","john@gmail.com","hash");
        Instant start = Instant.now();

        DiscoveredEvent event = new DiscoveredEvent(
                        "Agencia Lisboa",
                        "1",
                        "Venue",
                        "Description",
                        start,
                        null,
                        null,
                        "venue"
                );

        when(meetingRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Meeting meeting = service.copyFromDiscovered(user, event);

        assertEquals(start.plus(Duration.ofHours(2)), meeting.getEndTime());
    }

    @Test
    void shouldRejectInvalidToken() {

        when(userRepository.findByIcalToken("bad")).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.calendarForIcalToken("bad")
        );
    }

}
