package ru.otus.hw.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.otus.hw.domain.Student;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Service
public class StudentServiceImpl implements StudentService {

  private final IOService ioService;

  @Override
  public Student determineCurrentStudent() {
    var firstName = ioService.readStringWithPrompt("Please input your first name");
    var lastName = ioService.readStringWithPrompt("Please input your last name");
    return new Student(firstName, lastName);
  }
}
