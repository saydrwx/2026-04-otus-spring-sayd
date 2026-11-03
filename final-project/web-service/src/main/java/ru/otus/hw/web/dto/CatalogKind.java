package ru.otus.hw.web.dto;

import ru.otus.hw.web.exception.AdminException;

public enum CatalogKind {
  HOSPITALS("hospitals"),
  SPECIALTIES("specialties"),
  DOCTORS("doctors");

  private final String path;

  CatalogKind(String path) {
    this.path = path;
  }

  public String path() {
    return path;
  }

  public static CatalogKind parse(String value) {
    for (var kind : values()) {
      if (kind.path.equals(value)) {
        return kind;
      }
    }
    throw new AdminException(AdminException.Kind.NOT_FOUND);
  }
}
