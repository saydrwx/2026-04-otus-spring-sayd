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

  @NotBlank(message = "{book.title.not-blank}")
  @Size(min = 2, max = 255, message = "{book.title.size}")
  private String title = "";

  @NotNull(message = "{book.author.required}")
  private Long authorId;

  @NotEmpty(message = "{book.genres.required}")
  private Set<Long> genreIds = new LinkedHashSet<>();

  public BookForm(String title, Long authorId, Set<Long> genreIds) {
    this.title = title;
    this.authorId = authorId;
    this.genreIds = new LinkedHashSet<>(genreIds);
  }
}

