package io.github.mat3e.downloads.limiting.api;

import lombok.RequiredArgsConstructor;
import lombok.Value;

import jakarta.validation.constraints.NotBlank;

import static lombok.AccessLevel.PRIVATE;

@Value
@RequiredArgsConstructor(access = PRIVATE)
public class Asset {
    @NotBlank
    String id;
    @NotBlank
    String countryCode;

    public static AssetBuilder withId(String id) {
        return new AssetBuilder(id);
    }

    @RequiredArgsConstructor
    public static class AssetBuilder {
        private final String id;

        public Asset inCountry(String countryCode) {
            return new Asset(id, countryCode);
        }
    }
}
