package ru.otus.hw.authorization.security;

import ru.otus.hw.authorization.util.EmailAddresses;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.otus.hw.authorization.model.Account;
import ru.otus.hw.authorization.repository.AccountRepository;

@RequiredArgsConstructor
@Service
public class AccountUserDetailsService implements UserDetailsService {

  private final AccountRepository accountRepository;

  @Override
  public UserDetails loadUserByUsername(String email) {
    Account account = accountRepository.findByEmail(EmailAddresses.normalize(email))
      .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    return User.withUsername(account.id().toString())
      .password(account.password())
      .roles(account.role())
      .build();
  }
}
