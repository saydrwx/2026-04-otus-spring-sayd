package ru.otus.hw.web.controller;

import java.util.function.Supplier;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import ru.otus.hw.web.dto.ProfileForm;
import ru.otus.hw.web.exception.*;
import ru.otus.hw.web.security.SessionAccessTokens;
import ru.otus.hw.web.service.PatientProfileService;

@RequiredArgsConstructor
@Controller
public class PatientProfileController {

  private final PatientProfileService profiles;
  private final SessionAccessTokens tokens;

  @InitBinder("profileForm")
  void bind(WebDataBinder binder) {
    FormBindings.only(binder, "firstName", "lastName", "middleName", "birthDate");
  }

  @GetMapping("/patient/profile")
  public String profile(Authentication user, HttpServletRequest request,
    HttpServletResponse response, Model model) {
    String token = tokens.current(user, request, response);
    var profile = call(user, request, response, () -> profiles.find(token));
    if (profile.isEmpty()) {
      return "redirect:/patient/onboarding";
    }
    model.addAttribute("profile", profile.get());
    return "patient/profile";
  }

  @GetMapping({"/patient/onboarding", "/patient/profile/edit"})
  public String form(Authentication user, HttpServletRequest request, HttpServletResponse response,
    Model model) {
    boolean create = request.getRequestURI().equals("/patient/onboarding");
    String token = tokens.current(user, request, response);
    var profile = call(user, request, response, () -> profiles.find(token));
    if (create && profile.isPresent()) {
      return "redirect:/patient/profile";
    }
    if (!create && profile.isEmpty()) {
      return "redirect:/patient/onboarding";
    }
    model.addAttribute("profileForm", profile.map(ProfileForm::from).orElseGet(ProfileForm::new));
    model.addAttribute("creating", create);
    return "patient/form";
  }

  @PostMapping({"/patient/onboarding", "/patient/profile"})
  public String save(@Valid @ModelAttribute("profileForm") ProfileForm form, BindingResult errors,
    Authentication user,
    HttpServletRequest request, HttpServletResponse response, Model model) {
    boolean create = request.getRequestURI().equals("/patient/onboarding");
    model.addAttribute("creating", create);
    if (errors.getSuppressedFields().length > 0) {
      errors.reject("unsupported", "Форма содержит неподдерживаемые поля.");
    }
    if (errors.hasErrors()) {
      response.setStatus(400);
      return "patient/form";
    }
    String token = tokens.current(user, request, response);
    try {
      call(user, request, response,
        () -> create ? profiles.create(token, form.input()) : profiles.update(token, form.input()));
      return "redirect:/patient/profile";
    } catch (ProfileConflictException exception) {
      if (create) {
        return "redirect:/patient/profile";
      }
      response.setStatus(409);
      model.addAttribute("message",
        "Не удалось сохранить профиль. Обновите его перед дальнейшими изменениями.");
    } catch (ProfileMissingException exception) {
      return "redirect:/patient/onboarding";
    } catch (ProfileRejectedException exception) {
      response.setStatus(400);
      model.addAttribute("message",
        "Проверьте данные. Дата рождения должна быть раньше сегодняшнего дня.");
    } catch (ProfileUnavailableException exception) {
      response.setStatus(503);
      model.addAttribute("message",
        "Сохранение не подтверждено. Проверьте профиль перед повторной отправкой.");
    }
    return "patient/form";
  }

  private <T> T call(Authentication user, HttpServletRequest request, HttpServletResponse response,
    Supplier<T> action) {
    try {
      return action.get();
    } catch (SessionExpiredException exception) {
      tokens.expire(user, request, response);
      throw exception;
    }
  }
}
