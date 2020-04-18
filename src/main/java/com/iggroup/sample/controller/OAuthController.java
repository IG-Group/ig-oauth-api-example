package com.iggroup.sample.controller;

import com.iggroup.sample.service.OAuthClient;
import com.iggroup.sample.service.OAuthSession;
import com.iggroup.sample.service.dto.AccessTokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@RestController
@Slf4j
public class OAuthController {

   private final OAuthClient oAuthClient;
   private final OAuthSession oAuthSession;
   private final String redirectBaseUrl;
   private final String oAuthServerUrl;
   private final String clientId;
   private final String realm;

   @Autowired
   public OAuthController(OAuthClient oAuthClient,
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

   @RequestMapping("/oauth-provider")
   public void oauth2ProviderRedirection(@RequestParam MultiValueMap<String, String> queryParameters, HttpServletResponse httpResponse)
      throws IOException {

      String state = UUID.randomUUID().toString();
      String redirectUri = queryParameters.getFirst("redirect_uri");

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
   @GetMapping("/refresh-token")
   public AccessTokenResponse refreshToken(@RequestParam MultiValueMap<String, String> queryParameters) throws Exception {
      String refreshToken = queryParameters.getFirst("refresh_token");

      AccessTokenResponse accessTokenResponse = oAuthClient.refreshAccessToken(refreshToken);
      log.info("Access token response: {}", accessTokenResponse);

      return accessTokenResponse;
   }
}
