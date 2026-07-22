package ru.otus.hw.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BookForm {

  @NotBlank(message = "Название книги не должно быть пустым")
  @Size(min = 2, max = 255, message = "Длина названия книги должна быть от 2 до 255 символов")
  private String title = "";

  @NotNull(message = "Выберите автора")
  private Long authorId;

  @NotEmpty(message = "Выберите хотя бы один жанр")
  private Set<Long> genreIds = new LinkedHashSet<>();

  public BookForm(String title, Long authorId, Set<Long> genreIds) {
    this.title = title;
    this.authorId = authorId;
    this.genreIds = new LinkedHashSet<>(genreIds);
  }
}

