package com.financialsurveillance.tradingestion.exception;

/**
 * Thrown when a Trade was persisted but the corresponding TradeCreatedEvent
 * could not be published to Kafka. Triggers @Transactional rollback so the
 * DB doesn't end up with rows whose downstream events were never sent.
 *
 * RuntimeException by design — Spring's @Transactional rolls back unchecked
 * exceptions automatically. Making this checked would require
 * @Transactional(rollbackFor = ...) and fights the framework idiom.
 */
public class TradePublishException extends RuntimeException {
    public TradePublishException(String message, Throwable cause) {
        super(message, cause);
    }
}