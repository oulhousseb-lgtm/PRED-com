package com.pred.pred_api.service;

import com.pred.pred_api.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtUtil jwtUtil;

    public String generateToken(String email) {
        return jwtUtil.generateToken(email);
    }

    public String extractEmail(String token) {
        return jwtUtil.extractEmail(token);
    }

    public boolean validateToken(String token) {
        return jwtUtil.isTokenValid(token);
    }
}
