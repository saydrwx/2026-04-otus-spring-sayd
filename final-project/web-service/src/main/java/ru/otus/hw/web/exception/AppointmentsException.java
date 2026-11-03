package ru.otus.hw.web.exception;

import org.springframework.http.HttpStatus;

public class AppointmentsException extends RuntimeException {

  public enum Kind {
    INVALID(HttpStatus.BAD_REQUEST,
      "Проверьте данные записи, включая время и смещение часового пояса."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Запись на приём, врач или расписание не найдены."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "У вас нет прав на это действие с записью."),
    CONFLICT(HttpStatus.CONFLICT,
      "Действие не соответствует текущему состоянию записи. Обновите страницу перед повторной попыткой."),
    UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,
      "Сервис записи временно недоступен. Повторите попытку позже.");
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

  public AppointmentsException(Kind kind) {
    super("Appointments_" + kind.name());
    this.kind = kind;
  }

  public Kind kind() {
    return kind;
  }
}
