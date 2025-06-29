package com.asaf.whatnext.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/events/**").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic()
            .exceptionHandling()
            .accessDeniedHandler(new AccessDeniedHandler() {
                @Override
                public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
                    System.err.println("[SECURITY] AccessDeniedException: " + accessDeniedException.getMessage());
                    accessDeniedException.printStackTrace();
                    System.err.println("[SECURITY] Request URI: " + request.getRequestURI());
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Yetkisiz erişim: " + accessDeniedException.getMessage());
                }
            })
            .authenticationEntryPoint(new AuthenticationEntryPoint() {
                @Override
                public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
                    System.err.println("[SECURITY] AuthenticationException: " + authException.getMessage());
                    authException.printStackTrace();
                    System.err.println("[SECURITY] Request URI: " + request.getRequestURI());
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Kimlik doğrulama hatası: " + authException.getMessage());
                }
            });
        return http.build();
    }

    @Bean
    public UserDetailsService users() {
        UserDetails user = User.withUsername("admin")
            .password("{noop}asdqwe123.")
            .roles("ADMIN")
            .build();
        return new InMemoryUserDetailsManager(user);
    }
} 