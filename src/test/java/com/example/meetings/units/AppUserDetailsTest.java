package com.example.meetings.units;

import com.example.meetings.model.User;
import com.example.meetings.repository.UserRepository;
import com.example.meetings.service.AppUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AppUserDetailsService service;

    @Test
    void shouldLoadExistingUser() {
        User user = new User("john", "john@gmail.com", "hashed");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        UserDetails result = service.loadUserByUsername("john");

        assertEquals("john", result.getUsername());
        assertEquals("hashed", result.getPassword());
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("john"));
    }
}