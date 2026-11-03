package ru.otus.hw.web.view;

import org.springframework.stereotype.Component;

@Component("uiLabels")
public class UiLabels {

  public String catalog(String kind) {
    return switch (kind) {
      case "hospitals" -> "Больницы";
      case "specialties" -> "Специальности";
      case "doctors" -> "Врачи";
      default -> "Справочник";
    };
  }

  public String experience(int years) {
    int lastTwo = years % 100;
    int last = years % 10;
    if (lastTwo >= 11 && lastTwo <= 14) {
      return years + " лет";
    }
    if (last == 1) {
      return years + " год";
    }
    if (last >= 2 && last <= 4) {
      return years + " года";
    }
    return years + " лет";
  }
}
