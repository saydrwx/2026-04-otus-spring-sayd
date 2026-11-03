package ru.otus.hw.authorization.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RussianLoginController {

  @GetMapping("/login")
  public String login(HttpServletRequest request, Model model) {
    model.addAttribute("loginError", request.getParameterMap().containsKey("error"));
    model.addAttribute("loggedOut", request.getParameterMap().containsKey("logout"));
    return "auth/login";
  }
}
