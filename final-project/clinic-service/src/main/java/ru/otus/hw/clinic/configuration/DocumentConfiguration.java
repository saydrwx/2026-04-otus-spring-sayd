package ru.otus.hw.clinic.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DocumentProperties.class)
@EnableScheduling
public class DocumentConfiguration {

}
