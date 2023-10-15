package io.github.mat3e.downloads.limiting;

import io.github.mat3e.downloads.exceptionhandling.BusinessException;
import io.github.mat3e.downloads.limiting.LimitingFacade.AccountLimitExceeded;
import io.github.mat3e.downloads.limiting.api.AccountId;
import io.github.mat3e.downloads.limiting.api.Asset;
import io.github.mat3e.downloads.reporting.CapturingReportingFacade;
import org.junit.jupiter.api.Test;

import static io.github.mat3e.downloads.limiting.BusinessAssertions.then;
import static io.github.mat3e.downloads.limiting.BusinessAssertions.thenFoundIn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchException;
import static org.assertj.core.api.Assertions.tuple;

class LimitingTest {
    private static final AccountId ACCOUNT_ID = AccountId.valueOf("1");

    private final CapturingReportingFacade reporting = new CapturingReportingFacade();
    private final LimitingTestSetup setup = new LimitingTestSetup(reporting);
    private final LimitingFacade limiting = setup.facade();

    @Test
    void findAccount_unknownAccount_returnsEmpty() {
        assertThat(limiting.findForAccount(ACCOUNT_ID)).isEmpty();
    }

    @Test
    void downloadStarted_notExistingAccount_throwsNotFound() {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> limiting.assignDownloadedAsset(ACCOUNT_ID, Asset.withId("123").inCountry("US")))
                .withMessageContaining("not found")
                .withMessageContaining(ACCOUNT_ID.getId());
    }

    @Test
    void assetRemoved_notExistingAccount_throwsNotFound() {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> limiting.removeDownloadedAsset(ACCOUNT_ID, Asset.withId("123").inCountry("US")))
                .withMessageContaining("not found")
                .withMessageContaining(ACCOUNT_ID.getId());
    }

    @Test
    void overrideLimit_illegalValue_throws() {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> limiting.overrideAccountLimit(ACCOUNT_ID, -1))
                .withMessageContaining("negative");
    }

    @Test
    void downloadStarted_limitNotExceeded_storesAsset() {
        // given
        limiting.overrideAccountLimit(ACCOUNT_ID, 1);

        // when
        limiting.assignDownloadedAsset(ACCOUNT_ID, Asset.withId("123").inCountry("US"));

        thenFoundIn(limiting, ACCOUNT_ID).containsExactly(Asset.withId("123").inCountry("US"));
    }

    @Test
    void downloadStarted_limitExceeded_doesNotStoreAsset() {
        // given
        limiting.overrideAccountLimit(ACCOUNT_ID, 1);
        // and
        limiting.assignDownloadedAsset(ACCOUNT_ID, Asset.withId("123").inCountry("US"));

        // when
        var exception =
                catchException(() -> limiting.assignDownloadedAsset(ACCOUNT_ID, Asset.withId("456").inCountry("US")));

        then(exception).isInstanceOf(AccountLimitExceeded.class);
        thenFoundIn(limiting, ACCOUNT_ID).containsExactly(Asset.withId("123").inCountry("US"));
    }

    @Test
    void downloadStarted_sameAsset_doesNotStoreAsset() {
        // given
        limiting.overrideAccountLimit(ACCOUNT_ID, 2);
        // and
        limiting.assignDownloadedAsset(ACCOUNT_ID, Asset.withId("123").inCountry("US"));

        // when
        limiting.assignDownloadedAsset(ACCOUNT_ID, Asset.withId("123").inCountry("US"));

        thenFoundIn(limiting, ACCOUNT_ID).containsExactly(Asset.withId("123").inCountry("US"));
        reporting.recordedEvents()
                .extracting("accountId", "asset")
                .containsExactly(tuple(ACCOUNT_ID, Asset.withId("123").inCountry("US")));
    }

    @Test
    void downloadStarted_sameAssetDifferentCountry_storesAsset() {
        // given
        limiting.overrideAccountLimit(ACCOUNT_ID, 2);
        // and
        limiting.assignDownloadedAsset(ACCOUNT_ID, Asset.withId("123").inCountry("DE"));

        // when
        limiting.assignDownloadedAsset(ACCOUNT_ID, Asset.withId("123").inCountry("FR"));

        thenFoundIn(limiting, ACCOUNT_ID).containsExactly(
                Asset.withId("123").inCountry("DE"),
                Asset.withId("123").inCountry("FR"));
        reporting.recordedEvents()
                .extracting("accountId", "asset", "existingAssetCountry")
                .containsExactly((tuple(ACCOUNT_ID, Asset.withId("123").inCountry("FR"), "DE")));
    }

    @Test
    void assetRemoved_noAsset_ignores() {
        // given
        limiting.overrideAccountLimit(ACCOUNT_ID, 2);

        // when
        limiting.removeDownloadedAsset(ACCOUNT_ID, Asset.withId("123").inCountry("US"));

        thenFoundIn(limiting, ACCOUNT_ID).isEmpty();
        reporting.recordedEvents()
                .extracting("accountId", "asset")
                .containsExactly(tuple(ACCOUNT_ID, Asset.withId("123").inCountry("US")));
    }

    @Test
    void assetRemoved_newDownloadStarted_storesAsset() {
        // given
        limiting.overrideAccountLimit(ACCOUNT_ID, 2);
        // and
        limiting.assignDownloadedAsset(ACCOUNT_ID, Asset.withId("123").inCountry("DE"));
        // and
        limiting.assignDownloadedAsset(ACCOUNT_ID, Asset.withId("123").inCountry("FR"));

        // when
        limiting.removeDownloadedAsset(ACCOUNT_ID, Asset.withId("123").inCountry("FR"));
        // and
        limiting.assignDownloadedAsset(ACCOUNT_ID, Asset.withId("456").inCountry("DE"));

        thenFoundIn(limiting, ACCOUNT_ID).containsExactly(
                Asset.withId("123").inCountry("DE"),
                Asset.withId("456").inCountry("DE"));
    }

    @Test
    void downloadStarted_limitIncreased_storesAsset() {
        // given
        limiting.overrideAccountLimit(ACCOUNT_ID, 1);
        // and
        limiting.assignDownloadedAsset(ACCOUNT_ID, Asset.withId("123").inCountry("DE"));

        // when
        var exception =
                catchException(() -> limiting.assignDownloadedAsset(ACCOUNT_ID, Asset.withId("456").inCountry("DE")));

        then(exception).isInstanceOf(AccountLimitExceeded.class);
        thenFoundIn(limiting, ACCOUNT_ID).containsExactly(Asset.withId("123").inCountry("DE"));

        // when
        limiting.overrideAccountLimit(ACCOUNT_ID, 2);
        // and
        limiting.assignDownloadedAsset(ACCOUNT_ID, Asset.withId("456").inCountry("DE"));

        thenFoundIn(limiting, ACCOUNT_ID).containsExactly(
                Asset.withId("123").inCountry("DE"),
                Asset.withId("456").inCountry("DE"));
    }
}
