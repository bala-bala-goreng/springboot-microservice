package com.boilerplate.app.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_tokens", schema = "authentication", indexes = {
    @Index(name = "idx_auth_tokens_token", columnList = "token", unique = true),
    @Index(name = "idx_auth_tokens_partner_id", columnList = "partner_id")
})
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class AuthToken {

    @Id
    @Column(name = "id", length = 255)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", foreignKey = @ForeignKey(name = "fk_auth_tokens_partner"))
    private Partner partner;

    @Column(name = "token", length = 500, unique = true, nullable = false)
    private String token;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked")
    private Boolean revoked = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
}
