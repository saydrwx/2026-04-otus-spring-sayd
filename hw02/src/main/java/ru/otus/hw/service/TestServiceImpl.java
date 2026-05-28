package ru.otus.hw.service;

import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.otus.hw.dao.QuestionDao;
import ru.otus.hw.domain.Answer;
import ru.otus.hw.domain.Question;
import ru.otus.hw.domain.Student;
import ru.otus.hw.domain.TestResult;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Service
public class TestServiceImpl implements TestService {

  private final IOService ioService;

  private final QuestionDao questionDao;

  private final QuestionFormattingService formattingService;

  @Override
  public TestResult executeTestFor(Student student) {
    printStartMessage();

    var questions = questionDao.findAll();
    var testResult = new TestResult(student);

    for (var question : questions) {
      printQuestion(question);
      int inputAnswer = getInputAnswer(question);
      boolean isAnswerValid = isAnswerValid(question, inputAnswer);
      testResult.applyAnswer(question, isAnswerValid);
    }
    return testResult;
  }

  private void printStartMessage() {
    ioService.printLine("");
    ioService.printFormattedLine("Please answer the questions below%n");
  }

  private void printQuestion(Question question) {
    String formattedQuestion = formattingService.formatQuestion(question);
    ioService.printFormattedLine(formattedQuestion);
  }

  private int getInputAnswer(Question question) {
    int answersCount = question.answers().size();
    return ioService.readIntForRangeWithPrompt(1, answersCount, "Enter the answer number",
      "Answer number out of range");
  }

  private boolean isAnswerValid(Question question, int inputAnswer) {
    List<Answer> answers = question.answers();
    return IntStream.rangeClosed(0, answers.size() - 1)
      .anyMatch(i -> (i + 1 == inputAnswer) && answers.get(i).isCorrect());
  }
}
