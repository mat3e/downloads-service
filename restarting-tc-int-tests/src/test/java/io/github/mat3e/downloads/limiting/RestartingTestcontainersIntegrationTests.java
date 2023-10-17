package io.github.mat3e.downloads.limiting;

import io.github.mat3e.downloads.limiting.api.AccountId;
import io.github.mat3e.downloads.limiting.api.Asset;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestartingTestcontainersIntegrationTests {
    @Nested
    @IntegrationTest
    class LimitingControllerTest {
        private static final String ACCOUNT_ID = "1";

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private LimitingFacade facadeNeededByController;

        @Container
        static MariaDBContainer<?> mariaDb = new MariaDBContainer<>(DockerImageName.parse("mariadb:11"));

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
            // expect 404
            httpGetAssets("lookMaNotExistingId").andExpect(status().isNotFound());
            httpPostAsset("{ \"id\": \"123\", \"countryCode\": \"US\" }").andExpect(status().isNotFound());
            httpDeleteAsset("123", "US").andExpect(status().isNotFound());
        }

        @Test
        void overLimit_returnsUnprocessableEntity() throws Exception {
            // given
            facadeNeededByController.overrideAccountLimit(AccountId.valueOf(ACCOUNT_ID), 0);

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

        @DynamicPropertySource
        static void props(DynamicPropertyRegistry registry) {
            registry.add("spring.datasource.url", mariaDb::getJdbcUrl);
            registry.add("spring.datasource.username", mariaDb::getUsername);
            registry.add("spring.datasource.password", mariaDb::getPassword);
        }
    }

    @Nested
    @IntegrationTest
    class AccountRepositoryTest {
        @Autowired
        private AccountRepository systemUnderTest;

        @Autowired
        private JdbcTemplate jdbc;

        private final Clock clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);

        @Container
        static MariaDBContainer<?> mariaDb = new MariaDBContainer<>(DockerImageName.parse("mariadb:11"));

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

        @DynamicPropertySource
        static void props(DynamicPropertyRegistry registry) {
            registry.add("spring.datasource.url", mariaDb::getJdbcUrl);
            registry.add("spring.datasource.username", mariaDb::getUsername);
            registry.add("spring.datasource.password", mariaDb::getPassword);
        }
    }

    @Nested
    @IntegrationTest
    class AccountSettingRepositoryTest {
        @Autowired
        private AccountSettingRepository systemUnderTest;

        @Container
        static MariaDBContainer<?> mariaDb = new MariaDBContainer<>(DockerImageName.parse("mariadb:11"));

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

        @DynamicPropertySource
        static void props(DynamicPropertyRegistry registry) {
            registry.add("spring.datasource.url", mariaDb::getJdbcUrl);
            registry.add("spring.datasource.username", mariaDb::getUsername);
            registry.add("spring.datasource.password", mariaDb::getPassword);
        }
    }
}
