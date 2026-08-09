package ru.otus.hw.service;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.dto.UserDto;
import ru.otus.hw.dto.UserForm;
import ru.otus.hw.exception.UserAlreadyExistsException;
import ru.otus.hw.mapper.UserMapper;
import ru.otus.hw.model.Role;
import ru.otus.hw.model.User;
import ru.otus.hw.repository.RoleRepository;
import ru.otus.hw.repository.UserRepository;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;

  private final RoleRepository roleRepository;

  private final UserMapper userMapper;

  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public UserDto create(UserForm form) {
    User user = userMapper.toEntity(form);
    String userName = user.getName();

    if (userRepository.existsByName(userName)) {
      throw new UserAlreadyExistsException(userName);
    }

    user.setPassword(passwordEncoder.encode(form.getPassword()));

    Set<Role> defaultRoles = roleRepository.findByName("USER").map(Set::of).orElse(Set.of());
    user.setRoles(defaultRoles);

    try {
      return userMapper.toDto(userRepository.saveAndFlush(user));
    } catch (DataIntegrityViolationException exception) {
      throw new UserAlreadyExistsException(userName, exception);
    }
  }
}
