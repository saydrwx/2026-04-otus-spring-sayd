package ru.otus.hw.web.controller;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import ru.otus.hw.web.dto.RegistrationForm;
import ru.otus.hw.web.dto.RegistrationRequest;
import ru.otus.hw.web.exception.RegistrationRejectedException;
import ru.otus.hw.web.exception.RegistrationUnavailableException;
import ru.otus.hw.web.service.RegistrationService;

@RequiredArgsConstructor
@Controller
public class RegistrationController {

  private final RegistrationService registrations;

  @InitBinder("registration")
  void bind(WebDataBinder binder) {
    FormBindings.only(binder, "email", "password", "confirmPassword");
  }

  @GetMapping("/register")
  public String form(Model model) {
    model.addAttribute("email", "");
    return "registration/form";
  }

  @GetMapping("/register/complete")
  public String complete() {
    return "registration/complete";
  }

  @PostMapping("/register")
  public String register(@Valid @ModelAttribute("registration") RegistrationForm form,
    BindingResult errors,
    Model model, HttpServletResponse response) {
    var messages = new ArrayList<String>();
    if (errors.hasFieldErrors("email")) {
      messages.add(
        "Введите корректный адрес электронной почты латинскими символами (не более 255 символов).");
    }
    if (errors.hasFieldErrors("password")) {
      messages.add("Используйте пароль длиной от 12 до 64 символов, но не более 72 байт UTF-8.");
    }
    if (errors.hasFieldErrors("confirmPassword") || !Objects.equals(form.getPassword(),
      form.getConfirmPassword())) {
      messages.add("Пароли должны совпадать.");
    }
    if (errors.getSuppressedFields().length > 0) {
      messages.add("Форма содержит неподдерживаемые поля.");
    }
    String email = form.getEmail() == null ? "" : form.getEmail().strip().toLowerCase(Locale.ROOT);
    // Remove both the bound object and rejected field values before rendering any page.
    model.asMap().remove("registration");
    model.asMap().remove(BindingResult.MODEL_KEY_PREFIX + "registration");
    model.addAttribute("email", email);
    try {
      if (!messages.isEmpty()) {
        response.setStatus(400);
        model.addAttribute("messages", messages);
        return "registration/form";
      }
      registrations.register(new RegistrationRequest(email, form.getPassword()));
      return "redirect:/register/complete";
    } catch (RegistrationRejectedException exception) {
      response.setStatus(400);
      model.addAttribute("messages", java.util.List.of(
        "Не удалось завершить регистрацию. Проверьте данные или войдите, если учётная запись уже существует."));
      return "registration/form";
    } catch (RegistrationUnavailableException exception) {
      response.setStatus(503);
      model.addAttribute("messages", java.util.List.of(
        "Результат регистрации не подтверждён. Сначала попробуйте войти: учётная запись могла быть создана. Если это не удалось, повторите попытку позже."));
      return "registration/form";
    } finally {
      form.setPassword(null);
      form.setConfirmPassword(null);
    }
  }
}
