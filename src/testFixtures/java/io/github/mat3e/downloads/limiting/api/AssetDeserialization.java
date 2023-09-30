package io.github.mat3e.downloads.limiting.api;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AssetDeserialization {
    private String id;
    private String countryCode;

     public Asset toApi() {
        return Asset.withId(id).inCountry(countryCode);
    }
}
