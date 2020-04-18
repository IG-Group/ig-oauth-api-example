package com.iggroup.sample.service.dto;

import lombok.Data;

@Data
public class AccessTokenResponse {
    private String access_token;
    private String refresh_token;
    private String scope;
    private String id_token;
    private String token_type;
    private String expires_in;
}
