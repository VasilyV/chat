package com.example.chat.service;

import com.example.chat.persistence.User;
import com.example.chat.persistence.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean userExists(String username) {
        return userRepository.existsById(username);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findById(username);
    }

    public void save(User user) {
        userRepository.save(user);
    }
}
