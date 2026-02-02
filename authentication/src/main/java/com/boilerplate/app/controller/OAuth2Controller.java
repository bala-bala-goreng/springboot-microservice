package com.boilerplate.app.controller;

import com.boilerplate.app.model.request.RequestOAuth2Token;
import com.boilerplate.app.model.request.RequestValidateToken;
import com.boilerplate.app.model.response.OAuth2TokenResponse;
import com.boilerplate.app.model.response.ResponseError;
import com.boilerplate.app.model.response.ResponseValidateToken;
import com.boilerplate.app.service.oauth.OAuth2TokenService;
import com.boilerplate.app.service.TokenAuthenticationService;
import com.boilerplate.app.util.ErrorCodeConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
@Tag(name = "OAuth2", description = "OAuth2 token endpoints for partners")
public class OAuth2Controller {

    private final OAuth2TokenService oAuth2TokenService;
    private final TokenAuthenticationService tokenAuthenticationService;

    @Operation(
        summary = "Generate OAuth2 access token",
        description = "Generates OAuth2 access token using Client Credentials grant flow with Basic Authentication. " +
                "Requires Basic Auth header with client_id:client_secret and JSON body with grant_type=client_credentials. " +
                "Client credentials are validated against partners in the database."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token generated successfully",
            content = @Content(schema = @Schema(implementation = OAuth2TokenResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request - Invalid grant_type", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid Basic Auth credentials", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Invalid partner credentials", content = @Content)
    })
    @SecurityRequirement(name = "basicAuth")
    @PostMapping(
        value = "/token",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> getToken(
        @Parameter(description = "Authorization header with Basic Auth (client_id:client_secret)", required = true)
        @RequestHeader(value = "Authorization") String authorization,
        @Valid @RequestBody RequestOAuth2Token request
    ) {
        try {
            OAuth2TokenResponse response = oAuth2TokenService.generateToken(authorization, request.getGrantType());
            return ResponseEntity.ok(response);
        } catch (OAuth2TokenService.OAuth2TokenException e) {
            ResponseError errorResponse = new ResponseError();
            errorResponse.setResponseCode(e.getResponseCode());
            errorResponse.setResponseMessage(e.getResponseMessage());
            errorResponse.setData(new java.util.LinkedHashMap<>());
            return ResponseEntity.status(e.getHttpStatus()).body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error in getToken: {}", e.getMessage(), e);
            ResponseError errorResponse = new ResponseError();
            errorResponse.setResponseCode(ErrorCodeConstants.System.CODE_500_00);
            errorResponse.setResponseMessage(ErrorCodeConstants.System.MESSAGE_500_00);
            errorResponse.setData(new java.util.LinkedHashMap<>());
            return ResponseEntity.status(ErrorCodeConstants.System.HTTP_STATUS_500).body(errorResponse);
        }
    }

    @Operation(
        summary = "Validate OAuth2 token",
        description = "Validates if an OAuth2 JWT token is valid and not revoked"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token validation result",
            content = @Content(schema = @Schema(implementation = ResponseValidateToken.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - Token is required",
            content = @Content(schema = @Schema(implementation = ResponseError.class))
        )
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping(
        value = "/token/validate",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> validateToken(
        @Parameter(description = "Token validation request", required = true)
        @Valid @RequestBody RequestValidateToken request
    ) {
        boolean isValid = tokenAuthenticationService.validateToken(request.getToken());
        ResponseValidateToken response = new ResponseValidateToken();
        response.setValid(isValid);
        return ResponseEntity.ok(response);
    }
}
