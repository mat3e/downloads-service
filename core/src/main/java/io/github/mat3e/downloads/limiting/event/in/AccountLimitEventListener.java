package io.github.mat3e.downloads.limiting.event.in;

import io.github.mat3e.downloads.limiting.LimitingFacade;
import io.github.mat3e.downloads.limiting.api.AccountId;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty("spring.kafka.consumer.group-id")
class AccountLimitEventListener {
    static final String TOPIC = "limit-changes";

    private final LimitingFacade facade;

    @KafkaListener(topics = TOPIC)
    public void execute(@Payload Message message) {
        facade.overrideAccountLimit(message.accountId, message.limit);
    }

    record Message(AccountId accountId, int limit) {
        Message(String accountId, int limit) {
            this(AccountId.valueOf(accountId), limit);
        }
    }
}
