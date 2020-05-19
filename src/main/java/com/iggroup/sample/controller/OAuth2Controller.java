package com.iggroup.sample.controller;

import com.iggroup.sample.service.OAuthClient;
import com.iggroup.sample.service.OAuthSession;
import com.iggroup.sample.service.dto.AccessTokenResponse;
import com.iggroup.sample.service.dto.AuthorizationCodeRequest;
import com.iggroup.sample.service.dto.TokenResponse;
import com.iggroup.sample.service.dto.UserInformationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
public class OAuth2Controller {

   private final OAuthClient oAuthClient;
   private final OAuthSession oAuthSession;
   private final String redirectBaseUrl;
   private final String oAuthServerUrl;
   private final String clientId;
   private final String realm;

   @Autowired
   public OAuth2Controller(OAuthClient oAuthClient,
                          OAuthSession oAuthSession,
                          @Qualifier(value = "redirect.base.url") String redirectBaseUrl,
                          @Value("${ig.oauth.server}") String oAuthServerUrl,
                          @Value("${ig.oauth.client.id}") String clientId,
                          @Value("${ig.oauth.realm}") String realm) {

      this.oAuthClient = oAuthClient;
      this.oAuthSession = oAuthSession;
      this.redirectBaseUrl = redirectBaseUrl;
      this.oAuthServerUrl = oAuthServerUrl;
      this.clientId = clientId;
      this.realm = realm;
   }

   @CrossOrigin
   @RequestMapping("/oauth2/authorize")
   public void oauth2ProviderRedirection(@RequestParam MultiValueMap<String, String> queryParameters, HttpServletResponse httpResponse)
      throws IOException {
      String state = UUID.randomUUID().toString();
      String redirectUri = queryParameters.getFirst("redirect_uri");

      if (redirectUri == null) {
         log.warn("Missing redirect_uri parameter in the call");
         throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing redirect_uri param");
      }
      String url = oAuthServerUrl + "/oauth2/authorize";
      String params = String.format("?response_type=code&client_id=%s&redirect_uri=%s&scope=full_readonly&realm=%s&state=%s",
                                    clientId, redirectBaseUrl + "/authorization-handler", realm, state);

      String oldUri = oAuthSession.add(state, redirectUri);
      if (oldUri != null) {
         log.info("The mapping for state={} was already present and will be overwritten by={}.", state, oldUri);
      }
      httpResponse.sendRedirect(url + params);
   }

   @CrossOrigin
   @PostMapping("/oauth2/token")
   public TokenResponse accessToken(@RequestBody AuthorizationCodeRequest authorizationCodeRequest, HttpServletResponse response) {
      try {
         String authorizationCode = authorizationCodeRequest.getCode();

         AccessTokenResponse accessTokenResponse = oAuthClient.getAccessToken(authorizationCode);
         log.info("Access token response for authorization code={}: {}", authorizationCode, accessTokenResponse);

         String accessToken = accessTokenResponse.getAccess_token();
         UserInformationResponse userInformationResponse = oAuthClient.getUserInformation(accessToken);
         log.info("User information for access token={}: {}", accessToken, userInformationResponse);

         String clientId = userInformationResponse.getSub();
         String refreshToken = accessTokenResponse.getRefresh_token();
         String expiresIn = accessTokenResponse.getExpires_in();

         response.addCookie(createCookie(refreshToken));

         return new TokenResponse(accessToken, expiresIn);

      } catch (Exception e) {
         log.error("Unexpected exception occurred", e);
         throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized", e);
      }
   }

   @CrossOrigin
   @GetMapping("/oauth2/refresh")
   public TokenResponse refreshToken(@CookieValue(name = "refreshToken") String refreshToken, HttpServletResponse response) {
      try {
         AccessTokenResponse accessTokenResponse = oAuthClient.refreshAccessToken(refreshToken);
         log.info("Access token response: {}", accessTokenResponse);

         String accessToken = accessTokenResponse.getAccess_token();
         String expiresIn = accessTokenResponse.getExpires_in();

         response.addCookie(createCookie(refreshToken));
         return new TokenResponse(accessToken, expiresIn);

      } catch (Exception e) {
         log.error("Unexpected exception occurred", e);
         throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized", e);
      }
   }

   private Cookie createCookie(String refreshToken) {
      final Cookie cookie = new Cookie("refreshToken", refreshToken);
      cookie.setSecure(true);
      cookie.setHttpOnly(true);
      cookie.setMaxAge((int) TimeUnit.DAYS.toSeconds(3));
      return cookie;
   }
}
