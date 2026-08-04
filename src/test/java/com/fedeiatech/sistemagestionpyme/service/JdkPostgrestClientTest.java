package com.fedeiatech.sistemagestionpyme.service;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdkPostgrestClientTest {

    private HttpServer servidor;
    private String baseUrl;

    private String metodoRecibido;
    private String pathRecibido;
    private String bodyRecibido;
    private Headers headersRecibidos;
    private int statusCodeARetornar;
    private String bodyARetornar;

    @BeforeEach
    void setUp() throws IOException {
        statusCodeARetornar = 201;
        bodyARetornar = "[]";

        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/", exchange -> {
            metodoRecibido = exchange.getRequestMethod();
            pathRecibido = exchange.getRequestURI().toString();
            headersRecibidos = exchange.getRequestHeaders();

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            exchange.getRequestBody().transferTo(buffer);
            bodyRecibido = buffer.toString(StandardCharsets.UTF_8);

            byte[] respuesta = bodyARetornar.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCodeARetornar, respuesta.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(respuesta);
            }
        });
        servidor.start();
        baseUrl = "http://localhost:" + servidor.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        servidor.stop(0);
    }

    @Test
    void enviaMetodoPathHeadersYBodyCorrectos() throws IOException {
        PostgrestClient client = new JdkPostgrestClient(baseUrl);
        Map<String, String> headers = new HashMap<>();
        headers.put("apikey", "clave-anon");
        headers.put("Authorization", "Bearer token-123");
        headers.put("Prefer", "resolution=merge-duplicates");

        client.post("/rest/v1/products?on_conflict=sku", "{\"sku\":\"ABC\"}", headers);

        assertEquals("POST", metodoRecibido);
        assertEquals("/rest/v1/products?on_conflict=sku", pathRecibido);
        assertEquals("{\"sku\":\"ABC\"}", bodyRecibido);
        assertEquals("clave-anon", headersRecibidos.getFirst("apikey"));
        assertEquals("Bearer token-123", headersRecibidos.getFirst("Authorization"));
        assertEquals("resolution=merge-duplicates", headersRecibidos.getFirst("Prefer"));
    }

    @Test
    void mapeaRespuestaExitosaA2xx() throws IOException {
        statusCodeARetornar = 201;
        bodyARetornar = "[{\"id\":\"1\"}]";

        PostgrestClient client = new JdkPostgrestClient(baseUrl);
        PostgrestResponse respuesta = client.post("/rest/v1/products", "{}", new HashMap<>());

        assertEquals(201, respuesta.statusCode());
        assertEquals("[{\"id\":\"1\"}]", respuesta.body());
        assertTrue(respuesta.esExitosa());
    }

    @Test
    void mapeaRespuestaNoExitosaA401() throws IOException {
        statusCodeARetornar = 401;
        bodyARetornar = "{\"message\":\"invalid JWT\"}";

        PostgrestClient client = new JdkPostgrestClient(baseUrl);
        PostgrestResponse respuesta = client.post("/rest/v1/products", "{}", new HashMap<>());

        assertEquals(401, respuesta.statusCode());
        assertEquals("{\"message\":\"invalid JWT\"}", respuesta.body());
        assertTrue(!respuesta.esExitosa());
    }
}
