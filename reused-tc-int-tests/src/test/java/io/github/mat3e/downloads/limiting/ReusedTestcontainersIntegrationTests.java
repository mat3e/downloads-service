package io.github.mat3e.downloads.limiting;

import io.github.mat3e.downloads.limiting.api.AccountId;
import io.github.mat3e.downloads.limiting.api.Asset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReusedTestcontainersIntegrationTests {
    static long timer = 0;

    @BeforeAll
    static void setUp() {
        timer = System.currentTimeMillis();
    }

    @AfterAll
    static void tearDown() {
        System.out.println("Execution time: " + (System.currentTimeMillis() - timer));
    }

    @Nested
    @IntegrationTest
    class AccountLimitEventListenerTest {
        private static final AccountId ACCOUNT_ID = AccountId.valueOf("1");

        @Autowired
        private KafkaTemplate<String, Message> kafkaTemplate;

        @Autowired
        private JdbcTemplate jdbc;

        @Autowired
        private LimitingFacade facadeNeededByListener;

        @ServiceConnection
        static MariaDBContainer<?> mariaDb =
                new MariaDBContainer<>(DockerImageName.parse("mariadb:11")).withReuse(true);

        @ServiceConnection
        static KafkaContainer kafka =
                new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.6")).withReuse(true);

        static {
            Startables.deepStart(mariaDb, kafka).join();
        }

        @Test
        void incomingMessage_startsProcessing() throws ExecutionException, InterruptedException {
            whenAccountLimitMessage(2);
            // and
            await().atMost(5, SECONDS).until(interactedWithFacade());

            then(facadeNeededByListener.findForAccount(ACCOUNT_ID)).isPresent();

            // cleaning as different thread committed
            TestTransaction.end();
            jdbc.update("delete from downloading_accounts where id = ?", ACCOUNT_ID.getId());
        }

        private void whenAccountLimitMessage(int limit) throws ExecutionException, InterruptedException {
            kafkaTemplate.send("limit-changes", new Message(ACCOUNT_ID.getId(), limit)).get();
        }

        private Callable<Boolean> interactedWithFacade() {
            return () -> facadeNeededByListener.findForAccount(ACCOUNT_ID).isPresent();
        }

        record Message(String accountId, int limit) {
        }
    }

    @Nested
    @IntegrationTest
    class LimitingControllerTest {
        private static final String ACCOUNT_ID = "1";

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private LimitingFacade facadeNeededByController;

        @ServiceConnection
        static MariaDBContainer<?> mariaDb =
                new MariaDBContainer<>(DockerImageName.parse("mariadb:11")).withReuse(true);

        @ServiceConnection
        static KafkaContainer kafka =
                new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.6")).withReuse(true);

        static {
            Startables.deepStart(mariaDb, kafka).join();
        }

        @Test
        void illegalParams_returnsBadRequest() throws Exception {
            // expect 400
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
            httpPostAssetForAccount(
                    "  ",
                    "{ \"id\": \"123\", \"countryCode\": \"US\" }"
            ).andExpect(status().isNotFound());
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
    }

    @Nested
    @IntegrationTest
    class AccountRepositoryTest {
        @Autowired
        private AccountRepository systemUnderTest;

        @Autowired
        private JdbcTemplate jdbc;

        private final Clock clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);

        @ServiceConnection
        static MariaDBContainer<?> mariaDb =
                new MariaDBContainer<>(DockerImageName.parse("mariadb:11")).withReuse(true);

        @ServiceConnection

        static KafkaContainer kafka =
                new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.6")).withReuse(true);

        static {
            Startables.deepStart(mariaDb, kafka).join();
        }

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

            then(systemUnderTest.findById(account.id())).hasValueSatisfying(insertedAccount ->
                    then(insertedAccount.assets()).hasSize(1));

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
    @IntegrationTest
    class AccountSettingRepositoryTest {
        @Autowired
        private AccountSettingRepository systemUnderTest;

        @ServiceConnection
        static MariaDBContainer<?> mariaDb =
                new MariaDBContainer<>(DockerImageName.parse("mariadb:11")).withReuse(true);

        @ServiceConnection
        static KafkaContainer kafka =
                new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.6")).withReuse(true);

        static {
            Startables.deepStart(mariaDb, kafka).join();
        }

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
