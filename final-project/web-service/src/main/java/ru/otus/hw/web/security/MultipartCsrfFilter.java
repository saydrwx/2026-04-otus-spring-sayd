package ru.otus.hw.web.security;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;

@RequiredArgsConstructor
public class MultipartCsrfFilter extends OncePerRequestFilter {

  private final MultipartResolver resolver;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
    FilterChain chain) throws ServletException, IOException {
    if (!"POST".equals(request.getMethod()) || !request.getRequestURI()
      .equals(request.getContextPath() + "/patient/documents/upload")
      || request instanceof MultipartHttpServletRequest || !resolver.isMultipart(request)) {
      chain.doFilter(request, response);
      return;
    }
    MultipartHttpServletRequest parsed;
    try {
      parsed = resolver.resolveMultipart(request);
    } catch (MaxUploadSizeExceededException exception) {
      reject(response, 413);
      return;
    } catch (MultipartException exception) {
      reject(response, 400);
      return;
    }
    try {
      chain.doFilter(parsed, response);
    } finally {
      resolver.cleanupMultipart(parsed);
    }
  }

  private void reject(HttpServletResponse response, int status) throws IOException {
    response.setHeader("Cache-Control", "no-store");
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.sendError(status,
      status == 413 ? "Превышен допустимый размер файла" : "Некорректный запрос загрузки файла");
  }
}
