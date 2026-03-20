package eu.fabianfranz.jackson.net.http;

import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpResponse;

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

    private final Class<T> clazz;
    private final ObjectMapper mapper;
    private final String contentType;

    /**
     * Pass the class to deserialize to get a typed response.
     * This constructor will work with a default object mapper.
     *
     * This constructor is meant to be used for the very simple cases of using this client
     * @param clazz the class to deserialize
     */
    public JSONBodyHandler(Class<T> clazz) {
        this(clazz, new ObjectMapper());
    }

    /**
     * This constructor lets you pass in a custom object mapper. This will then be used for deserialization of the object.
     * Use this constructor, if you need customized behavior of the deserialization process.
     * This constructor assumes that you deserialize JSON.
     * If you want to deserialize another format, pass the content type additionally.
     * @param clazz the class to map
     * @param mapper the custom object mapper
     */
    public JSONBodyHandler(Class<T> clazz, ObjectMapper mapper) {
        this(clazz, mapper, "application/json");
    }

    /**
     * This constructor lets you pass in a custom object mapper. This will then be used for deserialization of the object.
     * Use this constructor, if you need customized behavior of the deserialization process.
     * @param clazz the class to map
     * @param mapper the custom object mapper
     * @param contentType the mime type to check for
     */
    public JSONBodyHandler(Class<T> clazz, ObjectMapper mapper, String contentType) {
        this.clazz = clazz;
        this.mapper = mapper;
        this.contentType = contentType;
    }

    @Override
    public HttpResponse.BodySubscriber<T> apply(HttpResponse.ResponseInfo responseInfo) {
        var contentType0 = responseInfo.headers().firstValue("Content-Type")
            .orElseThrow(() -> new IllegalStateException("Missing Content-Type"));
        if (!contentType0.contains(contentType)) {
            throw new IllegalStateException("Content is not JSON. It is " + contentType);
        }

        return new JSONBodySubscriber<>(clazz, mapper);
    }
}
