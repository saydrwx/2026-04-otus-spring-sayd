package ru.otus.hw.web.exception;

import org.springframework.http.HttpStatus;

public class AdminException extends RuntimeException {

  public enum Kind {
    INVALID(HttpStatus.BAD_REQUEST, "Проверьте поля формы и выбранный период."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "У вас нет прав на это действие."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Запрошенная запись не найдена."),
    CONFLICT(HttpStatus.CONFLICT,
      "Запись используется или была изменена. Обновите её перед дальнейшими изменениями."),
    UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,
      "Результат операции не подтверждён. Проверьте текущее состояние перед повторной отправкой.");
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

  public AdminException(Kind kind) {
    super(kind.name());
    this.kind = kind;
  }

  public Kind kind() {
    return kind;
  }
}
