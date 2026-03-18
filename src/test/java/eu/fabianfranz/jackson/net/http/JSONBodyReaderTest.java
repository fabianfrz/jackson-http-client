package eu.fabianfranz.jackson.net.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class JSONBodyReaderTest {
    HttpServer server;

    public void handle(HttpExchange t) throws IOException {
        byte[] body = null;
        try (InputStream is = t.getRequestBody()) {
            body = IOUtils.toByteArray(is);
        }
        String response = """
                {
                    "message": "Hello World!"
                }
                """;
        t.getResponseHeaders().set("Content-Type", "application/json");
        t.sendResponseHeaders(200, response.length());
        try (OutputStream os = t.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    @BeforeEach
    void before() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8000), 10);
        server.createContext("/", this::handle);
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    @AfterEach
    void after() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void testIt() throws URISyntaxException {

        MockRequest data = new MockRequest();
        data.setData("Data");
        try (HttpClient httpClient = HttpClient.newBuilder().build()) {
            var request = HttpRequest.newBuilder(new URI("http://127.0.0.1:8000/endpoint"))
                .header("Content-Type", "application/json")
                .POST(new JSONBodyPublisher<>(data))
                .build();
            var response = assertDoesNotThrow(
                () -> httpClient.send(request, new JSONBodyHandler<>(MockResponse.class))
            );
            assertEquals("Hello World!", response.body().getMessage());
        }
    }

}