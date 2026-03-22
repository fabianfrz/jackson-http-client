package eu.fabianfranz.jackson.net.http;

import tools.jackson.core.type.TypeReference;
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
public class JacksonBodyHandler<T> implements HttpResponse.BodyHandler<T> {

    private final Class<T> clazz;
    private final ObjectMapper mapper;
    private final String contentType;
    private final TypeReference<T> typeReference;

    /**
     * Pass the class to deserialize to get a typed response.
     * This constructor will work with a default object mapper.
     *
     * This constructor is meant to be used for the very simple cases of using this client
     * @param clazz the class to deserialize
     */
    public JacksonBodyHandler(Class<T> clazz) {
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
    public JacksonBodyHandler(Class<T> clazz, ObjectMapper mapper) {
        this(clazz, mapper, "application/json");
    }

    /**
     * This constructor lets you pass in a custom object mapper. This will then be used for deserialization of the object.
     * Use this constructor, if you need customized behavior of the deserialization process.
     * @param clazz the class to map
     * @param mapper the custom object mapper
     * @param contentType the mime type to check for
     */
    public JacksonBodyHandler(Class<T> clazz, ObjectMapper mapper, String contentType) {
        this.clazz = clazz;
        this.mapper = mapper;
        this.contentType = contentType;
        this.typeReference = null;
    }


    /**
     * Pass the type to deserialize to get a typed response.
     * This constructor will work with a default object mapper.
     *
     * This constructor is meant to be used for the very simple cases of using this client
     * @param typeReference the type to deserialize
     */
    public JacksonBodyHandler(TypeReference<T> typeReference) {
        this(typeReference, new ObjectMapper());
    }

    /**
     * This constructor lets you pass in a custom object mapper. This will then be used for deserialization of the object.
     * Use this constructor, if you need customized behavior of the deserialization process.
     * This constructor assumes that you deserialize JSON.
     * If you want to deserialize another format, pass the content type additionally.
     * @param clazz the class to map
     * @param mapper the custom object mapper
     */
    public JacksonBodyHandler(TypeReference<T> clazz, ObjectMapper mapper) {
        this(clazz, mapper, "application/json");
    }

    /**
     * This constructor allows full customization and can be used to wrap for other types such as YAML.
     * @param typeReference the class to map
     * @param mapper the custom object mapper
     * @param contentType the mime type to check for
     */
    public JacksonBodyHandler(TypeReference<T> typeReference, ObjectMapper mapper, String contentType) {
        this.clazz = null;
        this.mapper = mapper;
        this.contentType = contentType;
        this.typeReference = typeReference;
    }

    @Override
    public HttpResponse.BodySubscriber<T> apply(HttpResponse.ResponseInfo responseInfo) {
        var contentType0 = responseInfo.headers().firstValue("Content-Type")
            .orElseThrow(() -> new IllegalStateException("Missing Content-Type"));
        if (!contentType0.contains(contentType)) {
            throw new IllegalStateException("Content is not JSON. It is " + contentType);
        }
        if (typeReference == null) {
            return new JacksonBodySubscriber<>(clazz, mapper);
        } else {
            return new JacksonBodySubscriber<>(typeReference, mapper);
        }
    }
}
