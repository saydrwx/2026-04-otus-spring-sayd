package ru.otus.hw.web.configuration;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class PrivateApiConfiguration {

  @Bean
  RestClient registrationRestClient(RestClient.Builder builder, WebProperties p) {
    return builder.clone().baseUrl(p.authBackendBaseUrl().toString()).requestFactory(requests(p))
      .build();
  }

  @Bean
  RestClient adminDirectoryRestClient(RestClient.Builder builder, WebProperties p) {
    return builder.clone().baseUrl(p.directoryBaseUrl().toString()).requestFactory(requests(p))
      .build();
  }

  @Bean
  RestClient clinicRestClient(RestClient.Builder builder, WebProperties p) {
    return builder.clone().baseUrl(p.clinicBaseUrl().toString()).requestFactory(requests(p))
      .build();
  }

  @Bean
  RestClient documentRestClient(RestClient.Builder builder, WebProperties p) {
    return builder.clone().baseUrl(p.clinicBaseUrl().toString())
      .requestFactory(requests(p, p.documentReadTimeout())).build();
  }

  static JdkClientHttpRequestFactory requests(WebProperties p) {
    return requests(p, p.readTimeout());
  }

  private static JdkClientHttpRequestFactory requests(WebProperties p, Duration readTimeout) {
    var client = HttpClient.newBuilder().connectTimeout(p.connectTimeout())
      .followRedirects(HttpClient.Redirect.NEVER).build();
    var factory = new JdkClientHttpRequestFactory(client);
    factory.setReadTimeout(readTimeout);
    return factory;
  }
}
