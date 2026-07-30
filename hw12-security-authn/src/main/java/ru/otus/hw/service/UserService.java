package ru.otus.hw.service;

import ru.otus.hw.dto.UserDto;
import ru.otus.hw.dto.UserForm;

public interface UserService {

  UserDto create(UserForm form);
}
