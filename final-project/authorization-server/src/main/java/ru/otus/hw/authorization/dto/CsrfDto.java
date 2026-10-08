package ru.otus.hw.authorization.dto;

public record CsrfDto(String headerName, String parameterName, String token) {

}
