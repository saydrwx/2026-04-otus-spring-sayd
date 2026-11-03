package ru.otus.hw.web.controller;

import java.util.Arrays;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.mvc.method.annotation.ExtendedServletRequestDataBinder;

public final class FormBindings {

  private FormBindings() {
  }

  public static void only(WebDataBinder binder, String... fields) {
    var allowed = Arrays.copyOf(fields, fields.length + 1);
    allowed[fields.length] = "_csrf";
    binder.setAllowedFields(allowed);
    if (binder instanceof ExtendedServletRequestDataBinder servletBinder) {
      servletBinder.setHeaderPredicate(header -> false);
    }
  }
}
