package com.iggroup.sample.service.dto;

import lombok.Data;

@Data
public class UserInformationResponse {
    private String sub;
    private String updated_at;
    private String name;
    private String family_name;
}
