package com.iggroup.sample.service.dto;

import lombok.Value;

@Value
public class TokenResponse {
   private String access_token;
   private String expires_in;
}
