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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.otus.hw.directory.dto.DoctorDto;
import ru.otus.hw.directory.dto.DoctorInputDto;
import ru.otus.hw.directory.service.DoctorService;

@RequiredArgsConstructor
@RestController
public class DoctorController {

  private final DoctorService doctorService;

  @GetMapping("/api/doctors/{id}")
  public DoctorDto findById(@PathVariable long id) {
    return doctorService.findById(id);
  }

  @GetMapping("/api/doctors")
  public List<DoctorDto> findAll(
    @RequestParam(required = false) Long hospitalId,
    @RequestParam(required = false) List<Long> specialtyIds
  ) {
    return doctorService.findAll(hospitalId, specialtyIds);
  }

  @PostMapping("/api/doctors")
  public ResponseEntity<DoctorDto> create(@Valid @RequestBody DoctorInputDto inputDto) {
    DoctorDto doctorDto = doctorService.create(inputDto);
    return ResponseEntity.created(URI.create("/api/doctors/" + doctorDto.id()))
      .body(doctorDto);
  }

  @PutMapping("/api/doctors/{id}")
  public DoctorDto update(@PathVariable long id, @Valid @RequestBody DoctorInputDto inputDto) {
    return doctorService.update(id, inputDto);
  }

  @DeleteMapping("/api/doctors/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable long id) {
    doctorService.deleteById(id);
  }
}
