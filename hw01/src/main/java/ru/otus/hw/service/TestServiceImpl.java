package ru.otus.hw.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import ru.otus.hw.dao.QuestionDao;
import ru.otus.hw.domain.Answer;
import ru.otus.hw.domain.Question;

@RequiredArgsConstructor
public class TestServiceImpl implements TestService {

  private final IOService ioService;

  private final QuestionDao questionDao;

  @Override
  public void executeTest() {
    printStartMessage();
    printQuestions();
  }

  private void printStartMessage() {
    ioService.printLine("");
    ioService.printFormattedLine("Please answer the questions below%n");
  }

  private void printQuestions() {
    List<Question> questions = questionDao.findAll();
    questions.forEach(question -> {
      String formattedQuestion = getFormattedQuestion(question);
      ioService.printFormattedLine(formattedQuestion);
    });
  }

  private String getFormattedQuestion(Question question) {
    String text = question.text();
    List<Answer> answers = question.answers();
    String formattedAnswers = getFormattedAnswers(answers);
    return String.format("%s%n%s%n", text, formattedAnswers);
  }

  private String getFormattedAnswers(List<Answer> answers) {
    return IntStream.rangeClosed(0, answers.size() - 1)
      .mapToObj(index -> formatAnswer(answers.get(index), index + 1))
      .collect(Collectors.joining("%n"));
  }

  private String formatAnswer(Answer answer, int index) {
    return String.format("(%s) %s", index, answer.text());
  }
}
