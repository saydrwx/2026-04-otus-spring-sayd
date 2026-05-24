package ru.otus.hw;

import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.otus.hw.dao.QuestionDao;
import ru.otus.hw.domain.Answer;
import ru.otus.hw.domain.Question;
import ru.otus.hw.domain.Student;
import ru.otus.hw.service.IOService;
import ru.otus.hw.service.QuestionFormattingService;
import ru.otus.hw.service.TestServiceImpl;

@ExtendWith(MockitoExtension.class)
class TestServiceUnitTest {

  @Mock
  private IOService ioService;

  @Mock
  private QuestionDao questionDao;

  @Mock
  private QuestionFormattingService formattingService;

  private final Student student = new Student("Ivan", "Ivanov");

  @InjectMocks
  private TestServiceImpl testService;

  @Test
  @DisplayName("Should print start message when there are no questions")
  void shouldPrintStartMessageForEmptyQuestionList() {
    when(questionDao.findAll()).thenReturn(List.of());

    testService.executeTestFor(student);

    verify(ioService).printLine("");
    verify(ioService).printFormattedLine("Please answer the questions below%n");
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
    verify(ioService).printLine("");
    verify(ioService).printFormattedLine("Please answer the questions below%n");
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
    verify(ioService).printLine("");
    verify(ioService).printFormattedLine("Please answer the questions below%n");
    verify(ioService).printFormattedLine(formattedQuestion);
  }
}
