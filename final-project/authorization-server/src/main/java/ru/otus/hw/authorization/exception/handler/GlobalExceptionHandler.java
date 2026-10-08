package ru.otus.hw.authorization.exception.handler;

import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.otus.hw.authorization.exception.EmailAlreadyRegisteredException;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(EmailAlreadyRegisteredException.class)
  public ProblemDetail duplicate(EmailAlreadyRegisteredException exception) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail integrity(DataIntegrityViolationException exception) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
      "Registration conflicts with existing data");
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ProblemDetail invalidServiceInput(ConstraintViolationException exception) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid registration input");
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
    MethodArgumentNotValidException exception,
    HttpHeaders headers,
    HttpStatusCode status,
    WebRequest request
  ) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
      "Invalid registration input");
    Map<String, String> violations = new LinkedHashMap<>();
    exception.getBindingResult().getFieldErrors().forEach(error ->
      violations.putIfAbsent(error.getField(), error.getDefaultMessage()));
    problem.setProperty("violations", violations);
    return handleExceptionInternal(exception, problem, headers, status, request);
  }
}
