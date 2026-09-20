package ru.otus.hw.exception.handler;

import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import ru.otus.hw.exception.AbstractEntityNotFoundException;

@Slf4j
@RequiredArgsConstructor
@ControllerAdvice
public class GlobalExceptionHandler {

  private final MessageSource messageSource;

  @ExceptionHandler(AbstractEntityNotFoundException.class)
  public ModelAndView handleEntityNotFound(
    AbstractEntityNotFoundException exception,
    HttpServletRequest request,
    Locale locale
  ) {
    log.warn("Resource was not found for request {}: {}", request.getRequestURI(), exception.getMessage());

    String message = messageSource.getMessage(
      exception.getMessageCode(),
      exception.getMessageArguments(),
      locale
    );

    return errorModelAndView(HttpStatus.NOT_FOUND, message);
  }

  @ExceptionHandler(CallNotPermittedException.class)
  public ModelAndView handeCallNotPermittedException(CallNotPermittedException ex, Locale locale) {
    String message = messageSource.getMessage(
      "error.too-many-request",
      null,
      locale
    );

    return errorModelAndView(TOO_MANY_REQUESTS, message);
  }

  private ModelAndView errorModelAndView(HttpStatus status, String message) {
    var modelAndView = new ModelAndView("error");
    modelAndView.setStatus(status);
    modelAndView.addObject("status", status.value());
    modelAndView.addObject("message", message);
    return modelAndView;
  }
}
