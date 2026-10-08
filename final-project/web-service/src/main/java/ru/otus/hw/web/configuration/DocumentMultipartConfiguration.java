package ru.otus.hw.web.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import ru.otus.hw.web.security.MultipartCsrfFilter;

@Configuration(proxyBeanMethods = false)
public class DocumentMultipartConfiguration {

  @Bean
  FilterRegistrationBean<MultipartCsrfFilter> documentMultipartFilter(
    @Qualifier("multipartResolver") MultipartResolver resolver) {
    var registration = new FilterRegistrationBean<>(new MultipartCsrfFilter(resolver));
    registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER - 1);
    return registration;
  }
}
