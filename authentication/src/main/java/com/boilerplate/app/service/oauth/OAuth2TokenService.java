package com.boilerplate.app.service.oauth;

import com.boilerplate.app.model.entity.Partner;
import com.boilerplate.app.model.response.OAuth2TokenResponse;
import com.boilerplate.app.repository.PartnerRepository;
import com.boilerplate.app.service.TokenAuthenticationService;
import com.boilerplate.app.util.BasicAuthValidator;
import com.boilerplate.app.util.ErrorCodeConstants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2TokenService {

    private final TokenAuthenticationService tokenAuthenticationService;
    private final PartnerRepository partnerRepository;

    @Value("${jwt.expiration:3600}")
    private Long jwtExpiration;

    @Value("${oauth.token.scope:resource.WRITE resource.READ}")
    private String scope;

    public OAuth2TokenResponse generateToken(String authorizationHeader, String grantType) throws OAuth2TokenException {
        // Validate grant_type
        if (!"client_credentials".equals(grantType)) {
            throw new OAuth2TokenException(
                ErrorCodeConstants.System.HTTP_STATUS_400,
                ErrorCodeConstants.Message.CODE_400_01,
                "Invalid grant_type. Only 'client_credentials' is supported"
            );
        }

        // Validate Basic Auth and extract credentials
        BasicAuthValidator.BasicAuthCredentials credentials;
        try {
            credentials = BasicAuthValidator.validateAndExtract(authorizationHeader);
        } catch (BasicAuthValidator.BasicAuthException e) {
            throw new OAuth2TokenException(
                ErrorCodeConstants.System.HTTP_STATUS_401,
                ErrorCodeConstants.System.CODE_401_00,
                "Invalid Basic authentication: " + e.getMessage()
            );
        }

        // Find partner from database using clientId (partnerCode or apiKey) and clientSecret
        Optional<Partner> partnerOpt = partnerRepository
            .findByPartnerCodeAndClientSecretAndActiveTrue(credentials.getClientId(), credentials.getClientSecret())
            .or(() -> partnerRepository
                .findByApiKeyAndClientSecretAndActiveTrue(credentials.getClientId(), credentials.getClientSecret()));

        if (partnerOpt.isEmpty()) {
            throw new OAuth2TokenException(
                ErrorCodeConstants.System.HTTP_STATUS_401,
                ErrorCodeConstants.System.CODE_401_00,
                "Invalid client credentials or partner is inactive"
            );
        }

        Partner partner = partnerOpt.get();

        String accessToken = tokenAuthenticationService.generateToken(partner);

        log.info("OAuth2 token generated successfully for partner: {}", partner.getPartnerCode());
        return new OAuth2TokenResponse(
            accessToken,
            "Bearer",
            String.valueOf(jwtExpiration),
            scope
        );
    }

    @Getter
    @RequiredArgsConstructor
    public static class OAuth2TokenException extends Exception {
        private final int httpStatus;
        private final String responseCode;
        private final String responseMessage;
    }
}

