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
import ru.otus.hw.directory.dto.HospitalDto;
import ru.otus.hw.directory.dto.HospitalInputDto;
import ru.otus.hw.directory.service.HospitalService;

@RequiredArgsConstructor
@RestController
public class HospitalController {

  private final HospitalService hospitalService;

  @GetMapping("/api/hospitals/{id}")
  public HospitalDto findById(@PathVariable long id) {
    return hospitalService.findById(id);
  }

  @GetMapping("/api/hospitals")
  public List<HospitalDto> findAll() {
    return hospitalService.findAll();
  }

  @PostMapping("/api/hospitals")
  public ResponseEntity<HospitalDto> create(@Valid @RequestBody HospitalInputDto inputDto) {
    HospitalDto hospitalDto = hospitalService.create(inputDto);
    return ResponseEntity.created(URI.create("/api/hospitals/" + hospitalDto.id()))
      .body(hospitalDto);
  }

  @PutMapping("/api/hospitals/{id}")
  public HospitalDto update(@PathVariable long id, @Valid @RequestBody HospitalInputDto inputDto) {
    return hospitalService.update(id, inputDto);
  }

  @DeleteMapping("/api/hospitals/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable long id) {
    hospitalService.deleteById(id);
  }
}
