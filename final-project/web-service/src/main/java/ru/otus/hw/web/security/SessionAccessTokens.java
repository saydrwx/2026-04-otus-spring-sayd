package ru.otus.hw.web.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;

public interface SessionAccessTokens {

  String current(Authentication principal, HttpServletRequest request,
    HttpServletResponse response);

  void expire(Authentication principal, HttpServletRequest request, HttpServletResponse response);
}
