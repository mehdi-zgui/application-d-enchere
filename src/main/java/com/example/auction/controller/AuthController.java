package com.example.auction.controller;

import com.example.auction.dto.ApiDtos.*;
import com.example.auction.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService users;
    public AuthController(UserService users) { this.users = users; }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserView signup(@Valid @RequestBody Signup input) {
        return users.register(input);
    }

    @GetMapping("/me")
    public UserView me(Authentication authentication) {
        return UserView.from(users.require(authentication.getName()));
    }
}
