package ru.otus.hw.authorization.util;

import java.util.Locale;

public final class EmailAddresses {

  private EmailAddresses() {
  }

  public static String normalize(String email) {
    return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
  }
}
