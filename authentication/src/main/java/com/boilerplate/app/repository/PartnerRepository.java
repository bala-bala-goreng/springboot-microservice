package com.boilerplate.app.repository;

import com.boilerplate.app.model.entity.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, String> {
    Optional<Partner> findByPartnerCode(String partnerCode);
    Optional<Partner> findByPartnerCodeAndActiveTrue(String partnerCode);
    
    // Find partner by partnerCode and clientSecret for OAuth2 authentication
    Optional<Partner> findByPartnerCodeAndClientSecretAndActiveTrue(String partnerCode, String clientSecret);
    
    // Find partner by API key (consolidated from ApiKeyRepository)
    Optional<Partner> findByApiKey(String apiKey);
    Optional<Partner> findByApiKeyAndActiveTrue(String apiKey);
    
    // Find partner by API key and clientSecret for OAuth2 authentication
    Optional<Partner> findByApiKeyAndClientSecretAndActiveTrue(String apiKey, String clientSecret);
}
