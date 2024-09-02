package io.github.mat3e.downloads.limiting.event.out;

import io.github.mat3e.downloads.limiting.api.AccountId;
import io.github.mat3e.downloads.limiting.api.Asset;

import java.time.Clock;

class AssetDuplicated extends SuspiciousLimitingEvent {
    private final Asset asset;

    AssetDuplicated(Clock occurrenceClock, AccountId accountId, Asset asset) {
        super(occurrenceClock, accountId);
        this.asset = asset;
    }

    public Asset asset() {
        return asset;
    }

    @Override
    String description() {
        return "assigned already assigned asset: " + asset;
    }
}
