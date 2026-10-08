package ru.otus.hw.web.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class DirectoryConfiguration {

  @Bean
  RestClient directoryRestClient(RestClient.Builder builder, WebProperties p) {
    return builder.clone().baseUrl(p.directoryBaseUrl().toString())
      .requestFactory(HttpClients.publicDirectoryRequests(p)).build();
  }
}
