package ru.otus.hw.clinic.exception.handler;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
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
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.otus.hw.clinic.exception.AppointmentConflictException;
import ru.otus.hw.clinic.exception.DirectoryUnavailableException;
import ru.otus.hw.clinic.exception.DocumentStorageException;
import ru.otus.hw.clinic.exception.InvalidBookingException;
import ru.otus.hw.clinic.exception.InvalidDocumentException;
import ru.otus.hw.clinic.exception.ResourceNotFoundException;
import ru.otus.hw.clinic.exception.ProfileAlreadyExistsException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
@SuppressWarnings("unused")
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleEntityNotFound(
    ResourceNotFoundException exception
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

  @ExceptionHandler(ProfileAlreadyExistsException.class)
  public ResponseEntity<ProblemDetail> handleExistingProfile(
    ProfileAlreadyExistsException exception) {
    String requestUri = ServletUriComponentsBuilder.fromCurrentRequest().toUriString();
    String message = exception.getMessage();
    HttpStatus status = HttpStatus.CONFLICT;

    log.warn("Profile already exists for request {}: {}", requestUri, message);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, message);
    problemDetail.setTitle("Profile Already Exists");

    return ResponseEntity.status(status).body(problemDetail);
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<ProblemDetail> handleConcurrentUpdate(
    ObjectOptimisticLockingFailureException exception) {
    String requestUri = ServletUriComponentsBuilder.fromCurrentRequest().toUriString();
    String message = "Profile was changed concurrently; reload it before retrying";
    HttpStatus status = HttpStatus.CONFLICT;

    log.warn("Profile was changed concurrently for request {}", requestUri);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, message);
    problemDetail.setTitle("Profile Was Changed Concurrently");

    return ResponseEntity.status(status).body(problemDetail);
  }

  @ExceptionHandler(PessimisticLockingFailureException.class)
  public ResponseEntity<ProblemDetail> handleLockConflict(
    PessimisticLockingFailureException exception) {
    String requestUri = ServletUriComponentsBuilder.fromCurrentRequest().toUriString();
    String message = "Appointment changed concurrently; reload before retrying";
    HttpStatus status = HttpStatus.CONFLICT;

    log.warn("Appointment was changed concurrently for request {}", requestUri);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, message);
    problemDetail.setTitle("Appointment Was Changed Concurrently");

    return ResponseEntity.status(status).body(problemDetail);
  }

  @ExceptionHandler(DirectoryUnavailableException.class)
  public ResponseEntity<ProblemDetail> handleDirectoryUnavailable(
    DirectoryUnavailableException exception) {
    String requestUri = ServletUriComponentsBuilder.fromCurrentRequest().toUriString();
    String message = "Directory is temporarily unavailable; please retry later";
    HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;

    log.warn("Directory lookup could not be completed for request {}", requestUri);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, message);
    problemDetail.setTitle("Directory Is Temporarily Unavailable");

    return ResponseEntity.status(status).body(problemDetail);
  }

  @ExceptionHandler(InvalidBookingException.class)
  public ResponseEntity<ProblemDetail> handleInvalidBooking(InvalidBookingException exception) {
    String requestUri = ServletUriComponentsBuilder.fromCurrentRequest().toUriString();
    String message = exception.getMessage();
    HttpStatus status = HttpStatus.BAD_REQUEST;

    log.warn("Invalid booking for request {}: {}", requestUri, message);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, message);
    problemDetail.setTitle("Invalid Booking");

    return ResponseEntity.status(status).body(problemDetail);
  }

  @ExceptionHandler(AppointmentConflictException.class)
  public ResponseEntity<ProblemDetail> handleAppointmentConflict(
    AppointmentConflictException exception) {
    String requestUri = ServletUriComponentsBuilder.fromCurrentRequest().toUriString();
    String message = exception.getMessage();
    HttpStatus status = HttpStatus.CONFLICT;

    log.warn("Appointment conflict for request {}: {}", requestUri, message);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, message);
    problemDetail.setTitle("Appointment Conflict");

    return ResponseEntity.status(status).body(problemDetail);
  }

  @ExceptionHandler(InvalidDocumentException.class)
  public ResponseEntity<ProblemDetail> handleInvalidDocument(InvalidDocumentException exception) {
    String requestUri = ServletUriComponentsBuilder.fromCurrentRequest().toUriString();
    String message = exception.getMessage();
    HttpStatus status = exception.getStatus();

    log.warn("Invalid document for request {}: {}", requestUri, message);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, message);
    problemDetail.setTitle("Invalid document");

    return ResponseEntity.status(status).body(problemDetail);
  }

  @ExceptionHandler(DocumentStorageException.class)
  public ResponseEntity<ProblemDetail> handleDocumentStorage(DocumentStorageException exception) {
    String requestUri = ServletUriComponentsBuilder.fromCurrentRequest().toUriString();
    String message = "Document storage is temporarily unavailable";
    HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;

    log.warn("Document storage is temporarily unavailable");

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, message);
    problemDetail.setTitle("Document Storage Is Unavailable");

    return ResponseEntity.status(status).body(problemDetail);
  }

  @Override
  protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
    MaxUploadSizeExceededException exception,
    HttpHeaders headers,
    HttpStatusCode status,
    WebRequest request
  ) {
    String requestUri = ServletUriComponentsBuilder.fromCurrentRequest().toUriString();
    String message = "File limit is 5 MiB (5242880 bytes)";

    log.warn("File limit exceeded for request {}", requestUri);

    return handleExceptionInternal(
      exception,
      ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, message),
      headers,
      HttpStatus.PAYLOAD_TOO_LARGE,
      request
    );
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
