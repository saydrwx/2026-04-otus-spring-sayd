package ru.otus.hw.web.configuration;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

public final class HttpClients {

  private HttpClients() {
  }

  public static SimpleClientHttpRequestFactory requests(WebProperties properties) {
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(properties.connectTimeout());
    factory.setReadTimeout(properties.readTimeout());
    return factory;
  }

  public static SimpleClientHttpRequestFactory publicDirectoryRequests(WebProperties properties) {
    var factory = new SimpleClientHttpRequestFactory() {
      @Override
      protected void prepareConnection(java.net.HttpURLConnection connection, String method)
        throws java.io.IOException {
        super.prepareConnection(connection, method);
        connection.setInstanceFollowRedirects(false);
      }
    };
    factory.setConnectTimeout(properties.connectTimeout());
    factory.setReadTimeout(properties.readTimeout());
    return factory;
  }

}
