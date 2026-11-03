package ru.otus.hw.authorization.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.otus.hw.authorization.dto.CsrfDto;

@RestController
public class CsrfController {

  @GetMapping("/api/csrf")
  public ResponseEntity<CsrfDto> csrf(CsrfToken csrf) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
      .body(new CsrfDto(csrf.getHeaderName(), csrf.getParameterName(), csrf.getToken()));
  }
}
