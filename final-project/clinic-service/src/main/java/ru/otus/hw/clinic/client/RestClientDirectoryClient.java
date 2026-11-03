package ru.otus.hw.clinic.client;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ru.otus.hw.clinic.exception.DirectoryUnavailableException;
import ru.otus.hw.clinic.exception.ResourceNotFoundException;
import ru.otus.hw.clinic.exception.TransientDirectoryException;

@Component
public class RestClientDirectoryClient implements DirectoryClient {

  private final RestClient restClient;

  private final Retry retry;

  private final CircuitBreaker circuitBreaker;

  public RestClientDirectoryClient(
    @Qualifier("directoryRestClient") RestClient restClient,
    @Qualifier("directoryRetry") Retry retry,
    @Qualifier("directoryCircuitBreaker") CircuitBreaker circuitBreaker
  ) {
    this.restClient = restClient;
    this.retry = retry;
    this.circuitBreaker = circuitBreaker;
  }

  @Override
  public DirectoryDoctorDto findDoctor(long doctorId) {
    Supplier<DirectoryDoctorDto> lookup = Retry.decorateSupplier(retry,
      () -> requestDoctor(doctorId));
    try {
      return circuitBreaker.executeSupplier(lookup);
    } catch (CallNotPermittedException exception) {
      throw new DirectoryUnavailableException("Directory is temporarily unavailable", exception);
    }
  }

  private DirectoryDoctorDto requestDoctor(long doctorId) {
    try {
      DirectoryDoctorDto doctor = restClient.get().uri("/api/doctors/{id}", doctorId)
        .retrieve().body(DirectoryDoctorDto.class);
      if (doctor == null || doctor.id() != doctorId || doctor.hospital() == null
        || doctor.hospital().id() <= 0) {
        throw new DirectoryUnavailableException("Directory returned an invalid provider response");
      }
      return doctor;
    } catch (HttpClientErrorException.NotFound exception) {
      throw new ResourceNotFoundException("Doctor was not found");
    } catch (ResourceAccessException exception) {
      throw new TransientDirectoryException(exception);
    } catch (RestClientResponseException exception) {
      int status = exception.getStatusCode().value();
      if (status == 500 || status == 502 || status == 503 || status == 504) {
        throw new TransientDirectoryException(exception);
      }
      throw new DirectoryUnavailableException("Directory request could not be completed",
        exception);
    } catch (RestClientException exception) {
      throw new DirectoryUnavailableException("Directory returned an invalid provider response",
        exception);
    }
  }
}
