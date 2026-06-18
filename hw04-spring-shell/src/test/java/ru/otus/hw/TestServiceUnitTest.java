package ru.otus.hw;

import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.otus.hw.dao.CsvQuestionDao;
import ru.otus.hw.dao.QuestionDao;
import ru.otus.hw.domain.Answer;
import ru.otus.hw.domain.Question;
import ru.otus.hw.domain.Student;
import ru.otus.hw.service.LocalizedIOService;
import ru.otus.hw.service.LocalizedIOServiceImpl;
import ru.otus.hw.service.QuestionFormattingService;
import ru.otus.hw.service.TestServiceImpl;

@SpringBootTest(classes = {LocalizedIOServiceImpl.class, CsvQuestionDao.class,
  QuestionFormattingService.class, TestServiceImpl.class})
class TestServiceUnitTest {

  @MockitoBean
  private LocalizedIOService ioService;

  @MockitoBean
  private QuestionDao questionDao;

  @MockitoBean
  private QuestionFormattingService formattingService;

  private final Student student = new Student("Ivan", "Ivanov");

  @Autowired
  private TestServiceImpl testService;

  @Test
  @DisplayName("Should print start message when there are no questions")
  void shouldPrintStartMessageForEmptyQuestionList() {
    when(questionDao.findAll()).thenReturn(List.of());

    testService.executeTestFor(student);

    verify(ioService, times(2)).printLine("");
    verify(ioService).printLineLocalized("TestService.answer.the.questions");
    verify(questionDao).findAll();
    verifyNoMoreInteractions(ioService);
  }

  @Test
  @DisplayName("Should print a single question without answers")
  void shouldPrintSingleQuestionWithoutAnswers() {
    String questionText = "What is Java?";
    Question question = new Question(questionText, List.of());

    when(questionDao.findAll()).thenReturn(List.of(question));
    when(formattingService.formatQuestion(any(Question.class))).thenReturn(questionText);

    testService.executeTestFor(student);

    verify(formattingService).formatQuestion(question);
    verify(ioService, times(2)).printLine("");
    verify(ioService).printLineLocalized("TestService.answer.the.questions");
    verify(ioService).printFormattedLine(questionText);
    verify(questionDao).findAll();
  }

  @Test
  @DisplayName("Should print a single question with multiple answers")
  void shouldPrintSingleQuestionWithAnswers() {
    String questionText = "What is the capital of France?";
    List<Answer> answers = List.of(
      new Answer("Moscow", false),
      new Answer("Saratov", false),
      new Answer("Paris", true)
    );
    Question question = new Question(questionText, answers);
    String formattedQuestion = """
      \n
      What is the capital of France?
      (1) Moscow%n\
      (2) Saratov%n\
      (3) Paris
      """;

    when(questionDao.findAll()).thenReturn(List.of(question));
    when(formattingService.formatQuestion(any(Question.class))).thenReturn(formattedQuestion);

    testService.executeTestFor(student);

    verify(formattingService).formatQuestion(question);
    verify(ioService, times(2)).printLine("");
    verify(ioService).printLineLocalized("TestService.answer.the.questions");
    verify(ioService).printFormattedLine(formattedQuestion);
  }
}
