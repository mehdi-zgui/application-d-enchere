package com.example.auction.config;

import com.example.auction.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final UserService userService;

    @Autowired
    public SecurityConfig(UserService userService) {
        this.userService = userService;
    }

    // 1) PasswordEncoder bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2) AuthenticationProvider bean qui utilise userService + encoder
    @SuppressWarnings("deprecation")
	@Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        // customer UserDetailsService
        provider.setUserDetailsService(new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                return userService.findByUsername(username)
                    .map(u -> org.springframework.security.core.userdetails.User
                        .withUsername(u.getUsername())
                        .password(u.getPassword())
                        .roles(u.getRole())
                        .build()
                    )
                    .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
            }
        });
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // 3) Security filter chain qui enregistre notre provider
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
          .authenticationProvider(authenticationProvider())
          .authorizeHttpRequests(auth -> auth
             .requestMatchers("/login","/signup","/css/**").permitAll()
             .anyRequest().authenticated()
          )
          .formLogin(form -> form
             .loginPage("/login").defaultSuccessUrl("/products", true).permitAll()
          )
          .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll());

        return http.build();
    }
}

