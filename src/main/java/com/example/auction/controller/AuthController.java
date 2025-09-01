package com.example.auction.controller;

import com.example.auction.model.User;
import com.example.auction.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {
    private final UserService userService;
    private final PasswordEncoder encoder;
    public AuthController(UserService us, PasswordEncoder enc) {
        this.userService = us; this.encoder = enc;
    }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/signup")
    public String signupForm(Model m) {
        m.addAttribute("user", new User());
        return "signup";
    }

    @PostMapping("/signup")
    public String signupSubmit(@ModelAttribute User user, Model m) {
        user.setPassword(encoder.encode(user.getPassword()));
        userService.save(user);
        return "redirect:/login";
    }
}
