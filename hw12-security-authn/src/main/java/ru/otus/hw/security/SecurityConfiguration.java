package ru.otus.hw.security;

import java.util.Collections;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import ru.otus.hw.model.User;
import ru.otus.hw.repository.UserRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .csrf(AbstractHttpConfigurer::disable)
      .authorizeHttpRequests(authorize -> authorize
        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
        .requestMatchers("/login", "/register").permitAll()
        .anyRequest().authenticated())
      .formLogin(form -> form
        .loginPage("/login")
        .failureUrl("/login?error")
        .defaultSuccessUrl("/", true)
        .permitAll())
      .logout(logout -> logout
        .logoutSuccessUrl("/login?logout")
        .permitAll()
      );
    return http.build();
  }

  @Bean
  public PasswordEncoder bCryptPasswordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  public UserDetailsService userDetailsService(UserRepository userRepository) {
    return userName -> {
      User user = userRepository.findByName(userName)
        .orElseThrow(() -> new UsernameNotFoundException(
          "User with name %s was not found".formatted(userName)));

      return org.springframework.security.core.userdetails.User.builder()
        .username(user.getName())
        .password(user.getPassword())
        .authorities(Collections.emptyList())
        .build();
    };
  }
}
