package ru.otus.hw.web.controller;

import jakarta.servlet.http.HttpServletRequest;

public final class HtmxRequests {

  private HtmxRequests() {
  }

  public static boolean fragment(HttpServletRequest request) {
    return "/doctors".equals(request.getRequestURI()) && "true".equalsIgnoreCase(
      request.getHeader("HX-Request"))
      && !"true".equalsIgnoreCase(request.getHeader("HX-History-Restore-Request"));
  }
}
