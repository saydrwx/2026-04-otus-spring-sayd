package ru.otus.hw.web.exception;

import org.springframework.http.HttpStatus;

public class DocumentsException extends RuntimeException {

  public enum Kind {
    INVALID(HttpStatus.BAD_REQUEST,
      "Проверьте файл и поля формы. Загрузите один непустой файл PDF, JPEG или PNG с допустимым именем."),
    TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
      "Выберите файл PDF, JPEG или PNG с соответствующими расширением и типом."),
    TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "Максимальный размер файла — 5 МиБ (5 242 880 байт)."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Документ или список документов пациента недоступны."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "У вас нет прав на это действие с документом."),
    CONFLICT(HttpStatus.CONFLICT,
      "Действие не соответствует текущему состоянию документа. Проверьте список документов перед повторной попыткой."),
    UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,
      "Документы временно недоступны. Повторите попытку позже.");
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

  public DocumentsException(Kind kind) {
    super("Documents_" + kind.name());
    this.kind = kind;
  }

  public Kind kind() {
    return kind;
  }
}
