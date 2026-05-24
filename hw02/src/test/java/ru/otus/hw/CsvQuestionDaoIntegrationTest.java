package ru.otus.hw;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.otus.hw.config.TestFileNameProvider;
import ru.otus.hw.dao.CsvQuestionDao;
import ru.otus.hw.domain.Question;
import ru.otus.hw.exceptions.QuestionReadException;

@ExtendWith(MockitoExtension.class)
class CsvQuestionDaoIntegrationTest {

  @Mock
  private TestFileNameProvider fileNameProvider;

  @InjectMocks
  private CsvQuestionDao csvQuestionDao;

  @Test
  @DisplayName("Should read questions from valid CSV file")
  void shouldReadQuestionsFromValidCsvFile() {
    when(fileNameProvider.getTestFileName()).thenReturn("questions.csv");

    List<Question> questions = csvQuestionDao.findAll();

    assertThat(questions).isNotNull();
    assertThat(questions).hasSize(3);

    Question firstQuestion = questions.get(0);
    assertThat(firstQuestion.text()).isEqualTo("Is there life on Mars?");

    Question secondQuestion = questions.get(1);
    assertThat(secondQuestion.text()).isEqualTo("How should resources be loaded form jar in Java?");

    Question thirdQuestion = questions.get(2);
    assertThat(thirdQuestion.text()).isEqualTo("Which option is a good way to handle the exception?");
  }

  @Test
  @DisplayName("Should throw exception when file name is null")
  void shouldThrowExceptionWhenFileNameIsNull() {
    when(fileNameProvider.getTestFileName()).thenReturn(null);

    assertThatThrownBy(() -> csvQuestionDao.findAll())
      .isInstanceOf(QuestionReadException.class)
      .hasMessageContaining("Question file name is null or empty");
  }

  @Test
  @DisplayName("Should return empty list for empty files")
  void shouldReturnEmptyListForEmptyFiles() {
    when(fileNameProvider.getTestFileName()).thenReturn("empty-questions.csv");
    List<Question> questions = csvQuestionDao.findAll();
    assertThat(questions).isEmpty();
  }
}
