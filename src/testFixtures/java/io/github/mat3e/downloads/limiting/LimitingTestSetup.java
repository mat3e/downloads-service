package io.github.mat3e.downloads.limiting;

import io.github.mat3e.downloads.reporting.ReportingFacade;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

class LimitingTestSetup {
    private final LimitingConfiguration creator;

    LimitingTestSetup(ReportingFacade reportingFacade) {
        this(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), reportingFacade);
    }

    LimitingTestSetup(Clock clock, ReportingFacade reportingFacade) {
        var accountRepository = new InMemoryAccountRepository();
        var settingsRepository = new InMemoryAccountSettingRepository(clock, accountRepository);
        creator = new LimitingConfiguration(clock, accountRepository, settingsRepository, reportingFacade);
    }

    LimitingFacade facade() {
        return creator.facade();
    }
}
