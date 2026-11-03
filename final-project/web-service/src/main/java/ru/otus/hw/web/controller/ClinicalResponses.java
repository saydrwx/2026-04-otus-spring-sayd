package ru.otus.hw.web.controller;

import java.util.UUID;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;
import ru.otus.hw.web.exception.ClinicalException;

import static ru.otus.hw.web.exception.ClinicalException.Kind.*;

final class ClinicalResponses {

  private ClinicalResponses() {
  }

  static String actionError(ClinicalException exception, UUID id, boolean doctor,
    HttpServletResponse response, Model model, String message) {
    response.setStatus(exception.kind().status().value());
    model.addAttribute("doctorView", doctor);
    model.addAttribute("appointmentId", id);
    model.addAttribute("message",
      exception.kind() == NOT_FOUND || exception.kind() == FORBIDDEN ? exception.kind().message()
        : message);
    return "clinical/error";
  }
}
