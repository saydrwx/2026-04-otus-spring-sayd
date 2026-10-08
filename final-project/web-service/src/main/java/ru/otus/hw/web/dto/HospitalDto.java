package ru.otus.hw.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HospitalDto(long id, String name, String address) {

}
