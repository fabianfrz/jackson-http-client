package eu.fabianfranz.jackson.net.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.Flow;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class JacksonSubscriptionTest {
    @Mock
    Flow.Subscriber<ByteBuffer> subscriber;

    @Test
    void request() {
        byte[] bytes = new ObjectMapper().writeValueAsBytes(
            Map.of("key", "value", "key2", "value2".repeat(200))
        );
        JacksonSubscription jacksonSubscription = new JacksonSubscription(subscriber, bytes);
        jacksonSubscription.request(Long.MAX_VALUE);
        verify(subscriber).onComplete();
    }

    @Test
    void cancel() {
        byte[] bytes = new ObjectMapper().writeValueAsBytes(
                Map.of("key", "value", "key2", "value2".repeat(2000))
        );
        JacksonSubscription jacksonSubscription = new JacksonSubscription(subscriber, bytes);
        jacksonSubscription.request(100);
        jacksonSubscription.cancel();
        jacksonSubscription.request(Long.MAX_VALUE);
        verify(subscriber).onError(any());
    }
}