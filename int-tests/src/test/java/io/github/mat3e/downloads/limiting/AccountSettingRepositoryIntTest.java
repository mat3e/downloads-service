package io.github.mat3e.downloads.limiting;

import io.github.mat3e.downloads.limiting.api.AccountId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.BDDAssertions.then;

@IntegrationTest
class AccountSettingRepositoryIntTest {
    @Autowired
    private AccountSettingRepository systemUnderTest;

    @Test
    void accountSettingLifecycle() {
        // given
        var accountSetting = AccountSetting.newFor(AccountId.valueOf("test"), 1);

        // when
        systemUnderTest.save(accountSetting);

        then(systemUnderTest.findById(accountSetting.id())).hasValueSatisfying(insertedAccount -> {
            then(insertedAccount.limit()).isEqualTo(1);
            then(insertedAccount).extracting("version").isEqualTo(0);
        });

        // when
        systemUnderTest.findById("test").ifPresent(existingAccount -> {
            existingAccount.overrideLimit(2);
            systemUnderTest.save(existingAccount);
        });

        then(systemUnderTest.findById(accountSetting.id())).hasValueSatisfying(updatedAccount -> {
            then(updatedAccount.limit()).isEqualTo(2);
            then(updatedAccount).extracting("version").isEqualTo(1);
        });
    }
}
