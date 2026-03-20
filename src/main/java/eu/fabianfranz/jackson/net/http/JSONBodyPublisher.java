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
public class JSONBodyPublisher<T> implements HttpRequest.BodyPublisher {
    private final byte[] bytes;

    /**
     * JSON Body publisher that serializes the object using the standard ObjectMapper.
     * @param data the data to serialize.
     */
    public JSONBodyPublisher(T data) {
        this(data, new ObjectMapper());
    }

    /**
     * This constructor sends the object in data serialized to the web server.
     * @param data the data to serialize
     * @param mapper the object mapper that serializes the data.
     */
    public JSONBodyPublisher(T data, ObjectMapper mapper) {
        bytes = mapper.writeValueAsBytes(data);
    }

    @Override
    public long contentLength() {
        return bytes.length;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
        subscriber.onSubscribe(new JSONSubscription(subscriber, bytes));
    }
}
