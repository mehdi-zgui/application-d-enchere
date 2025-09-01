package com.example.auction.service;

import com.example.auction.model.User;
import com.example.auction.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository repo;
    public UserService(UserRepository repo) { this.repo = repo; }
    public User save(User u) { return repo.save(u); }
    public Optional<User> findByUsername(String username) { return repo.findByUsername(username); }
}
