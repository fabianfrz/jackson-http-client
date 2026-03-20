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
 * {@link java.net.http.HttpResponse.BodySubscriber} that does the actual work.
 * The getBody will be called first to get the future.
 * Afterward we have to read the entire body and then decode the result.
 * @param <T> the return type to be serialized
 */
public class JSONBodySubscriber<T> implements HttpResponse.BodySubscriber<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JSONBodySubscriber.class.getName());
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final CompletableFuture<T> future = new CompletableFuture<>();
    private final Class<T> clazz;
    private final ObjectMapper mapper;

    public JSONBodySubscriber(Class<T> clazz, ObjectMapper mapper) {
        this.clazz = clazz;
        this.mapper = mapper;
    }

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