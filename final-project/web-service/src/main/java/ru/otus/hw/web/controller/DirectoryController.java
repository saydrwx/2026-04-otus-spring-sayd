package ru.otus.hw.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.otus.hw.web.dto.DoctorSearchInput;
import ru.otus.hw.web.service.DirectoryService;

@RequiredArgsConstructor
@Controller
public class DirectoryController {

  private final DirectoryService directory;

  @GetMapping("/doctors")
  public String doctors(
    @Valid @ModelAttribute("search") DoctorSearchInput input,
    Model model,
    HttpServletRequest request,
    HttpServletResponse response
  ) {
    response.setHeader("Vary", "HX-Request, HX-History-Restore-Request");
    model.addAttribute("doctors", directory.doctors(input));
    if (HtmxRequests.fragment(request)) {
      return "doctors/list :: results";
    }
    model.addAttribute("hospitals", directory.hospitals());
    model.addAttribute("specialties", directory.specialties());
    return "doctors/list";
  }

  @GetMapping("/doctors/{id}")
  public String doctor(@PathVariable @Positive long id, Model model) {
    model.addAttribute("doctor", directory.doctor(id));
    return "doctors/detail";
  }
}
