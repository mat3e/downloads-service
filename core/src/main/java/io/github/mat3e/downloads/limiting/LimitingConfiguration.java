package io.github.mat3e.downloads.limiting;

import io.github.mat3e.downloads.reporting.ReportingFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@RequiredArgsConstructor
class LimitingConfiguration {
    /* just IO dependencies and other modules */
    private final Clock clock;
    private final AccountRepository accountRepository;
    private final AccountSettingRepository accountSettingRepository;
    private final ReportingFacade reportingFacade;

    @Bean
    LimitingFacade facade() {
        return new LimitingFacade(clock, accountRepository, accountSettingRepository, reportingFacade);
    }
}
