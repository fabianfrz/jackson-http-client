package eu.fabianfranz.jackson.net.http;


import tools.jackson.core.type.TypeReference;
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
public class JacksonBodySubscriber<T> implements HttpResponse.BodySubscriber<T> {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final CompletableFuture<T> future = new CompletableFuture<>();
    private final Class<T> clazz;
    private final ObjectMapper mapper;
    private final TypeReference<T> typeReference;

    /**
     * This constuctor allows to deserialize a simple class
     * @param clazz the class to deserialize
     * @param mapper the mapper used for deserialization
     */
    public JacksonBodySubscriber(Class<T> clazz, ObjectMapper mapper) {
        this.clazz = clazz;
        this.mapper = mapper;
        this.typeReference = null;
    }

    /**
     * This constructor allows to use a type reference for typed generic collections etc.
     * @param typeReference the type of the return value
     * @param mapper the mapper to use
     */
    public JacksonBodySubscriber(TypeReference<T> typeReference, ObjectMapper mapper) {
        this.clazz = null;
        this.mapper = mapper;
        this.typeReference = typeReference;
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
        future.completeExceptionally(throwable); // this will rethrow the exception on .get().
    }

    @Override
    public void onComplete() {
        T obj = typeReference != null
            ? mapper.readValue(output.toByteArray(), typeReference)
            : mapper.readValue(output.toByteArray(), clazz);
        future.complete(obj);
    }
}