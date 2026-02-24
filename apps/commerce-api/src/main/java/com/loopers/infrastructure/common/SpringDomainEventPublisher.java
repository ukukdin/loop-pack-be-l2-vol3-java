package com.loopers.infrastructure.common;

import com.loopers.domain.model.common.AggregateRoot;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringDomainEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public SpringDomainEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishEvents(AggregateRoot aggregateRoot) {
        aggregateRoot.getDomainEvents().forEach(eventPublisher::publishEvent);
        aggregateRoot.clearDomainEvents();
    }
}
