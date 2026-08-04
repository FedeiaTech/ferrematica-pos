package com.fedeiatech.sistemagestionpyme.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Production implementation of {@link PostgrestClient} using the JDK-built-in
 * {@code java.net.http} client (available since Java 11, no extra Maven dependency).
 */
public class JdkPostgrestClient implements PostgrestClient {

    private final String baseUrl;
    private final HttpClient httpClient;

    public JdkPostgrestClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public PostgrestResponse post(String path, String jsonBody, Map<String, String> headers) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        for (Map.Entry<String, String> header : headers.entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }

        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new PostgrestResponse(response.statusCode(), response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Sincronización interrumpida", e);
        }
    }
}
