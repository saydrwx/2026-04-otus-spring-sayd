package ru.otus.hw.web;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartResolver;
import ru.otus.hw.web.security.MultipartCsrfFilter;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class MultipartCsrfFilterTest {
  MockHttpServletRequest request() {var r=new MockHttpServletRequest("POST","/patient/documents/upload");r.setContentType("multipart/form-data; boundary=test");return r;}
  @Test void resolvesNativeMultipartBeforeSecurityAndAlwaysCleansIt() throws Exception {
    var resolver=mock(MultipartResolver.class);var request=request();var parsed=new MockMultipartHttpServletRequest();parsed.setParameter("_csrf","masked-token");
    when(resolver.isMultipart(request)).thenReturn(true);when(resolver.resolveMultipart(request)).thenReturn(parsed);
    var filter=new MultipartCsrfFilter(resolver);var response=new MockHttpServletResponse();
    filter.doFilter(request,response,(wrapped,result) -> {assertThat(wrapped).isSameAs(parsed);assertThat(wrapped.getParameter("_csrf")).isEqualTo("masked-token");});
    verify(resolver).cleanupMultipart(parsed);
  }
  @Test void containerLimitErrorsDoNotExposeExceptionTextOrRunTheChain() throws Exception {
    var resolver=mock(MultipartResolver.class);var request=request();var response=new MockHttpServletResponse();when(resolver.isMultipart(request)).thenReturn(true);
    when(resolver.resolveMultipart(request)).thenThrow(new MaxUploadSizeExceededException(5*1024*1024));
    new MultipartCsrfFilter(resolver).doFilter(request,response,(r,s) -> {throw new AssertionError("No chain after parse failure");});
    assertThat(response.getStatus()).isEqualTo(413);assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
    assertThat(response.getErrorMessage()).isEqualTo("Превышен допустимый размер файла");
  }
  @Test void malformedMultipartBecomesGeneric400() throws Exception {
    var resolver=mock(MultipartResolver.class);var request=request();var response=new MockHttpServletResponse();when(resolver.isMultipart(request)).thenReturn(true);
    when(resolver.resolveMultipart(request)).thenThrow(new MultipartException("private-filename-and-parser-detail"));
    new MultipartCsrfFilter(resolver).doFilter(request,response,(r,s) -> {throw new AssertionError("No chain after parse failure");});
    assertThat(response.getStatus()).isEqualTo(400);assertThat(response.getErrorMessage()).doesNotContain("private-filename");
  }
  @Test void alreadyWrappedRequestsAreNotParsedOrCleanedTwice() throws Exception {
    var resolver=mock(MultipartResolver.class);var request=new MockMultipartHttpServletRequest();request.setMethod("POST");request.setRequestURI("/patient/documents/upload");
    new MultipartCsrfFilter(resolver).doFilter(request,new MockHttpServletResponse(),(r,s) -> assertThat(r).isSameAs(request));verifyNoInteractions(resolver);
  }
}
