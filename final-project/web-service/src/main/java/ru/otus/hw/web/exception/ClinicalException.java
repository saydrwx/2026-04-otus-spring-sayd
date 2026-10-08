package ru.otus.hw.web.exception;

import org.springframework.http.HttpStatus;

public class ClinicalException extends RuntimeException {

  public enum Kind {
    INVALID(HttpStatus.BAD_REQUEST, "Проверьте поля заключения или отзыва."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Медицинская запись или действие недоступны."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "У вас нет прав на это медицинское действие."),
    CONFLICT(HttpStatus.CONFLICT,
      "Приём или отзыв изменились либо действие для них недоступно. Проверьте текущее состояние."),
    UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,
      "Сервис клиники временно недоступен. Повторите попытку позже.");
    private final HttpStatus status;
    private final String message;

    Kind(HttpStatus status, String message) {
      this.status = status;
      this.message = message;
    }

    public HttpStatus status() {
      return status;
    }

    public String message() {
      return message;
    }
  }

  private final Kind kind;

  public ClinicalException(Kind kind) {
    super("Clinical_" + kind.name());
    this.kind = kind;
  }

  public Kind kind() {
    return kind;
  }
}
