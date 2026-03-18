package eu.fabianfranz.jackson.net.http;


import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.concurrent.Flow;

/**
 * A {@link java.net.http.HttpRequest.BodyPublisher} implementation that can decode JSON DTOs directly.
 * It converts the object to JSON and then slices it into 4k blocks, which are sent to the HTTP client implementation.
 * @param <T>
 */
public class JSONBodyPublisher<T> implements HttpRequest.BodyPublisher {
    byte[] bytes;

    public JSONBodyPublisher(T data) {
        ObjectMapper mapper = new ObjectMapper();
        bytes = mapper.writeValueAsBytes(data);
    }
    @Override
    public long contentLength() {
        return bytes.length;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
        subscriber.onSubscribe(new JSONSubscription(subscriber));

    }

    /**
     * Worker class that does the actual transmission.
     */
    private class JSONSubscription implements Flow.Subscription {
        final ByteArrayInputStream bais;
        private final Flow.Subscriber<? super ByteBuffer> subscriber;

        public JSONSubscription(Flow.Subscriber<? super ByteBuffer> subscriber) {
            this.subscriber = subscriber;
            bais = new ByteArrayInputStream(bytes);
        }

        @Override
        public void request(long n) {
            try {
                long count = 0;
                do {
                    var bytesLeft = contentLength() - count;
                    if (bytesLeft <= 0) {
                        break;
                    }
                    bytes = bais.readNBytes((int)Math.min(4096, bytesLeft));
                    count += bytes.length;
                    if (bytes.length > 0) {
                        subscriber.onNext(ByteBuffer.wrap(bytes));
                    } else  {
                        subscriber.onComplete();
                        break;
                    }
                } while (bytes.length != 0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void cancel() {
            bytes = null;
        }
    }
}
