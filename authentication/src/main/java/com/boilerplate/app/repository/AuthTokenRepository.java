package com.boilerplate.app.repository;

import com.boilerplate.app.model.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, String> {
    Optional<AuthToken> findByToken(String token);
    Optional<AuthToken> findByTokenAndRevokedFalseAndExpiresAtAfter(String token, LocalDateTime now);
    List<AuthToken> findByPartnerIdAndRevokedFalse(String partnerId);
}
