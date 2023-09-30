package io.github.mat3e.downloads.reporting;

import io.github.mat3e.downloads.eventhandling.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

public interface ReportingFacade {
    void recordEvent(DomainEvent event);
}

@Slf4j
@Service
class LoggingReportingFacade implements ReportingFacade {
    @Override
    public void recordEvent(DomainEvent event) {
        if (event.suspicious()) {
            log.warn("Suspicious event recorded: " + event);
            return;
        }
        log.info("Event recorded: " + event);
    }
}
