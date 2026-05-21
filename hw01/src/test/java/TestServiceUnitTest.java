import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.otus.hw.dao.CsvQuestionDao;
import ru.otus.hw.dao.QuestionDao;
import ru.otus.hw.domain.Answer;
import ru.otus.hw.domain.Question;
import ru.otus.hw.service.IOService;
import ru.otus.hw.service.StreamsIOService;
import ru.otus.hw.service.TestService;
import ru.otus.hw.service.TestServiceImpl;

import static org.mockito.Mockito.*;

class TestServiceUnitTest {

  private IOService ioService;
  private QuestionDao questionDao;
  private TestService testService;

  @BeforeEach
  void setUp() {
    ioService = mock(StreamsIOService.class);
    questionDao = mock(CsvQuestionDao.class);
    testService = new TestServiceImpl(ioService, questionDao);
  }

  @Test
  @DisplayName("Should print start message when there are no questions")
  void shouldPrintStartMessageForEmptyQuestionList() {
    when(questionDao.findAll()).thenReturn(List.of());

    testService.executeTest();

    verify(ioService).printLine("");
    verify(ioService).printFormattedLine("Please answer the questions below%n");
    verify(questionDao).findAll();
    verifyNoMoreInteractions(ioService);
  }

  @Test
  @DisplayName("Should print a single question without answers")
  void shouldPrintSingleQuestionWithoutAnswers() {
    Question question = new Question("What is Java?", List.of());
    when(questionDao.findAll()).thenReturn(List.of(question));

    testService.executeTest();

    verify(ioService).printLine("");
    verify(ioService).printFormattedLine("Please answer the questions below%n");
    verify(ioService).printFormattedLine("What is Java?\n\n");
    verify(questionDao).findAll();
  }

  @Test
  @DisplayName("Should print a single question with multiple answers")
  void shouldPrintSingleQuestionWithAnswers() {
    List<Answer> answers = List.of(
      new Answer("Moscow", false),
      new Answer("Saratov", false),
      new Answer("Paris", true)
    );
    Question question = new Question("What is the capital of France?", answers);
    when(questionDao.findAll()).thenReturn(List.of(question));

    testService.executeTest();

    verify(ioService).printLine("");
    verify(ioService).printFormattedLine("Please answer the questions below%n");
    verify(ioService).printFormattedLine(
      """
        What is the capital of France?
        (1) Moscow%n\
        (2) Saratov%n\
        (3) Paris
        """
    );
  }
}
