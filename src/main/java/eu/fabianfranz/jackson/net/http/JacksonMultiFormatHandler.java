package eu.fabianfranz.jackson.net.http;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

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
public class JacksonMultiFormatHandler<T> implements HttpResponse.BodyHandler<T> {
    public static final Pattern JSON = Pattern.compile("application/json(\\+.*)?");

    private final Class<T> clazz;
    private final Map<Pattern, ObjectMapper> mappers;
    private final TypeReference<T> typeReference;

    /**
     * This constructor allows you to specify a returning class as well as the supported object mappers.
     * @param clazz the class to map
     * @param mappers the object mappers
     */
    public JacksonMultiFormatHandler(Class<T> clazz, Map<Pattern, ObjectMapper> mappers) {
        this.clazz = Objects.requireNonNull(clazz, "clazz cannot be null");
        this.mappers = Objects.requireNonNull(mappers, "mappers cannot be null");
        this.typeReference = null;
        if (mappers.isEmpty()) {
            throw new IllegalArgumentException("mappers cannot be empty");
        }
    }


    /**
     * This constructor allows you to specify a returning class as well as the supported object mappers.
     * @param typeReference the type to map
     * @param mappers the object mappers
     */
    public JacksonMultiFormatHandler(TypeReference<T> typeReference, Map<Pattern, ObjectMapper> mappers) {
        this.typeReference = Objects.requireNonNull(typeReference, "typeReference cannot be null");
        this.mappers = Objects.requireNonNull(mappers, "mappers cannot be null");
        this.clazz = null;
        if (mappers.isEmpty()) {
            throw new IllegalArgumentException("mappers cannot be empty");
        }
    }

    @Override
    public HttpResponse.BodySubscriber<T> apply(HttpResponse.ResponseInfo responseInfo) {
        var contentType0 = responseInfo.headers().firstValue("Content-Type")
            .orElseThrow(() -> new IllegalStateException("Missing Content-Type"));
        var matchingResponse = mappers.entrySet().stream()
            .filter(entry -> entry.getKey().matcher(contentType0).matches())
            .findFirst();
        if (matchingResponse.isEmpty()) {
            throw new IllegalStateException("Content type is not known. It is " + contentType0 + "!");
        }

        var mapper = matchingResponse.get().getValue();

        if (typeReference == null) {
            return new JacksonBodySubscriber<>(clazz, mapper);
        } else  {
            return new JacksonBodySubscriber<>(typeReference, mapper);
        }
    }
}
