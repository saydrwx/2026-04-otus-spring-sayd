package ru.otus.hw.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import ru.otus.hw.domain.Answer;
import ru.otus.hw.domain.Question;

@Service
public final class QuestionFormattingServiceImpl implements QuestionFormattingService {

  @Override
  public String formatQuestion(final Question question) {
    String text = question.text();
    List<Answer> answers = question.answers();
    String formattedAnswers = formatAnswers(answers);
    return String.format("%n%s%n%s%n", text, formattedAnswers);
  }

  private String formatAnswers(List<Answer> answers) {
    return IntStream.rangeClosed(0, answers.size() - 1)
      .mapToObj(index -> formatAnswerWithIndex(answers.get(index), index + 1))
      .collect(Collectors.joining("%n"));
  }

  private String formatAnswerWithIndex(Answer answer, int index) {
    return String.format("(%s) %s", index, answer.text());
  }
}
