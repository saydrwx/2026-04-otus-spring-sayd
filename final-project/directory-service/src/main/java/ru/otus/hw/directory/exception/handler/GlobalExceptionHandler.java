package ru.otus.hw.directory.exception.handler;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.otus.hw.directory.exception.AbstractEntityNotFoundException;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
@SuppressWarnings("unused")
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(AbstractEntityNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleEntityNotFound(
    AbstractEntityNotFoundException exception
  ) {
    String requestUri = ServletUriComponentsBuilder.fromCurrentRequest().toUriString();
    String message = exception.getMessage();
    HttpStatus status = HttpStatus.NOT_FOUND;

    log.warn("Resource was not found for request {}: {}", requestUri, message);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, message);
    problemDetail.setTitle("Resource Not Found");

    return ResponseEntity.status(status).body(problemDetail);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(
    DataIntegrityViolationException exception
  ) {
    String requestUri = ServletUriComponentsBuilder.fromCurrentRequest().toUriString();
    String message = "Data integrity violation";
    HttpStatus status = HttpStatus.CONFLICT;

    log.warn("Data integrity violation for request {}: {}", requestUri, message);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, message);
    problemDetail.setTitle("Data Integrity Violation");

    return ResponseEntity.status(status).body(problemDetail);
  }

  @Override
  public ResponseEntity<Object> handleMethodArgumentNotValid(
    MethodArgumentNotValidException exception,
    HttpHeaders headers,
    HttpStatusCode status,
    WebRequest request
  ) {
    String requestUri = ServletUriComponentsBuilder.fromCurrentRequest().toUriString();
    ProblemDetail problemDetail = exception.getBody();

    log.warn("Validation failed for request {}: {}", requestUri, problemDetail.getDetail());

    Map<String, String> errors = new HashMap<>();
    exception.getBindingResult().getAllErrors().forEach((error) -> {
      if (error instanceof FieldError fieldError) {
        errors.put(fieldError.getField(), fieldError.getDefaultMessage());
      } else {
        errors.put(error.getObjectName(), error.getDefaultMessage());
      }
    });
    problemDetail.setProperty("violations", errors);

    return handleExceptionInternal(exception, problemDetail, headers, status, request);
  }
}
