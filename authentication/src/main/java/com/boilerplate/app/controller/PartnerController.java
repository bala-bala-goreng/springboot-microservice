package com.boilerplate.app.controller;

import com.boilerplate.app.model.dto.PartnerRequest;
import com.boilerplate.app.model.entity.Partner;
import com.boilerplate.app.model.response.ResponsePartner;
import com.boilerplate.app.service.partner.PartnerService;
import com.boilerplate.app.util.ResponseHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/partners")
@RequiredArgsConstructor
@Tag(name = "Partner Management", description = "Partner management endpoints. API keys are now part of the Partner document.")
public class PartnerController {

    private final PartnerService partnerService;

    @Operation(summary = "Create a new partner", description = "Creates a new partner with specified authentication method")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Partner created successfully"),
        @ApiResponse(responseCode = "409", description = "Partner code already exists")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createPartner(@Valid @RequestBody PartnerRequest request) {
        try {
            ResponsePartner response = partnerService.createPartner(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (PartnerService.PartnerAlreadyExistsException e) {
            return ResponseHelper.conflict(e.getMessage());
        }
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Partner>> getAllPartners() {
        List<Partner> partners = partnerService.getAllPartners();
        return ResponseEntity.ok(partners);
    }

    @GetMapping(value = "/{partnerCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPartner(@PathVariable String partnerCode) {
        return partnerService.getPartnerByCode(partnerCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/{partnerCode}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> activatePartner(@PathVariable String partnerCode) {
        return partnerService.activatePartner(partnerCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/{partnerCode}/deactivate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deactivatePartner(@PathVariable String partnerCode) {
        return partnerService.deactivatePartner(partnerCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Note: API key management endpoints have been removed.
    // API keys are now part of the Partner document and can be set/updated when creating/updating a partner.
    // Use PUT /partners/{partnerCode} to update API key fields (apiKey, secretKey, publicKey, privateKey, apiKeyExpiresAt).
}

