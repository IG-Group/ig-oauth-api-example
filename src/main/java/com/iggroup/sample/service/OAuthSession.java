package com.iggroup.sample.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OAuthSession {

   private final Map<String, String> redirectUrls = new ConcurrentHashMap<>();

   public String add(String sessionId, String url) {
      return redirectUrls.put(sessionId, url);
   }

   public String remove(String sessionId) {
      return redirectUrls.remove(sessionId);
   }
}
