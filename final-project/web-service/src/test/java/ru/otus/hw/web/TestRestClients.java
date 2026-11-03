package ru.otus.hw.web;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

final class TestRestClients {

  private TestRestClients() {
  }

  static RestClient.Builder builder(String baseUrl) {
    var mapper = JsonMapper.builder().addModule(new JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
    return RestClient.builder().baseUrl(baseUrl).messageConverters(converters -> {
      converters.removeIf(converter -> converter instanceof MappingJackson2HttpMessageConverter);
      converters.add(new MappingJackson2HttpMessageConverter(mapper));
    });
  }
}
