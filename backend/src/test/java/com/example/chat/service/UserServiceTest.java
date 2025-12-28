package com.example.chat.service;

import com.example.chat.persistence.User;
import com.example.chat.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;

    @InjectMocks UserService userService;

    @Test
    void userExists_returnsTrue() {
        when(userRepository.existsById(any())).thenReturn(true);

        boolean doesUserExist = userService.userExists("alice");

        assertThat(doesUserExist).isTrue();
        verify(userRepository).existsById("alice");
    }

    @Test
    void userExists_returnsFalse() {
        when(userRepository.existsById(any())).thenReturn(false);

        boolean doesUserExist = userService.userExists("alice");

        assertThat(doesUserExist).isFalse();
        verify(userRepository).existsById("alice");
    }

    @Test
    void findByUsername_delegatesToRepo() {
        User user = new User();
        user.setUsername("alice");

        when(userRepository.findById(any())).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByUsername("alice");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("alice");
        verify(userRepository).findById("alice");
    }

    @Test
    void save_shouldSaveUserWithUsernameAndPasswordHash() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        User user = new User();
        user.setUsername("alice");
        user.setPasswordHash("HASH");

        userService.save(user);

        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getPasswordHash()).isEqualTo("HASH");
    }
}
