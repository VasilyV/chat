package com.example.chat.service;

import com.example.chat.persistence.User;
import com.example.chat.persistence.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean userExists(String username) {
        boolean userExists = userRepository.existsById(username);
        if (userExists) {
            log.warn("User creation refused (already exists) username={}", username);
        }
        return userExists;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findById(username);
    }

    public void save(User user) {
        log.info("Creating user username={}", user.getUsername());
        userRepository.save(user);
    }
}
