package com.example.auction.service;

import com.example.auction.dto.ApiDtos.*;
import com.example.auction.model.User;
import com.example.auction.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository repo;
    private final PasswordEncoder encoder;
    public UserService(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }
    public Optional<User> findByUsername(String username) { return repo.findByUsername(username); }
    public User require(String username) {
        return findByUsername(username).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
    }
    @Transactional
    public UserView register(Signup input) {
        if (input.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mot de passe limité à 72 octets UTF-8");
        }
        String username = input.username().trim();
        if (repo.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nom d'utilisateur déjà utilisé");
        }
        return UserView.from(repo.saveAndFlush(
            new User(username, encoder.encode(input.password()), input.role().name())));
    }
}
