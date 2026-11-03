package ru.otus.hw.web.controller;

import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import ru.otus.hw.web.dto.*;
import ru.otus.hw.web.exception.ClinicalException;
import ru.otus.hw.web.security.ClinicalSession;
import ru.otus.hw.web.service.AppointmentService;
import ru.otus.hw.web.service.ClinicalService;

import static ru.otus.hw.web.exception.ClinicalException.Kind.*;

@RequiredArgsConstructor
@Controller
public class ReviewController {

  private final ClinicalService clinical;
  private final AppointmentService appointments;
  private final ClinicalSession session;

  @InitBinder("reviewForm")
  void bind(WebDataBinder binder) {
    FormBindings.only(binder, "appointmentId", "rating", "text");
  }

  @GetMapping({"/patient/appointments/{id}/review", "/doctor/appointments/{id}/review"})
  public String review(@PathVariable UUID id, Authentication user, HttpServletRequest request,
    HttpServletResponse response, Model model) {
    return session.call(user, request, response, token -> {
      var appointment = appointments.find(token, id);
      var review = clinical.review(token, id);
      model.addAttribute("appointment", appointment);
      model.addAttribute("review", review.orElse(null));
      model.addAttribute("doctorView", doctor(request));
      model.addAttribute("canReview",
        !doctor(request) && appointment.status() == AppointmentStatus.COMPLETED);
      return "clinical/review";
    });
  }

  @GetMapping({"/patient/appointments/{id}/review/new", "/patient/appointments/{id}/review/edit"})
  public String form(@PathVariable UUID id, Authentication user, HttpServletRequest request,
    HttpServletResponse response, Model model) {
    boolean editing = request.getRequestURI().endsWith("/edit");
    return session.call(user, request, response, token -> {
      completed(appointments.find(token, id));
      var review = clinical.review(token, id);
      if (!editing && review.isPresent()) {
        return "redirect:/patient/appointments/" + id + "/review";
      }
      if (editing && review.isEmpty()) {
        throw new ClinicalException(NOT_FOUND);
      }
      var form = new ReviewForm();
      form.setAppointmentId(id);
      form.setRating(review.map(ReviewDto::rating).orElse(null));
      form.setText(review.map(ReviewDto::text).orElse(null));
      model.addAttribute("reviewForm", form);
      model.addAttribute("editing", editing);
      return "clinical/review-form";
    });
  }

  @PostMapping({"/patient/reviews/create", "/patient/reviews/update"})
  public String save(@Valid @ModelAttribute("reviewForm") ReviewForm form, BindingResult errors,
    Authentication user,
    HttpServletRequest request, HttpServletResponse response, Model model) {
    boolean editing = request.getRequestURI().endsWith("/update");
    model.addAttribute("editing", editing);
    if (errors.getSuppressedFields().length > 0) {
      errors.reject("unsupported", "Форма содержит неподдерживаемые поля.");
    }
    if (errors.hasErrors()) {
      response.setStatus(400);
      return "clinical/review-form";
    }
    try {
      session.call(user, request, response,
        token -> editing ? clinical.updateReview(token, form.getAppointmentId(), form.input())
          : clinical.createReview(token, form.getAppointmentId(), form.input()));
      return "redirect:/patient/appointments/" + form.getAppointmentId() + "/review";
    } catch (ClinicalException exception) {
      if (exception.kind() != INVALID && exception.kind() != UNAVAILABLE) {
        return ClinicalResponses.actionError(exception, form.getAppointmentId(), false, response,
          model, "Не удалось сохранить отзыв. Проверьте текущий отзыв перед повторной попыткой.");
      }
      response.setStatus(exception.kind().status().value());
      model.addAttribute("message", exception.kind() == UNAVAILABLE
        ? "Сохранение отзыва не подтверждено. Проверьте текущий отзыв перед повторной отправкой."
        : "Выберите оценку от 1 до 5. Комментарий должен содержать не более 2 000 символов.");
      return "clinical/review-form";
    }
  }

  @GetMapping("/patient/appointments/{id}/review/delete")
  public String deleteForm(@PathVariable UUID id, Authentication user, HttpServletRequest request,
    HttpServletResponse response, Model model) {
    return session.call(user, request, response, token -> {
      var appointment = appointments.find(token, id);
      completed(appointment);
      var review = clinical.review(token, id).orElseThrow(() -> new ClinicalException(NOT_FOUND));
      model.addAttribute("appointment", appointment);
      model.addAttribute("review", review);
      return "clinical/review-delete";
    });
  }

  @PostMapping("/patient/appointments/{id}/review/delete")
  public String delete(@PathVariable UUID id, @RequestParam MultiValueMap<String, String> fields,
    Authentication user,
    HttpServletRequest request, HttpServletResponse response, Model model) {
    if (fields.keySet().stream().anyMatch(name -> !name.equals("_csrf"))) {
      throw new ClinicalException(INVALID);
    }
    try {
      session.call(user, request, response, token -> {
        clinical.deleteReview(token, id);
        return Boolean.TRUE;
      });
      return "redirect:/patient/appointments/" + id + "/review";
    } catch (ClinicalException exception) {
      return ClinicalResponses.actionError(exception, id, false, response, model,
        "Удаление отзыва не подтверждено. Проверьте текущий отзыв перед повторной попыткой.");
    }
  }

  private void completed(AppointmentDto value) {
    if (value.status() != AppointmentStatus.COMPLETED) {
      throw new ClinicalException(CONFLICT);
    }
  }

  private boolean doctor(HttpServletRequest request) {
    return request.getRequestURI().startsWith("/doctor/");
  }
}
