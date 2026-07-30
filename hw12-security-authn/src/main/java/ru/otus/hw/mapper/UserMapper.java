package ru.otus.hw.mapper;

import org.springframework.stereotype.Component;
import ru.otus.hw.dto.UserDto;
import ru.otus.hw.dto.UserForm;
import ru.otus.hw.model.User;

@Component
public class UserMapper {

  public UserDto toDto(User user) {
    return new UserDto(user.getName(), user.getPassword());
  }

  public User toEntity(UserForm form) {
    return new User(normalize(form.getName()), form.getPassword());
  }

  private String normalize(String value) {
    return value == null ? null : value.trim();
  }
}
