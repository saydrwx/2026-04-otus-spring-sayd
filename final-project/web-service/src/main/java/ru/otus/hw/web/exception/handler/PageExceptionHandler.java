package ru.otus.hw.web.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import ru.otus.hw.web.controller.HtmxRequests;
import ru.otus.hw.web.exception.DirectoryUnavailableException;
import ru.otus.hw.web.exception.DoctorNotFoundException;
import ru.otus.hw.web.exception.ProfileUnavailableException;
import ru.otus.hw.web.exception.ProfileForbiddenException;
import ru.otus.hw.web.exception.SessionExpiredException;
import ru.otus.hw.web.exception.AppointmentsException;
import ru.otus.hw.web.exception.ClinicalException;
import ru.otus.hw.web.exception.DocumentsException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;

@Slf4j
@ControllerAdvice
public class PageExceptionHandler {

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ModelAndView unsupportedContentType(HttpServletRequest request,
    HttpServletResponse response) {
    if (request.getRequestURI().endsWith("/documents/upload")) {
      var view = new ModelAndView("documents/upload");
      view.setStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
      view.addObject("message",
        "Используйте форму загрузки, чтобы отправить один файл PDF, JPEG или PNG.");
      return view;
    }
    return error(request, response, HttpStatus.UNSUPPORTED_MEDIA_TYPE,
      "Используйте формат данных, предусмотренный этой формой.");
  }

  @ExceptionHandler(ru.otus.hw.web.exception.AdminException.class)
  public ModelAndView admin(ru.otus.hw.web.exception.AdminException exception) {
    var view = new ModelAndView("admin/error");
    view.setStatus(exception.kind().status());
    view.addObject("message", exception.kind().message());
    return view;
  }

  @ExceptionHandler(DocumentsException.class)
  public ModelAndView documents(DocumentsException exception, HttpServletRequest request) {
    var view = new ModelAndView("documents/error");
    view.setStatus(exception.kind().status());
    view.addObject("message", exception.kind().message());
    view.addObject("doctorView", request.getRequestURI().startsWith("/doctor/"));
    return view;
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ModelAndView uploadTooLarge() {
    var view = new ModelAndView("documents/upload");
    view.setStatus(HttpStatus.PAYLOAD_TOO_LARGE);
    view.addObject("message", DocumentsException.Kind.TOO_LARGE.message());
    return view;
  }

  @ExceptionHandler({MultipartException.class, MissingServletRequestPartException.class})
  public ModelAndView invalidUpload() {
    var view = new ModelAndView("documents/upload");
    view.setStatus(HttpStatus.BAD_REQUEST);
    view.addObject("message", DocumentsException.Kind.INVALID.message());
    return view;
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ModelAndView methodNotAllowed(HttpRequestMethodNotSupportedException exception,
    HttpServletRequest request, HttpServletResponse response) {
    if (exception.getSupportedMethods() != null) {
      response.setHeader("Allow", String.join(", ", exception.getSupportedMethods()));
    }
    return error(request, response, HttpStatus.METHOD_NOT_ALLOWED,
      "Этот способ отправки запроса не поддерживается.");
  }

  @ExceptionHandler(ClinicalException.class)
  public ModelAndView clinical(ClinicalException exception, HttpServletRequest request) {
    var view = new ModelAndView("clinical/error");
    view.setStatus(exception.kind().status());
    view.addObject("message", exception.kind().message());
    view.addObject("doctorView", request.getRequestURI().startsWith("/doctor/"));
    return view;
  }

  @ExceptionHandler(AppointmentsException.class)
  public ModelAndView appointments(AppointmentsException exception, HttpServletRequest request) {
    var view = new ModelAndView("appointments/error");
    view.setStatus(exception.kind().status());
    view.addObject("message", exception.kind().message());
    view.addObject("doctorView", request.getRequestURI().startsWith("/doctor/"));
    return view;
  }

  @ExceptionHandler(SessionExpiredException.class)
  public ModelAndView expired() {
    var view = new ModelAndView("session-expired");
    view.setStatus(HttpStatus.UNAUTHORIZED);
    return view;
  }

  @ExceptionHandler(ProfileUnavailableException.class)
  public ModelAndView profileUnavailable(HttpServletRequest r, HttpServletResponse response) {
    return error(r, response, HttpStatus.SERVICE_UNAVAILABLE,
      "Профиль временно недоступен. Повторите попытку позже.");
  }

  @ExceptionHandler(ProfileForbiddenException.class)
  public ModelAndView profileForbidden(HttpServletRequest r, HttpServletResponse response) {
    return error(r, response, HttpStatus.FORBIDDEN, "У вас нет доступа к этому профилю.");
  }

  @ExceptionHandler(DirectoryUnavailableException.class)
  public ModelAndView unavailable(HttpServletRequest r, HttpServletResponse response) {
    return error(r, response, HttpStatus.SERVICE_UNAVAILABLE,
      "Справочник временно недоступен. Повторите попытку позже.");
  }

  @ExceptionHandler({DoctorNotFoundException.class, NoResourceFoundException.class})
  public ModelAndView missing(HttpServletRequest r, HttpServletResponse response) {
    return error(r, response, HttpStatus.NOT_FOUND, "Страница или врач не найдены.");
  }

  @ExceptionHandler({BindException.class, HandlerMethodValidationException.class,
    MethodArgumentTypeMismatchException.class, ServletRequestBindingException.class})
  public ModelAndView invalid(HttpServletRequest r, HttpServletResponse response) {
    return error(r, response, HttpStatus.BAD_REQUEST,
      "Проверьте данные запроса и выберите корректные дату, время или фильтр.");
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ModelAndView forbidden(HttpServletRequest r, HttpServletResponse response) {
    return error(r, response, HttpStatus.FORBIDDEN, "У вас нет доступа к этой странице.");
  }

  @ExceptionHandler(Exception.class)
  public ModelAndView unexpected(Exception exception, HttpServletRequest r,
    HttpServletResponse response) {
    log.error("Unexpected web request failure", exception);
    return error(r, response, HttpStatus.INTERNAL_SERVER_ERROR,
      "Произошла ошибка. Повторите попытку позже.");
  }

  private ModelAndView error(HttpServletRequest request, HttpServletResponse response,
    HttpStatus status, String message) {
    response.setHeader("HX-Push-Url", "false");
    response.setHeader("Vary", "HX-Request, HX-History-Restore-Request");
    var view = new ModelAndView(
      HtmxRequests.fragment(request) ? "doctors/list :: results" : "error-page");
    view.setStatus(status);
    view.addObject("message", message);
    view.addObject("status", status.value());
    return view;
  }
}
