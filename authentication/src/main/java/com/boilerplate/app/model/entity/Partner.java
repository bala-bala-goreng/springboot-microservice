package com.boilerplate.app.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "partners", schema = "authentication", indexes = {
    @Index(name = "idx_partners_partner_code", columnList = "partner_code", unique = true),
    @Index(name = "idx_partners_api_key", columnList = "api_key", unique = true)
})
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Partner {

    @Id
    @Column(name = "id", length = 255)
    private String id;

    @Column(name = "partner_code", length = 255, unique = true, nullable = false)
    private String partnerCode;

    @Column(name = "partner_name", length = 255)
    private String partnerName;

    @Column(name = "client_secret", length = 255)
    private String clientSecret;
    
    @Column(name = "partner_public_key", columnDefinition = "TEXT")
    private String partnerPublicKey;

    @Column(name = "payment_notify_url", length = 500)
    private String paymentNotifyUrl;

    @Column(name = "api_key", length = 255, unique = true)
    private String apiKey;

    @Column(name = "public_key", columnDefinition = "TEXT")
    private String publicKey;
    
    @Column(name = "private_key", columnDefinition = "TEXT")
    private String privateKey;
    
    @Column(name = "api_key_expires_at")
    private LocalDateTime apiKeyExpiresAt;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
