package com.iggroup.sample.controller;

import com.iggroup.sample.service.OAuthSession;
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

   private final OAuthSession oAuthSession;

   @Autowired
   public AuthorizationHandlerController(OAuthSession oAuthSession) {
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

      httpResponse.sendRedirect(String.format("%s?code=%s", redirectUri, authorizationCode));
   }
}