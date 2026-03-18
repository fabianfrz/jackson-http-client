package eu.fabianfranz.jackson.net.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/**
 * {@link HttpResponse.BodyHandler} for the Java 9+ HTTP client implementation.
 *
 * Example usage:
 * <pre>
 *     var resp = httpClient.send(request, new JSONBodyHandler<>(ResponseType.class)
 *     ResponseType x = resp.body();
 * </pre>
 *
 * Important note: This class checks the HTTP header for the <pre>Content-Type</pre> to be a JSON type.
 * It will throw in any other case.
 *
 * @param <T> the type to decode.
 */
public class JSONBodyHandler<T> implements HttpResponse.BodyHandler<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JSONBodyHandler.class);

    private final Class<T> clazz;
    private final ObjectMapper mapper;

    public JSONBodyHandler(Class<T> clazz) {
        this.clazz = clazz;
        this.mapper = new ObjectMapper();
    }
    public JSONBodyHandler(Class<T> clazz, ObjectMapper mapper) {
        this.clazz = clazz;
        this.mapper = mapper;
    }

    @Override
    public HttpResponse.BodySubscriber<T> apply(HttpResponse.ResponseInfo responseInfo) {
        var contentType = responseInfo.headers().firstValue("Content-Type")
            .orElseThrow(() -> new IllegalStateException("Missing Content-Type"));
        if (!contentType.contains("application/json")) {
            throw new IllegalStateException("Content is not JSON. It is " + contentType);
        }

        return new JSONBodySubscriber();
    }

    /**
     * {@link java.net.http.HttpResponse.BodySubscriber} that does the actual work.
     * The getBody will be called first to get the future.
     * Afterward we have to read the entire body and then decode the result.
     */
    private class JSONBodySubscriber implements HttpResponse.BodySubscriber<T> {
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private final CompletableFuture<T> future = new CompletableFuture<>();

        @Override
        public CompletionStage<T> getBody() {
            return future;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            for (var byteBuffer : item) {
                var bytes = new byte[byteBuffer.remaining()];
                byteBuffer.get(bytes);
                try {
                    output.write(bytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }

        @Override
        public void onError(Throwable throwable) {
            LOGGER.error("Error on response parsing", throwable);
            future.cancel(true);
        }

        @Override
        public void onComplete() {
            T obj = mapper.readValue(output.toByteArray(), clazz);
            future.complete(obj);
        }
    }
}
