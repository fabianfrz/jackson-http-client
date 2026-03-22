package eu.fabianfranz.jackson.net.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class JSONConversationTest {
    public static final String RESPONSE_TEMPLATE = """
            {
                "message": "%s"
            }
            """;
    private HttpServer server;
    private byte[] body;
    private String responseBody;
    private URI endpointURI;

    public void handle(HttpExchange t) throws IOException {
        try (InputStream is = t.getRequestBody()) {
            body = is.readAllBytes();
        }
        t.getResponseHeaders().set("Content-Type", "application/json");
        t.sendResponseHeaders(200, responseBody.length());
        try (OutputStream os = t.getResponseBody()) {
            os.write(responseBody.getBytes(StandardCharsets.UTF_8));
        }
    }

    @BeforeEach
    void before() throws IOException, URISyntaxException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 10);
        server.createContext("/", this::handle);
        server.setExecutor(null); // creates a default executor
        server.start();
        var port = server.getAddress().getPort();
        endpointURI = new URI("http://127.0.0.1:%d/endpoint".formatted(port));
    }

    @AfterEach
    void after() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void testIt() {

        MockRequest data = new MockRequest();
        data.setData("Data");
        responseBody = RESPONSE_TEMPLATE.formatted("Hello World!");
        HttpClient httpClient = HttpClient.newBuilder().build();
        var request = HttpRequest.newBuilder(endpointURI)
            .header("Content-Type", "application/json")
            .POST(new JacksonBodyPublisher<>(data))
            .build();
        var response = assertDoesNotThrow(
            () -> httpClient.send(request, new JacksonBodyHandler<>(MockResponse.class))
        );
        assertEquals("Hello World!", response.body().getMessage());
    }

    @Test
    void testMultiFormatHandlerSuccessful() {

        MockRequest data = new MockRequest();
        data.setData("Data");
        responseBody = RESPONSE_TEMPLATE.formatted("Hello World!");
        HttpClient httpClient = HttpClient.newBuilder().build();
        var request = HttpRequest.newBuilder(endpointURI)
            .header("Content-Type", "application/json")
            .POST(new JacksonBodyPublisher<>(data))
            .build();
        var mappers = Map.of(JacksonMultiFormatHandler.JSON, new ObjectMapper(),
            Pattern.compile("text/yaml(\\+.*)"), new ObjectMapper());
        var response = assertDoesNotThrow(
            () -> httpClient.send(request, new JacksonMultiFormatHandler<>(MockResponse.class, mappers))
        );
        assertEquals("Hello World!", response.body().getMessage());
    }

    @Test
    void testMultiFormatHandlerFailureEmptyConfig() {
        var error = assertThrows(IllegalArgumentException.class,
            () -> new JacksonMultiFormatHandler<>(MockResponse.class, Map.of())
        );
        assertTrue(error.getMessage().contains("empty"));
    }

    @Test
    void testMultiFormatHandlerNoDecoder() {

        MockRequest data = new MockRequest();
        data.setData("Data");
        responseBody = RESPONSE_TEMPLATE.formatted("Hello World!");
        HttpClient httpClient = HttpClient.newBuilder().build();
        var request = HttpRequest.newBuilder(endpointURI)
                .header("Content-Type", "application/json")
                .POST(new JacksonBodyPublisher<>(data))
                .build();
        var mappers = Map.of(Pattern.compile("Not/Existing"), new ObjectMapper());
        var response = assertThrows(Exception.class,
                () -> httpClient.send(request, new JacksonMultiFormatHandler<>(MockResponse.class, mappers))
        );

        var cause = response.getCause();
        assertTrue(cause.getMessage().contains("Content type is not known"));
    }


    @Test
    void testItWithHugeRequestAndResponse() {

        MockRequest data = new MockRequest();
        int testSize = 1024 * 1024 * 5; // 5mb
        data.setData("A".repeat(testSize));
        responseBody = RESPONSE_TEMPLATE.formatted("A".repeat(testSize)); // 5MB
        HttpClient httpClient = HttpClient.newBuilder().build();
        var request = HttpRequest.newBuilder(endpointURI)
            .header("Content-Type", "application/json")
            .POST(new JacksonBodyPublisher<>(data))
            .build();
        var response = assertDoesNotThrow(
            () -> httpClient.send(request, new JacksonBodyHandler<>(MockResponse.class))
        );
        assertEquals(testSize, response.body().getMessage().length());
        assertTrue(body.length > testSize);
    }

    @Test
    void testItWithInvalidResponse() {

        MockRequest data = new MockRequest();
        data.setData("Data");
        responseBody = "([{"; // 5MB
        HttpClient httpClient = HttpClient.newBuilder().build();
        var request = HttpRequest.newBuilder(endpointURI)
            .header("Content-Type", "application/json")
            .POST(new JacksonBodyPublisher<>(data))
            .build();
        var ioException = assertThrows(IOException.class,
            () -> httpClient.send(request, new JacksonBodyHandler<>(MockResponse.class))
        );

        assertTrue(ioException.getMessage().contains("Unexpected character"));
    }
    @Test
    void testItWithListResponse() {

        MockRequest data = new MockRequest();
        data.setData("Data");
        responseBody = "[" +
            RESPONSE_TEMPLATE.formatted("A") + "," +
            RESPONSE_TEMPLATE.formatted("B") + "]";

        HttpClient httpClient = HttpClient.newBuilder().build();
        var request = HttpRequest.newBuilder(endpointURI)
            .header("Content-Type", "application/json")
            .POST(new JacksonBodyPublisher<>(data))
            .build();
        var response = assertDoesNotThrow(
            () -> httpClient.send(request, new JacksonBodyHandler<>(new TypeReference<List<MockResponse>>() { }))
        );

        assertEquals(2, response.body().size());
    }

}