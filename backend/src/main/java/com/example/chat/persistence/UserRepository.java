package com.example.chat.persistence;

import com.example.chat.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
}

