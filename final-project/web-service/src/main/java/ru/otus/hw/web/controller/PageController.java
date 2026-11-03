package ru.otus.hw.web.controller;

import org.springframework.stereotype.Controller;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

  @GetMapping("/")
  public String home() {
    return "home";
  }

  @GetMapping("/login")
  public String login(@AuthenticationPrincipal OidcUser user, HttpServletRequest request,
    Model model) {
    if (user != null) {
      return "redirect:/account";
    }
    model.addAttribute("loginError", request.getParameterMap().containsKey("error"));
    return "login";
  }

  @GetMapping("/account")
  public String account(Authentication user) {
    return user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"))
      ? "redirect:/patient/profile" : "account";
  }
}
