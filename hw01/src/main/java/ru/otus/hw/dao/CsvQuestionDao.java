package ru.otus.hw.dao;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import ru.otus.hw.config.TestFileNameProvider;
import ru.otus.hw.dao.dto.QuestionDto;
import ru.otus.hw.domain.Question;

import java.util.List;
import ru.otus.hw.exceptions.QuestionReadException;

@RequiredArgsConstructor
public class CsvQuestionDao implements QuestionDao {

  private static final char SEPARATOR = ';';

  private static final int NUM_LINES_TO_SKIP = 1;

  private final TestFileNameProvider fileNameProvider;

  @Override
  public List<Question> findAll() {
    String testFileName = fileNameProvider.getTestFileName();
    if (testFileName == null || testFileName.isBlank()) {
      throw new QuestionReadException("Question file name is null or empty");
    }

    try (InputStream testFileStream = getClass().getClassLoader()
      .getResourceAsStream(testFileName)) {

      if (testFileStream == null) {
        throw new QuestionReadException("Question file stream is null");
      }

      try (Reader testFileReader = new BufferedReader(
        new InputStreamReader(testFileStream, StandardCharsets.UTF_8))) {

        var csvToBean = getCsvToBean(testFileReader, QuestionDto.class);

        return csvToBean.parse()
          .stream()
          .map(QuestionDto::toDomainObject)
          .toList();
      }
    } catch (IOException e) {
      throw new QuestionReadException("IO exception while reading question file", e);
    }
  }

  private <T> CsvToBean<T> getCsvToBean(Reader reader, Class<T> clazz) {
    return new CsvToBeanBuilder<T>(reader)
      .withType(clazz)
      .withSeparator(SEPARATOR)
      .withIgnoreLeadingWhiteSpace(true)
      .withSkipLines(NUM_LINES_TO_SKIP)
      .build();
  }
}
