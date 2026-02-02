package com.boilerplate.app.model.response;

import com.boilerplate.app.model.entity.Partner;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponsePartner {
    private String id;
    private String partnerCode;
    private String partnerName;
    private boolean active;
    
    public static ResponsePartner from(Partner partner) {
        ResponsePartner response = new ResponsePartner();
        response.setId(partner.getId());
        response.setPartnerCode(partner.getPartnerCode());
        response.setPartnerName(partner.getPartnerName());
        response.setActive(partner.getActive());
        return response;
    }
}

