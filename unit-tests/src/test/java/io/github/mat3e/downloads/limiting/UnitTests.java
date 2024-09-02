package io.github.mat3e.downloads.limiting;

import io.github.mat3e.downloads.limiting.api.AccountId;
import io.github.mat3e.downloads.reporting.ReportingFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UnitTests {
    @Nested
    class LimitingFacadeTest {
        private final Clock clock = mock(Clock.class);
        private final AccountRepository accountRepository = mock(AccountRepository.class);
        private final AccountSettingRepository accountSettingRepository = mock(AccountSettingRepository.class);
        private final ReportingFacade reporting = mock(ReportingFacade.class);

        private LimitingFacade systemUnderTest;

        @BeforeEach
        void setUp() {
            systemUnderTest = new LimitingFacade(clock, accountRepository, accountSettingRepository, reporting);
        }

        @Test
        void newAccount_overrideLimit_createsAccount() {
            // given
            var accountId = AccountId.valueOf("1");
            when(accountSettingRepository.findById(accountId)).thenReturn(Optional.empty());

            // when
            systemUnderTest.overrideAccountLimit(accountId, 1);

            // then
            var accountSettingCaptor = ArgumentCaptor.forClass(AccountSetting.class);
            verify(accountSettingRepository).save(accountSettingCaptor.capture());
            assertThat(accountSettingCaptor.getValue().id()).isEqualTo(accountId);
            assertThat(accountSettingCaptor.getValue().limit()).isEqualTo(1);
        }

        @Test
        void existingAccount_overrideLimit_updatesAccount() {
            // given
            var accountId = AccountId.valueOf("1");
            var existingAccount = mock(AccountSetting.class);
            when(accountSettingRepository.findById(accountId)).thenReturn(Optional.of(existingAccount));

            // when
            systemUnderTest.overrideAccountLimit(accountId, 1);

            // then
            verify(existingAccount).overrideLimit(1);
            verify(accountSettingRepository).save(existingAccount);
        }
    }

    @SuppressWarnings("AccessStaticViaInstance") // "and" is AssertJ way to avoid 2 "then" methods
    @Nested
    @ExtendWith(MockitoExtension.class)
    class MockitoExtensionLimitingFacadeTest {
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

            then(accountSettingRepository).should().save(accountSettingCaptor.capture());
            var account = accountSettingCaptor.getValue();
            and.then(account.id()).isEqualTo(accountId);
            and.then(account.limit()).isEqualTo(1);
        }

        @Test
        void existingAccount_overrideLimit_updatesAccount() {
            var accountId = AccountId.valueOf("1");
            var existingAccount = new AccountSetting(accountId.getId(), 2, 1);
            given(accountSettingRepository.findById(accountId)).willReturn(Optional.of(existingAccount));

            // when
            systemUnderTest.overrideAccountLimit(accountId, 1);

            then(accountSettingRepository).should().save(existingAccount);
            and.then(existingAccount.limit()).isEqualTo(1);
        }
    }
}
