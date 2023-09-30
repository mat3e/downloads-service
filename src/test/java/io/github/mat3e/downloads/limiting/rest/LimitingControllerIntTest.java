package io.github.mat3e.downloads.limiting.rest;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.github.mat3e.downloads.limiting.LimitingFacade;
import io.github.mat3e.downloads.limiting.api.AccountId;
import io.github.mat3e.downloads.limiting.api.Asset;
import io.github.mat3e.downloads.limiting.api.AssetDeserialization;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@AutoConfigureMockMvc
@SpringBootTest
class LimitingControllerIntTest {
    private static final String ACCOUNT_ID = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void downloadStarted_storesAssetsTillLimit() throws Exception {
        givenAccountLimit(2);
        // and
        httpSuccessfulPostAsset(
                "{",
                " \"id\": \"123\",",
                " \"countryCode\": \"US\"",
                "}");
        // and
        httpSuccessfulPostAsset(
                "{",
                " \"id\": \"456\",",
                " \"countryCode\": \"US\"",
                "}");

        // when
        httpPostAsset(
                "{",
                " \"id\": \"789\",",
                " \"countryCode\": \"US\"",
                "}"
        ).andExpect(status().isUnprocessableEntity());

        then(httpSuccessfulGetAssets()).containsExactly(
                Asset.withId("123").inCountry("US"),
                Asset.withId("456").inCountry("US"));

        // when
        httpSuccessfulDeleteAsset("123", "US");
        // and
        httpSuccessfulPostAsset(
                "{",
                " \"id\": \"789\",",
                " \"countryCode\": \"US\"",
                "}");

        then(httpSuccessfulGetAssets()).containsExactly(
                Asset.withId("456").inCountry("US"),
                Asset.withId("789").inCountry("US"));
    }

    @Test
    void illegalParams_returnsClientError() throws Exception {
        // given
        var validBody = "{ \"id\": \"123\", \"countryCode\": \"US\" }";

        // expect 404 - no account created, no assets
        httpGetAssets("lookMaNotExistingId").andExpect(status().isNotFound());
        httpPostAsset(validBody).andExpect(status().isNotFound());
        httpDeleteAsset("123", "US").andExpect(status().isNotFound());

        // expect 400
        httpPostAssetForAccount("  ", validBody).andExpect(status().isBadRequest());
        httpPostAsset("{ \"countryCode\": \"US\" }").andExpect(status().isBadRequest());
        httpPostAsset("{ \"id\": \"123\" }").andExpect(status().isBadRequest());
        httpDeleteAsset("  ", "OK").andExpect(status().isBadRequest());
        httpDeleteAsset("123", "  ").andExpect(status().isBadRequest());
    }

    private void httpSuccessfulDeleteAsset(String assetId, String countryCode) {
        try {
            httpDeleteAsset(assetId, countryCode).andExpect(status().isNoContent());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ResultActions httpDeleteAsset(String assetId, String countryCode) throws Exception {
        return mockMvc.perform(delete("/api/accounts/{id}/assets/{assetId}", ACCOUNT_ID, assetId)
                .queryParam("countryCode", countryCode));
    }

    private Collection<Asset> httpSuccessfulGetAssets() {
        try {
            String jsonResponse = httpGetAssets(ACCOUNT_ID)
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            JavaType returnType =
                    TypeFactory.defaultInstance().constructCollectionType(Collection.class, AssetDeserialization.class);
            return new ObjectMapper().<Collection<AssetDeserialization>>readValue(jsonResponse, returnType).stream()
                    .map(AssetDeserialization::toApi)
                    .collect(toUnmodifiableList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ResultActions httpGetAssets(String accountId) throws Exception {
        return mockMvc.perform(get("/api/accounts/{id}/assets", accountId).contentType(APPLICATION_JSON));
    }

    private void httpSuccessfulPostAsset(String... jsonLines) {
        try {
            httpPostAsset(jsonLines).andExpect(status().isCreated());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ResultActions httpPostAsset(String... jsonLines) throws Exception {
        return httpPostAssetForAccount(ACCOUNT_ID, jsonLines);
    }

    private ResultActions httpPostAssetForAccount(String accountId, String... jsonLines) throws Exception {
        return mockMvc.perform(post("/api/accounts/{id}/assets", accountId)
                .contentType(APPLICATION_JSON)
                .content(String.join("\n", jsonLines)));
    }

    void givenAccountLimit(int limit) {
        facade.overrideAccountLimit(AccountId.valueOf(ACCOUNT_ID), limit);
    }

    @Autowired
    private LimitingFacade facade;
}
