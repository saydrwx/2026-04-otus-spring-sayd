package ru.otus.hw.web.controller;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import ru.otus.hw.web.dto.*;
import ru.otus.hw.web.exception.DocumentsException;
import ru.otus.hw.web.security.DocumentSession;
import ru.otus.hw.web.service.DocumentService;
import ru.otus.hw.web.service.PatientProfileService;
import ru.otus.hw.web.validation.DocumentFiles;

import static ru.otus.hw.web.exception.DocumentsException.Kind.*;

@RequiredArgsConstructor
@Controller
public class DocumentController {

  private static final int PAGE_SIZE = 20;
  private final DocumentService documents;
  private final PatientProfileService profiles;
  private final DocumentSession session;
  private final DocumentFiles files;

  @GetMapping("/patient/documents")
  public String own(@RequestParam(defaultValue = "0") @Min(0) int page, Authentication user,
    HttpServletRequest request, HttpServletResponse response, Model model) {
    return session.call(user, request, response, token -> {
      var profile = profiles.find(token);
      if (profile.isEmpty()) {
        return "redirect:/patient/onboarding";
      }
      return listing(documents.list(token, profile.get().id(), page, PAGE_SIZE), false, null,
        model);
    });
  }

  @GetMapping("/doctor/patients/{id}/documents")
  public String patient(@PathVariable UUID id, @RequestParam(defaultValue = "0") @Min(0) int page,
    Authentication user, HttpServletRequest request, HttpServletResponse response, Model model) {
    return session.call(user, request, response,
      token -> listing(documents.list(token, id, page, PAGE_SIZE), true, id, model));
  }

  @GetMapping({"/patient/documents/{id}", "/doctor/documents/{id}"})
  public String detail(@PathVariable UUID id, Authentication user, HttpServletRequest request,
    HttpServletResponse response, Model model) {
    var value = session.call(user, request, response, token -> documents.find(token, id));
    model.addAttribute("document", value);
    model.addAttribute("doctorView", doctor(request));
    return "documents/detail";
  }

  @GetMapping("/patient/documents/upload")
  public String uploadForm(Authentication user, HttpServletRequest request,
    HttpServletResponse response) {
    return session.call(user, request, response,
      token -> profiles.find(token).isEmpty() ? "redirect:/patient/onboarding"
        : "documents/upload");
  }

  @PostMapping(value = "/patient/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public String upload(@RequestPart("file") MultipartFile file, MultipartHttpServletRequest request,
    Authentication user, HttpServletResponse response, Model model) {
    try {
      if (request.getMultiFileMap().values().stream().mapToInt(java.util.List::size).sum() != 1
        || request.getParameterMap().keySet().stream().anyMatch(name -> !name.equals("_csrf"))) {
        throw new DocumentsException(INVALID);
      }
      var input = files.prepare(file);
      return session.call(user, request, response, token -> {
        var profile = profiles.find(token);
        if (profile.isEmpty()) {
          return "redirect:/patient/onboarding";
        }
        var created = documents.upload(token, input);
        if (!profile.get().id().equals(created.patientId())) {
          throw new DocumentsException(UNAVAILABLE);
        }
        return "redirect:/patient/documents/" + created.id();
      });
    } catch (DocumentsException exception) {
      response.setStatus(exception.kind().status().value());
      model.addAttribute("message", exception.kind() == UNAVAILABLE
        ? "Загрузка не подтверждена. Проверьте список документов, прежде чем выбирать и загружать файл повторно."
        : exception.kind() == CONFLICT
          ? "Загрузка противоречит текущим данным. Проверьте список документов перед повторной загрузкой."
          : exception.kind().message());
      return "documents/upload";
    }
  }

  @GetMapping({"/patient/documents/{id}/download", "/doctor/documents/{id}/download"})
  public ResponseEntity<byte[]> download(@PathVariable UUID id, Authentication user,
    HttpServletRequest request, HttpServletResponse response) {
    var result = session.call(user, request, response, token -> documents.download(token, id));
    if (result == null || !files.valid(result.metadata()) || !id.equals(result.metadata().id())
      || result.bytes() == null || result.bytes().length != result.metadata().sizeBytes()
      || result.bytes().length > DocumentFiles.MAX_BYTES) {
      throw new DocumentsException(UNAVAILABLE);
    }
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(result.metadata().mediaType()))
      .contentLength(result.bytes().length)
      .cacheControl(CacheControl.noStore().cachePrivate())
      .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
        .filename(result.metadata().filename(), StandardCharsets.UTF_8).build().toString())
      .header("Content-Security-Policy", "sandbox; default-src 'none'")
      .header("X-Content-Type-Options", "nosniff")
      .header("Referrer-Policy", "no-referrer").body(result.bytes());
  }

  @GetMapping("/patient/documents/{id}/delete")
  public String confirmDelete(@PathVariable UUID id, Authentication user,
    HttpServletRequest request, HttpServletResponse response, Model model) {
    var value = session.call(user, request, response, token -> documents.find(token, id));
    model.addAttribute("document", value);
    return "documents/delete";
  }

  @PostMapping("/patient/documents/{id}/delete")
  public String delete(@PathVariable UUID id, @RequestParam MultiValueMap<String, String> fields,
    Authentication user, HttpServletRequest request, HttpServletResponse response, Model model) {
    if (fields.keySet().stream().anyMatch(name -> !name.equals("_csrf"))) {
      throw new DocumentsException(INVALID);
    }
    try {
      session.call(user, request, response, token -> {
        documents.delete(token, id);
        return Boolean.TRUE;
      });
      return "redirect:/patient/documents";
    } catch (DocumentsException exception) {
      response.setStatus(exception.kind().status().value());
      model.addAttribute("doctorView", false);
      model.addAttribute("documentId", id);
      model.addAttribute("message", exception.kind() == UNAVAILABLE || exception.kind() == CONFLICT
        ? "Удаление не подтверждено. Проверьте список документов перед повторной попыткой."
        : exception.kind().message());
      return "documents/error";
    }
  }

  private String listing(DocumentPageDto page, boolean doctor, UUID patientId, Model model) {
    model.addAttribute("documents", page);
    model.addAttribute("doctorView", doctor);
    model.addAttribute("patientId", patientId);
    return "documents/list";
  }

  private boolean doctor(HttpServletRequest request) {
    return request.getRequestURI().startsWith("/doctor/");
  }
}
