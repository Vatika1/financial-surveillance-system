package com.financialsurveillance.casemanagement.producer;

import com.financialsurveillance.events.CaseClosedEvent;
import com.financialsurveillance.events.CaseCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class CaseEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.cases-created}")
    private String topicCaseCreated;

    @Value("${kafka.topics.cases-closed}")
    private String topicCaseClosed;

    private static final Logger log = LoggerFactory.getLogger(CaseEventProducer.class);

    public void publishCaseCreated(CaseCreatedEvent event){
        String key = event.getAdvisorId();

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topicCaseCreated, key, event);

        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error(
                        "Failed to publish CaseCreatedEvent. caseId={}, alertId={}, advisorId={}, topic={}",
                        event.getCaseId(),
                        event.getAlertId(),
                        event.getAdvisorId(),
                        topicCaseCreated,
                        throwable
                );
                return;
            }
            RecordMetadata metadata = result.getRecordMetadata();
            log.info(
                    "Published CaseCreatedEvent successfully. caseId={}, alertId={}, advisorId={}, topicCaseCreated={}, partition={}, offset={}",
                    event.getCaseId(),
                    event.getAlertId(),
                    event.getAdvisorId(),
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset()
            );

        });
    }

    public void publishCaseClosed(CaseClosedEvent event){
        String key = event.getAdvisorId();

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topicCaseClosed, key, event);

        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error(
                        "Failed to publish CaseClosedEvent. caseId={}, alertId={}, advisorId={}, topicCaseClosed={}",
                        event.getCaseId(),
                        event.getAlertId(),
                        event.getAdvisorId(),
                        topicCaseClosed,
                        throwable
                );
                return;
            }
            RecordMetadata metadata = result.getRecordMetadata();
            log.info(
                    "Published CaseClosedEvent successfully. caseId={}, alertId={}, advisorId={}, topicCaseClosed={}, partition={}, offset={}",
                    event.getCaseId(),
                    event.getAlertId(),
                    event.getAdvisorId(),
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset()
            );

        });
    }
}
