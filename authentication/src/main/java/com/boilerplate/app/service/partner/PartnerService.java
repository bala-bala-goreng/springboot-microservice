package com.boilerplate.app.service.partner;

import com.boilerplate.app.model.dto.PartnerRequest;
import com.boilerplate.app.model.entity.Partner;
import com.boilerplate.app.model.response.ResponseMessage;
import com.boilerplate.app.model.response.ResponsePartner;
import com.boilerplate.app.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerService {

    private final PartnerRepository partnerRepository;

    public ResponsePartner createPartner(PartnerRequest request) {
        if (partnerRepository.findByPartnerCode(request.getPartnerCode()).isPresent()) {
            throw new PartnerAlreadyExistsException("Partner code already exists: " + request.getPartnerCode());
        }

        Partner partner = new Partner();
        partner.setId(java.util.UUID.randomUUID().toString());
        partner.setPartnerCode(request.getPartnerCode());
        partner.setPartnerName(request.getPartnerName());
        
        // Set B2B authentication fields if provided
        if (request.getClientSecret() != null) {
            partner.setClientSecret(request.getClientSecret());
        }
        if (request.getPartnerPublicKey() != null) {
            partner.setPartnerPublicKey(request.getPartnerPublicKey());
        }
        if (request.getPaymentNotifyUrl() != null) {
            partner.setPaymentNotifyUrl(request.getPaymentNotifyUrl());
        }
        
        // Set API key fields if provided
        if (request.getApiKey() != null && !request.getApiKey().isEmpty()) {
            partner.setApiKey(request.getApiKey());
        } else if (request.getApiKey() == null || request.getApiKey().isEmpty()) {
            // Auto-generate API key if not provided (using UUID)
            partner.setApiKey(java.util.UUID.randomUUID().toString().replace("-", ""));
        }
        if (request.getPublicKey() != null) {
            partner.setPublicKey(request.getPublicKey());
        }
        if (request.getPrivateKey() != null) {
            partner.setPrivateKey(request.getPrivateKey());
        }
        if (request.getApiKeyExpiresAt() != null) {
            partner.setApiKeyExpiresAt(request.getApiKeyExpiresAt());
        }
        
        if (request.getCreatedBy() != null) {
            partner.setCreatedBy(request.getCreatedBy());
        }
        
        partner = partnerRepository.save(partner);

        log.info("Partner created: {}", partner.getPartnerCode());
        return ResponsePartner.from(partner);
    }

    public List<Partner> getAllPartners() {
        return partnerRepository.findAll();
    }

    public Optional<ResponsePartner> getPartnerByCode(String partnerCode) {
        return partnerRepository.findByPartnerCode(partnerCode)
                .map(ResponsePartner::from);
    }

    public Optional<ResponseMessage> activatePartner(String partnerCode) {
        return partnerRepository.findByPartnerCode(partnerCode)
                .map(partner -> {
                    partner.setActive(true);
                    partner = partnerRepository.save(partner);
                    log.info("Partner activated: {}", partnerCode);
                    return new ResponseMessage("Partner activated", partner.getPartnerCode());
                });
    }

    public Optional<ResponseMessage> deactivatePartner(String partnerCode) {
        return partnerRepository.findByPartnerCode(partnerCode)
                .map(partner -> {
                    partner.setActive(false);
                    partner = partnerRepository.save(partner);
                    log.info("Partner deactivated: {}", partnerCode);
                    return new ResponseMessage("Partner deactivated", partner.getPartnerCode());
                });
    }

    public Partner findPartnerByCodeOrThrow(String partnerCode) {
        return partnerRepository.findByPartnerCode(partnerCode)
                .orElseThrow(() -> new PartnerNotFoundException("Partner not found: " + partnerCode));
    }

    public static class PartnerAlreadyExistsException extends RuntimeException {
        public PartnerAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class PartnerNotFoundException extends RuntimeException {
        public PartnerNotFoundException(String message) {
            super(message);
        }
    }
}

