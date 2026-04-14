package com.financialsurveillance.casemanagement.producer;

import com.financialsurveillance.events.CaseClosedEvent;
import com.financialsurveillance.events.CaseCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

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

    }

    public void publishCaseClosed(CaseClosedEvent event){

    }
}
