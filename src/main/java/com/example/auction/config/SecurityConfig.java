package com.example.auction.config;

import com.example.auction.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import java.io.IOException;
import java.util.Map;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public UserDetailsService userDetailsService(UserRepository users) {
        return username -> users.findByUsername(username)
            .map(u -> User.withUsername(u.getUsername()).password(u.getPassword()).roles(u.getRole()).build())
            .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable"));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, UserDetailsService users,
                                           PasswordEncoder encoder, ObjectMapper json) throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        http.authenticationProvider(provider)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/csrf", "/api/auth/signup", "/api/auth/login", "/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/products").hasRole("SELLER")
                .requestMatchers(HttpMethod.POST, "/api/products/*/bids").hasRole("BUYER")
                .anyRequest().authenticated())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((req, res, ex) -> error(res, json, 401, "Authentification requise"))
                .accessDeniedHandler((req, res, ex) -> error(res, json, 403, "Accès refusé")))
            .formLogin(form -> form.loginPage("/api/auth/login").loginProcessingUrl("/api/auth/login")
                .successHandler((req, res, auth) -> {
                    res.setContentType("application/json");
                    json.writeValue(res.getOutputStream(), Map.of("username", auth.getName()));
                })
                .failureHandler((req, res, ex) -> error(res, json, 401, "Identifiants invalides")))
            .logout(logout -> logout.logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)))
            .requestCache(cache -> cache.disable());
        return http.build();
    }

    private static void error(HttpServletResponse response, ObjectMapper json, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        json.writeValue(response.getOutputStream(), Map.of("status", status, "detail", message));
    }
}
