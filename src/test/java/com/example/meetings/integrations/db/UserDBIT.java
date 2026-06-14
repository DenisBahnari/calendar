package com.example.meetings.integrations.db;

import com.example.meetings.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static org.junit.jupiter.api.Assertions.*;

class UserDBIT extends BaseDBSetupIT {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindUserByUsername() {

        execute(sequenceOf(
                insertInto("users")
                        .columns("id", "username", "email", "password_hash", "ical_token")
                        .values(1, "alice", "a@test.com", "pass", "token1")
                        .build()
        ));

        var user = userRepository.findByUsername("alice");

        assertTrue(user.isPresent());
        assertEquals("alice", user.get().getUsername());
        assertEquals("a@test.com", user.get().getEmail());
    }

    @Test
    void shouldReturnEmptyWhenUserDoesNotExist() {

        execute(sequenceOf(
                insertInto("users")
                        .columns("id", "username", "email", "password_hash", "ical_token")
                        .values(1, "alice", "a@test.com", "pass", "token1")
                        .build()
        ));

        var user = userRepository.findByUsername("bob");

        assertTrue(user.isEmpty());
    }

    @Test
    void shouldNotBreakWhenMultipleUsersExist() {

        execute(sequenceOf(
                insertInto("users")
                        .columns("id", "username", "email", "password_hash", "ical_token")
                        .values(1, "alice", "a@test.com", "pass", "token1")
                        .values(2, "bob", "b@test.com", "pass", "token2")
                        .build()
        ));

        var alice = userRepository.findByUsername("alice").get();
        var bob = userRepository.findByUsername("bob").get();

        assertEquals("alice", alice.getUsername());
        assertEquals("bob", bob.getUsername());
    }
}