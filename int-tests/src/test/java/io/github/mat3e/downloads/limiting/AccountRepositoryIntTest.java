package io.github.mat3e.downloads.limiting;

import io.github.mat3e.downloads.limiting.api.Asset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;

@IntegrationTest
class AccountRepositoryIntTest {
    @Autowired
    private AccountRepository systemUnderTest;

    @Autowired
    private JdbcTemplate jdbc;

    private final Clock clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);

    @Test
    void accountLifecycle() {
        // given
        var account = new Account("test", emptyList(), 1, null);

        // when
        systemUnderTest.save(account);

        then(systemUnderTest.findById(account.id())).hasValueSatisfying(insertedAccount -> {
            then(insertedAccount.assets()).isEmpty();
            then(insertedAccount).extracting("version").isEqualTo(0);
        });

        // when
        systemUnderTest.findById("test").ifPresent(existingAccount -> {
            existingAccount.assignAsset(Asset.withId("asset").inCountry("US"), clock);
            systemUnderTest.save(existingAccount);
        });

        then(systemUnderTest.findById(account.id())).hasValueSatisfying(updatedAccount -> {
            then(updatedAccount.assets()).hasSize(1);
            then(updatedAccount).extracting("version").isEqualTo(1);
        });
    }

    @Test
    void accountDbConstraints() {
        // given
        var account = new Account("test", new ArrayList<>(), 10, null);
        // and
        account.assignAsset(Asset.withId("test-asset").inCountry("US"), clock);

        // when
        systemUnderTest.save(account);

        then(systemUnderTest.findById(account.id())).hasValueSatisfying(insertedAccount -> then(insertedAccount.assets()).hasSize(
                1));

        thenExceptionOfType(DuplicateKeyException.class).isThrownBy(() -> jdbc.update(
                "INSERT INTO downloaded_assets (asset_id, country_code, account, downloading_accounts_key) VALUES (?, ?, ?, ?)",
                "test-asset",
                "US",
                "test",
                1
        )).withMessageContaining("ASSET_IN_COUNTRY");
    }
}
