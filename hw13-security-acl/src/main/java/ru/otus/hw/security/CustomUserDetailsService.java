package ru.otus.hw.security;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.otus.hw.model.Role;
import ru.otus.hw.model.User;
import ru.otus.hw.repository.UserRepository;

@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
    User user = userRepository.findByName(userName)
      .orElseThrow(() -> new UsernameNotFoundException(
        "User with name %s was not found".formatted(userName)));
    List<String> roles = user.getRoles() != null ?
      user.getRoles().stream().map(Role::getName).toList() :
      new ArrayList<>();

    return org.springframework.security.core.userdetails.User.builder()
      .username(user.getName())
      .password(user.getPassword())
      .roles(roles.toArray(new String[0]))
      .build();
  }
}
