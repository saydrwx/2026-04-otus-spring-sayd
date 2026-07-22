package ru.otus.hw.exception.handler;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import ru.otus.hw.exception.AbstractEntityNotFoundException;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AbstractEntityNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleEntityNotFound(
    AbstractEntityNotFoundException exception,
    ServletWebRequest request,
    Locale locale
  ) {
    log.warn("Resource was not found for request {}: {}",
      request.getRequest().getRequestURI(), exception.getMessage());

    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
      exception.getMessage());
    problem.setTitle("Resource not found");
    problem.setInstance(URI.create(request.getRequest().getRequestURI()));
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
    var errors = new LinkedHashMap<String, String>();
    exception.getBindingResult().getFieldErrors()
      .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

    var problem = ProblemDetail.forStatusAndDetail(
      HttpStatus.BAD_REQUEST,
      "Ошибка валидации"
    );
    problem.setTitle("Validation failed");
    problem.setProperty("errors", errors);
    return ResponseEntity.badRequest().body(problem);
  }
}
