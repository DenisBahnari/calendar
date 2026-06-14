package com.example.meetings.integrations.db;

import com.example.meetings.model.InviteStatus;
import com.example.meetings.repository.MeetingParticipantRepository;
import com.example.meetings.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static org.junit.jupiter.api.Assertions.*;

class MeetingParticipantDBIT extends BaseDBSetupIT {

    @Autowired
    private MeetingParticipantRepository participantRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldReturnPendingInvitesOnly() {

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
                        .values(100, 10, 2, "PENDING")
                        .values(101, 10, 1, "ACCEPTED")
                        .build()
        ));

        var bob = userRepository.findByUsername("bob").get();

        var pending = participantRepository.findByUserAndStatus(bob, InviteStatus.PENDING);

        assertEquals(1, pending.size());
        assertEquals("PENDING", pending.get(0).getStatus().name());
    }

    @Test
    void shouldReturnEmptyWhenNoPendingInvites() {

        execute(sequenceOf(
                insertInto("users")
                        .columns("id", "username", "email", "password_hash", "ical_token")
                        .values(1, "bob", "b@test.com", "pass", "t2")
                        .build()
        ));

        var bob = userRepository.findByUsername("bob").get();

        var pending = participantRepository.findByUserAndStatus(bob, InviteStatus.PENDING);

        assertTrue(pending.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenNoParticipantsExist() {

        execute(sequenceOf(
                insertInto("users")
                        .columns("id", "username", "email", "password_hash", "ical_token")
                        .values(1, "alice", "a@test.com", "pass", "t1")
                        .build(),

                insertInto("meetings")
                        .columns("id", "title", "start_time", "end_time", "organizer_id")
                        .values(10, "M1", "2026-06-20 10:00:00", "2026-06-20 11:00:00", 1)
                        .build()
        ));

        var alice = userRepository.findByUsername("alice").get();

        var pending = participantRepository.findByUserAndStatus(alice, InviteStatus.PENDING);

        assertTrue(pending.isEmpty());
    }

    @Test
    void shouldFilterByMeetingCorrectly() {

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
                        .build(),

                insertInto("meeting_participants")
                        .columns("id", "meeting_id", "user_id", "status")
                        .values(100, 10, 2, "PENDING")
                        .values(101, 11, 2, "ACCEPTED")
                        .build()
        ));

        var bob = userRepository.findByUsername("bob").orElseThrow();

        var pending = participantRepository.findByUserAndStatus(bob, InviteStatus.PENDING);

        assertEquals(1, pending.size());
        assertEquals(10L, pending.get(0).getMeeting().getId());
    }

    @Test
    void shouldNotReturnAcceptedWhenFilteringPending() {

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
                        .values(100, 10, 2, "ACCEPTED")
                        .build()
        ));

        var bob = userRepository.findByUsername("bob").orElseThrow();

        var pending = participantRepository.findByUserAndStatus(bob, InviteStatus.PENDING);

        assertTrue(pending.isEmpty());
    }

    @Test
    void shouldNotReturnDeclinedWhenFilteringPending() {

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
                        .values(100, 10, 2, "DECLINED")
                        .build()
        ));

        var bob = userRepository.findByUsername("bob").orElseThrow();

        var pending = participantRepository.findByUserAndStatus(bob, InviteStatus.PENDING);

        assertTrue(pending.isEmpty());
    }
}