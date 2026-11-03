package ru.otus.hw.directory.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.otus.hw.directory.dto.SpecialtyDto;
import ru.otus.hw.directory.dto.SpecialtyInputDto;
import ru.otus.hw.directory.service.SpecialtyService;

@RequiredArgsConstructor
@RestController
public class SpecialtyController {

  private final SpecialtyService specialtyService;

  @GetMapping("/api/specialties/{id}")
  public SpecialtyDto findById(@PathVariable long id) {
    return specialtyService.findById(id);
  }

  @GetMapping("/api/specialties")
  public List<SpecialtyDto> findAll() {
    return specialtyService.findAll();
  }

  @PostMapping("/api/specialties")
  public ResponseEntity<SpecialtyDto> create(@Valid @RequestBody SpecialtyInputDto inputDto) {
    SpecialtyDto specialtyDto = specialtyService.create(inputDto);
    return ResponseEntity.created(URI.create("/api/specialties/" + specialtyDto.id()))
      .body(specialtyDto);
  }

  @PutMapping("/api/specialties/{id}")
  public SpecialtyDto update(@PathVariable long id, @Valid @RequestBody SpecialtyInputDto inputDto) {
    return specialtyService.update(id, inputDto);
  }

  @DeleteMapping("/api/specialties/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable long id) {
    specialtyService.deleteById(id);
  }
}
