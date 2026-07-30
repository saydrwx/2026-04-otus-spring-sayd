package ru.otus.hw.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CommentForm {

  @NotBlank(message = "{comment.text.not-blank}")
  @Size(min = 2, max = 1000, message = "{comment.text.size}")
  private String text = "";
}
