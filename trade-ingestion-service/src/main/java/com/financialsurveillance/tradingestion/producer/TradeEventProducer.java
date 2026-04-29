package com.financialsurveillance.tradingestion.producer;

import com.financialsurveillance.events.TradeCreatedEvent;
import com.financialsurveillance.tradingestion.config.KafkaTopicsProperties;
import com.financialsurveillance.tradingestion.exception.TradePublishException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Duration;
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
    public void publishTradeCreated(TradeCreatedEvent event) {
        String key = event.getAdvisorId();
        String tradeId = event.getTradeId();

        try {
            SendResult<String, TradeCreatedEvent> result = kafkaTemplate
                    .send(topics.tradesRaw(), key, event)
                    .get(SEND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            log.info("Event published tradeId={} advisorId={} topic={} partition={} offset={}",
                    tradeId,
                    key,
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

        } catch (TimeoutException e) {
            log.error("Kafka send timed out tradeId={} advisorId={} timeoutMs={}",
                    tradeId, key, SEND_TIMEOUT.toMillis(), e);
            throw new TradePublishException(
                    "Kafka publish timed out for tradeId=" + tradeId, e);

        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("Kafka send failed tradeId={} advisorId={} cause={}",
                    tradeId, key, cause.getMessage(), cause);
            throw new TradePublishException(
                    "Kafka publish failed for tradeId=" + tradeId, cause);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while publishing tradeId={} advisorId={}",
                    tradeId, key, e);
            throw new TradePublishException(
                    "Interrupted while publishing tradeId=" + tradeId, e);
        }
    }
}