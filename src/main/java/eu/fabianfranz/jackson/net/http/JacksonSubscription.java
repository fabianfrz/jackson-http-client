package eu.fabianfranz.jackson.net.http;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Flow;

/**
 * Worker class that does the actual transmission.
 */
public class JacksonSubscription implements Flow.Subscription {
    final ByteArrayInputStream bais;
    private final Flow.Subscriber<? super ByteBuffer> subscriber;
    private final long contentLength;
    private boolean cancelled;

    public JacksonSubscription(Flow.Subscriber<? super ByteBuffer> subscriber, byte[] bytes) {
        this.subscriber = subscriber;
        bais = new ByteArrayInputStream(bytes);
        contentLength = bytes.length;
    }

    @Override
    public void request(long n) {
        try {
            long count = 0;
            while (true) {
                if (cancelled) {
                    subscriber.onError(new CancellationException("Subscription has been cancelled"));
                    return;
                }
                var bytesLeft = contentLength - count;
                if (bytesLeft <= 0) { // we have all bytes already
                    subscriber.onComplete();
                    break;
                }
                var bytes = bais.readNBytes((int)Math.min(4096, bytesLeft));
                count += bytes.length;
                if (bytes.length > 0) {
                    subscriber.onNext(ByteBuffer.wrap(bytes));
                } else  {
                    subscriber.onComplete();
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("IO exception during reading the request into a buffer", e);
        }
    }

    @Override
    public void cancel() {
        cancelled = true;
    }
}
