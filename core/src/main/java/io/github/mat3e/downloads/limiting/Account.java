package io.github.mat3e.downloads.limiting;

import io.github.mat3e.downloads.limiting.LimitingFacade.AccountLimitExceeded;
import io.github.mat3e.downloads.limiting.api.AccountId;
import io.github.mat3e.downloads.limiting.api.Asset;
import io.github.mat3e.downloads.limiting.event.SuspiciousLimitingEvent;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toUnmodifiableList;

@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table("downloading_accounts")
class Account {

    @Id
    @EqualsAndHashCode.Include
    private final String id;
    private final List<DownloadedAsset> assets;
    @Column("limitation")
    private final Integer limit;
    @Version
    private final Integer version;

    AccountId id() {
        return AccountId.valueOf(id);
    }

    List<Asset> assets() {
        return assets.stream()
                .map(downloadedAsset -> Asset.withId(downloadedAsset.assetId).inCountry(downloadedAsset.countryCode))
                .collect(toUnmodifiableList());
    }

    List<SuspiciousLimitingEvent> assignAsset(Asset asset, Clock clock) {
        if (alreadyHas(asset)) {
            return List.of(SuspiciousLimitingEvent.assetDuplicated(clock, id(), asset));
        }
        if (assets.size() >= limit) {
            throw new AccountLimitExceeded(limit);
        }
        var events = new ArrayList<SuspiciousLimitingEvent>();
        findSimilarTo(asset).forEach(downloadedAsset -> events.add(SuspiciousLimitingEvent.assetAlreadyInDifferentCountry(
                clock,
                id(),
                asset,
                downloadedAsset.countryCode)));
        assets.add(DownloadedAsset.newFrom(asset));
        return events;
    }

    private Stream<DownloadedAsset> findSimilarTo(Asset asset) {
        return assets.stream().filter(existingAsset -> existingAsset.hasSameIdAndDifferentCountryAs(asset));
    }

    private boolean alreadyHas(Asset asset) {
        return assets.stream().anyMatch(existingAsset -> existingAsset.hasSameIdAndCountryAs(asset));
    }

    List<SuspiciousLimitingEvent> unassignAsset(Asset downloadedAsset, Clock clock) {
        boolean anyRemoved = assets.removeIf(existingAsset -> existingAsset.hasSameIdAndCountryAs(downloadedAsset));
        if (anyRemoved) {
            return emptyList();
        }
        return List.of(SuspiciousLimitingEvent.unnecessaryRemoval(clock, id(), downloadedAsset));
    }

    @RequiredArgsConstructor
    @Table("downloaded_assets")
    private static class DownloadedAsset {
        static DownloadedAsset newFrom(Asset asset) {
            return new DownloadedAsset(null, asset.getId(), asset.getCountryCode(), null);
        }

        @Id
        private final Integer id;
        private final String assetId;
        private final String countryCode;
        @Version
        private final Integer version;

        boolean hasSameIdAndDifferentCountryAs(Asset asset) {
            return hasSameIdAs(asset) && !countryCode.equals(asset.getCountryCode());
        }

        boolean hasSameIdAndCountryAs(Asset asset) {
            return hasSameIdAs(asset) && countryCode.equals(asset.getCountryCode());
        }

        private boolean hasSameIdAs(Asset asset) {
            return assetId.equals(asset.getId());
        }
    }
}
