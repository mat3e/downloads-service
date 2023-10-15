package io.github.mat3e.downloads.limiting;

import io.github.mat3e.downloads.limiting.api.AccountId;
import io.github.mat3e.downloads.reporting.ReportingFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.Optional;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LimitingFacadeBetterTest {
    @Mock
    private Clock clock;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountSettingRepository accountSettingRepository;
    @Mock
    private ReportingFacade reporting;

    @InjectMocks
    private LimitingFacade systemUnderTest;

    @Captor
    private ArgumentCaptor<AccountSetting> accountSettingCaptor;

    @Test
    void newAccount_overrideLimit_createsAccount() {
        var accountId = AccountId.valueOf("1");
        given(accountSettingRepository.findById(accountId)).willReturn(Optional.empty());

        // when
        systemUnderTest.overrideAccountLimit(accountId, 1);

        BDDMockito.then(accountSettingRepository).should().save(accountSettingCaptor.capture());
        var account = accountSettingCaptor.getValue();
        then(account.id()).isEqualTo(accountId);
        then(account.limit()).isEqualTo(1);
    }

    @Test
    void existingAccount_overrideLimit_updatesAccount() {
        var accountId = AccountId.valueOf("1");
        var existingAccount = new AccountSetting(accountId.getId(), 2, 1);
        given(accountSettingRepository.findById(accountId)).willReturn(Optional.of(existingAccount));

        // when
        systemUnderTest.overrideAccountLimit(accountId, 1);

        then(existingAccount.limit()).isEqualTo(1);
        BDDMockito.then(accountSettingRepository).should().save(existingAccount);
    }
}
