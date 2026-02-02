package com.boilerplate.app.service;

import com.boilerplate.app.model.entity.AuthToken;
import com.boilerplate.app.model.entity.Partner;
import com.boilerplate.app.repository.AuthTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenAuthenticationService {

    private final AuthTokenRepository authTokenRepository;

    @Value("${jwt.secret:eleanor-secret-key-for-jwt-token-generation-minimum-256-bits}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600}")
    private Long jwtExpiration;

    public String generateToken(Partner partner) {
        return generateToken(partner, null);
    }

    public String generateToken(Partner partner, String clientKey) {
        return generateToken(partner, clientKey, null);
    }

    public String generateToken(Partner partner, String clientKey, String partnerId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration * 1000);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        var tokenBuilder = Jwts.builder()
                .subject(partner.getPartnerCode())
                .claim("partnerId", partner.getId())
                .claim("X-INTERNAL-PARTNER-ID", partner.getPartnerCode())
                .issuedAt(now)
                .expiration(expiryDate);

        // Add X-PARTNER-ID to JWT claims if provided
        if (partnerId != null && !partnerId.isEmpty()) {
            tokenBuilder.claim("X-PARTNER-ID", partnerId);
        }

        String token = tokenBuilder.signWith(key).compact();

        AuthToken authToken = new AuthToken();
        authToken.setId(java.util.UUID.randomUUID().toString());
        authToken.setPartner(partner);
        authToken.setToken(token);
        authToken.setExpiresAt(LocalDateTime.now().plusSeconds(jwtExpiration));
        authTokenRepository.save(authToken);

        return token;
    }

    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Optional<AuthToken> authToken = authTokenRepository
                    .findByTokenAndRevokedFalseAndExpiresAtAfter(token, LocalDateTime.now());

            return authToken.isPresent() && !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return false;
        }
    }

    public void revokeToken(String token) {
        authTokenRepository.findByToken(token).ifPresent(authToken -> {
            authToken.setRevoked(true);
            authToken.setRevokedAt(LocalDateTime.now());
            authTokenRepository.save(authToken);
        });
    }
}

