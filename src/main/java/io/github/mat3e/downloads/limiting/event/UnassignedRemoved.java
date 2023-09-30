package io.github.mat3e.downloads.limiting.event;

import io.github.mat3e.downloads.limiting.api.AccountId;
import io.github.mat3e.downloads.limiting.api.Asset;

import java.time.Clock;

class UnassignedRemoved extends SuspiciousLimitingEvent {
    private final Asset asset;

    UnassignedRemoved(Clock occurrenceClock, AccountId accountId, Asset asset) {
        super(occurrenceClock, accountId);
        this.asset = asset;
    }

    @Override
    String description() {
        return "removed unassigned asset: " + asset;
    }
}
