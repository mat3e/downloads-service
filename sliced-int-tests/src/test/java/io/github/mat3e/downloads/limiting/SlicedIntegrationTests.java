package io.github.mat3e.downloads.limiting;

import io.github.mat3e.downloads.exceptionhandling.BusinessException;
import io.github.mat3e.downloads.limiting.api.AccountId;
import io.github.mat3e.downloads.limiting.api.Asset;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.zalando.problem.spring.web.autoconfigure.ProblemAutoConfiguration;
import org.zalando.problem.spring.web.autoconfigure.ProblemJacksonWebMvcAutoConfiguration;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SlicedIntegrationTests {
    @Nested
    @WebMvcTest
    @ImportAutoConfiguration({ProblemAutoConfiguration.class, ProblemJacksonWebMvcAutoConfiguration.class})
    class LimitingControllerTest {
        private static final String ACCOUNT_ID = "1";

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        LimitingFacade facadeNeededByController;

        @Test
        void illegalParams_returnsBadRequest() throws Exception {
            // expect 400
            httpPostAssetForAccount(
                    "  ",
                    "{ \"id\": \"123\", \"countryCode\": \"US\" }"
            ).andExpect(status().isBadRequest());
            httpPostAsset("{ \"countryCode\": \"US\" }").andExpect(status().isBadRequest());
            httpPostAsset("{ \"id\": \"123\" }").andExpect(status().isBadRequest());
            httpDeleteAsset("  ", "OK").andExpect(status().isBadRequest());
            httpDeleteAsset("123", "  ").andExpect(status().isBadRequest());
        }

        @Test
        void nonExisting_returnsNotFound() throws Exception {
            given(facadeNeededByController.findForAccount(AccountId.valueOf(ACCOUNT_ID)))
                    .willThrow(BusinessException.notFound("Account", ACCOUNT_ID));
            doThrow(BusinessException.notFound("Account", ACCOUNT_ID))
                    .when(facadeNeededByController)
                    .assignDownloadedAsset(eq(AccountId.valueOf(ACCOUNT_ID)), any(Asset.class));
            doThrow(BusinessException.notFound("Account", ACCOUNT_ID))
                    .when(facadeNeededByController)
                    .removeDownloadedAsset(eq(AccountId.valueOf(ACCOUNT_ID)), any(Asset.class));

            // expect 404
            httpGetAssets("lookMaNotExistingId").andExpect(status().isNotFound());
            httpPostAsset("{ \"id\": \"123\", \"countryCode\": \"US\" }").andExpect(status().isNotFound());
            httpDeleteAsset("123", "US").andExpect(status().isNotFound());
        }

        @Test
        void overLimit_returnsUnprocessableEntity() throws Exception {
            // given
            doThrow(LimitingFacade.AccountLimitExceeded.class)
                    .when(facadeNeededByController)
                    .assignDownloadedAsset(eq(AccountId.valueOf(ACCOUNT_ID)), any(Asset.class));

            // expect 422
            httpPostAsset("{ \"id\": \"123\", \"countryCode\": \"US\" }").andExpect(status().isUnprocessableEntity());
        }

        private ResultActions httpDeleteAsset(String assetId, String countryCode) throws Exception {
            return mockMvc.perform(delete("/api/accounts/{id}/assets/{assetId}", ACCOUNT_ID, assetId)
                    .queryParam("countryCode", countryCode));
        }

        private ResultActions httpGetAssets(String accountId) throws Exception {
            return mockMvc.perform(get("/api/accounts/{id}/assets", accountId).contentType(APPLICATION_JSON));
        }

        private ResultActions httpPostAsset(String... jsonLines) throws Exception {
            return httpPostAssetForAccount(ACCOUNT_ID, jsonLines);
        }

        private ResultActions httpPostAssetForAccount(String accountId, String... jsonLines) throws Exception {
            return mockMvc.perform(post("/api/accounts/{id}/assets", accountId)
                    .contentType(APPLICATION_JSON)
                    .content(String.join("\n", jsonLines)));
        }
    }

    @Nested
    @DataJdbcTest
    @AutoConfigureTestDatabase(replace = NONE) // auto-configured doesn't use CASE_INSENSITIVE_IDENTIFIERS=TRUE
    class AccountRepositoryTest {
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
            )).withMessageContaining("test-asset");
        }
    }

    @Nested
    @DataJdbcTest
    @AutoConfigureTestDatabase(replace = NONE) // auto-configured doesn't use CASE_INSENSITIVE_IDENTIFIERS=TRUE
    class AccountSettingRepositoryTest {
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
}
