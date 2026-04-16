package com.agroruta.user.application.ports.out;

import org.springframework.security.core.userdetails.UserDetails;

public interface TokenPort {
    String generateToken(UserDetails userDetails);
    String extractUsername(String token);
    boolean isTokenValid(String token, UserDetails userDetails);
}