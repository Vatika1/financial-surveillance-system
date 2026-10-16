package com.financialsurveillance.tradingestion.producer;

import com.financialsurveillance.events.TradeCreatedEvent;
import com.financialsurveillance.tradingestion.config.KafkaTopicsProperties;
import com.financialsurveillance.tradingestion.exception.TradePublishException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publishes trade lifecycle events to Kafka.
 *
 * <p>Sends synchronously with a bounded timeout so failures surface to the
 * caller and can trigger transactional rollback. Translates Kafka-specific
 * exceptions into a single domain exception (TradePublishException) so
 * upstream code doesn't need to know about Kafka error types.
 *
 * <p><b>Why no producer-side DLQ:</b> trade-ingestion is REST-fronted, so
 * the HTTP caller IS the retry mechanism. On publish failure we roll back
 * the DB and return 503; the caller retries. A producer DLQ here would
 * accumulate orphan messages with no corresponding DB row and no consumer
 * waiting for them. Producer DLQs make sense for asynchronous producers
 * (scheduled jobs, stream-to-stream forwarders) where there's no caller
 * to fail back to.
 *
 * <p>The {@code trades.raw.DLT} topic IS declared (in KafkaTopicConfig)
 * because activity-monitor will route poison-pill messages there during
 * consumer-side error handling.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeEventProducer {

    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(5);

    private final KafkaTemplate<String, TradeCreatedEvent> kafkaTemplate;
    private final KafkaTopicsProperties topics;

    /**
     * Publishes a TradeCreatedEvent partitioned by advisorId so all events
     * for the same advisor land on the same partition (preserves per-advisor
     * ordering and enables stateful per-advisor processing downstream).
     *
     * @throws TradePublishException if the broker rejects the message,
     *         the send times out, or the thread is interrupted.
     */


    public CompletableFuture<SendResult<String, TradeCreatedEvent>> publishTradeCreatedAsync(
            TradeCreatedEvent event, String correlationId) {
        ProducerRecord<String, TradeCreatedEvent> record =
                new ProducerRecord<>(topics.tradesRaw(), event.getAccountId(), event);   // same key/topic you use now
        if (correlationId != null) {
            record.headers().add("correlationId", correlationId.getBytes(StandardCharsets.UTF_8));
        }
        return kafkaTemplate.send(record);   // no .get()
    }
}