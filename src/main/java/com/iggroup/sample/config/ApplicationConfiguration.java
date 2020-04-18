package com.iggroup.sample.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
@Slf4j
public class ApplicationConfiguration {

   @Bean
   @Qualifier(value = "redirect.base.url")
   public String redirectUrl(Environment env) throws UnknownHostException {
      String redirectServerUrl = env.getProperty("redirect.server.url");
      String serverPort = env.getProperty("server.port");
      String contextPath = env.getProperty("server.servlet.context-path");
      String redirectBaseUrl = (redirectServerUrl.isBlank() ? "http://" + InetAddress.getLocalHost().getHostName() + ":" + serverPort : redirectServerUrl) + contextPath;
      log.info("Server redirectBaseUrl={}", redirectBaseUrl);
      return redirectBaseUrl;
   }
}
