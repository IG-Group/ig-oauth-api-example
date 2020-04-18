package com.iggroup.sample.controller;

import com.iggroup.sample.service.OAuthClient;
import com.iggroup.sample.service.OAuthSession;
import com.iggroup.sample.service.dto.AccessTokenResponse;
import com.iggroup.sample.service.dto.UserInformationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;

@Controller
@Slf4j
public class AuthorizationHandlerController {

   private final OAuthClient oAuthClient;
   private final OAuthSession oAuthSession;

   @Autowired
   public AuthorizationHandlerController(OAuthClient oAuthClient, OAuthSession oAuthSession) {
      this.oAuthClient = oAuthClient;
      this.oAuthSession = oAuthSession;
   }

   @RequestMapping("/authorization-handler")
   public void handle(@RequestParam MultiValueMap<String, String> queryParameters, HttpServletResponse httpResponse) throws Exception {
      if (queryParameters.containsKey("error")) {
         log.error(queryParameters.getFirst("error_description"));
         return;
      }
      String authorizationCode = queryParameters.getFirst("code");
      String state = queryParameters.getFirst("state");
      String redirectUri = oAuthSession.remove(state);
      if (redirectUri == null) {
         log.error("The request state={} does not exist", state);
         return;
      }
      log.info("Received authorization code={}", authorizationCode);

      AccessTokenResponse accessTokenResponse = oAuthClient.getAccessToken(authorizationCode);
      log.info("Access token response for authorization code={}: {}", authorizationCode, accessTokenResponse);

      String accessToken = accessTokenResponse.getAccess_token();
      UserInformationResponse userInformationResponse = oAuthClient.getUserInformation(accessToken);
      log.info("User information for access token={}: {}", accessToken, userInformationResponse);

      // Store refresh token
      String clientId = userInformationResponse.getSub();
      String refreshToken = accessTokenResponse.getRefresh_token();
      String expiresIn = accessTokenResponse.getExpires_in();

      httpResponse.sendRedirect(String.format("%s?access_token=%s&refresh_token=%s&client_id=%s",
                                              redirectUri, accessToken, refreshToken, clientId));
   }
}