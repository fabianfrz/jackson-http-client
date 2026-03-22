package eu.fabianfranz.jackson.net.http;


import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.concurrent.Flow;

/**
 * A {@link java.net.http.HttpRequest.BodyPublisher} implementation that can decode JSON DTOs directly.
 * It converts the object to JSON and then slices it into 4k blocks, which are sent to the HTTP client implementation.
 * @param <T>
 */
public class JacksonBodyPublisher<T> implements HttpRequest.BodyPublisher {
    private final byte[] bytes;
    private final static ObjectMapper DEFAULT_MAPPER = new ObjectMapper();

    /**
     * JSON Body publisher that serializes the object using the standard ObjectMapper.
     * @param data the data to serialize.
     */
    public JacksonBodyPublisher(T data) {
        this(data, DEFAULT_MAPPER);
    }

    /**
     * This constructor sends the object in data serialized to the web server.
     * @param data the data to serialize
     * @param mapper the object mapper that serializes the data.
     */
    public JacksonBodyPublisher(T data, ObjectMapper mapper) {
        bytes = mapper.writeValueAsBytes(data);
    }

    @Override
    public long contentLength() {
        return bytes.length;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
        subscriber.onSubscribe(new JacksonSubscription(subscriber, bytes));
    }
}
