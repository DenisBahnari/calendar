package com.example.meetings.units;

import com.example.meetings.model.User;
import com.example.meetings.repository.UserRepository;
import com.example.meetings.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService service;

    @Test
    void shouldRegisterUser() {
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(passwordEncoder.encode("123")).thenReturn("HASH");

        User savedUser = new User("john","john@mail.com","HASH");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        User result = service.register("john","john@mail.com","123");

        assertEquals("john", result.getUsername());
        verify(passwordEncoder).encode("123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldRejectDuplicateUsername() {
        when(userRepository.existsByUsername("john")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.register("john","john@mail.com", "123456"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldFindUserByUsername() {
        User user = new User("john", "john@gmail.com", "hashed");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        assertEquals(user, service.requireByUsername("john"));
    }

    @Test
    void shouldThrowForUnknownUser() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.requireByUsername("john"));
    }

}
