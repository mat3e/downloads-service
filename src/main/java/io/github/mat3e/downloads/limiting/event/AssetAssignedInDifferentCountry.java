package io.github.mat3e.downloads.limiting.event;

import io.github.mat3e.downloads.limiting.api.AccountId;
import io.github.mat3e.downloads.limiting.api.Asset;

import java.time.Clock;

class AssetAssignedInDifferentCountry extends SuspiciousLimitingEvent {
    private final Asset asset;
    private final String existingAssetCountry;

    AssetAssignedInDifferentCountry(
            Clock occurrenceClock,
            AccountId accountId,
            Asset newAsset,
            String existingAssetCountry) {
        super(occurrenceClock, accountId);
        this.asset = newAsset;
        this.existingAssetCountry = existingAssetCountry;
    }

    @Override
    String description() {
        return "assigned " + asset + " while it was already assigned in " + existingAssetCountry;
    }
}
