package com.iggroup.sample.service;

import com.iggroup.sample.service.dto.AccessTokenResponse;
import com.iggroup.sample.service.dto.UserInformationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

@Service
@Slf4j
public class OAuthClient {

   private final WebClient webClient;
   private final String redirectBaseUrl;
   private final String authorizationHeader;
   private final String realm;

   @Autowired
   public OAuthClient(WebClient.Builder webClientBuilder,
                      @Qualifier(value = "redirect.base.url") String redirectBaseUrl,
                      @Value("${ig.oauth.server}") String oauthServerUrl,
                      @Value("${ig.oauth.client.id}") String clientId,
                      @Value("${ig.oauth.client.secret}") String clientSecret,
                      @Value("${ig.oauth.realm}") String realm) throws UnsupportedEncodingException {

      this.webClient = webClientBuilder
         .baseUrl(oauthServerUrl)
         .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
         .build();

      this.redirectBaseUrl = redirectBaseUrl;
      this.realm = realm;
      this.authorizationHeader = authorizationHeader(clientId, clientSecret);
   }

   public AccessTokenResponse getAccessToken(String authorizationCode) throws Exception {
      log.info("Requesting access token for authorization code={} redirectBaseUrl={}", authorizationCode, redirectBaseUrl);

      ClientResponse response = this.webClient
         .post()
         .uri(uriBuilder -> uriBuilder
              .path("/oauth2/access_token")
              .queryParam("grant_type", "authorization_code")
              .queryParam("code", authorizationCode)
              .queryParam("realm", realm)
              .queryParam("redirect_uri", redirectBaseUrl + "/authorization-handler")
              .build()
         )
         .header(HttpHeaders.AUTHORIZATION, this.authorizationHeader)
         .exchange()
         .block();

      if (response.statusCode() != HttpStatus.OK) {
         throw new Exception("Failed with status=" + response.statusCode());
      }
      return response.toEntity(AccessTokenResponse.class).block().getBody();
   }

   public AccessTokenResponse refreshAccessToken(String refreshToken) throws Exception {
      log.debug("Requesting access token for refreshToken={}", refreshToken);

      ClientResponse response = this.webClient
         .post()
         .uri(uriBuilder -> uriBuilder
            .path("/oauth2/access_token")
            .queryParam("grant_type", "refresh_token")
            .queryParam("refresh_token", refreshToken)
            .queryParam("realm", realm)
            .build()
         )
         .header(HttpHeaders.AUTHORIZATION, this.authorizationHeader)
         .exchange()
         .block();

      if (response.statusCode() != HttpStatus.OK) {
         throw new Exception("Failed with status=" + response.statusCode());
      }
      return response.toEntity(AccessTokenResponse.class).block().getBody();
   }

   public UserInformationResponse getUserInformation(String accessToken) throws Exception {
      log.info("Requesting user information for access token={}", accessToken);

      ClientResponse response = this.webClient
         .post()
         .uri("/oauth2/userinfo")
         .header(HttpHeaders.AUTHORIZATION, authorizationHeader(accessToken))
         .exchange()
         .block();

      if (response.statusCode() != HttpStatus.OK) {
         throw new Exception("Failed with status=" + response.statusCode());
      }
      return response.toEntity(UserInformationResponse.class).block().getBody();
   }

   private static String authorizationHeader(String clientId, String clientSecret) throws UnsupportedEncodingException {
      String credentials = clientId + ':' + clientSecret;
      String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes("UTF-8"));
      String header = "Basic " + encodedCredentials;
      log.debug("Authorization header={}", header);
      return header;
   }

   private static String authorizationHeader(String accessToken) {
      String header = "Bearer " + accessToken;
      log.debug("Authorization header={}", header);
      return header;
   }
}
