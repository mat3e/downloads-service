package io.github.mat3e.downloads.limiting.api;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value(staticConstructor = "valueOf")
public class AccountId {
    @NotBlank
    String id;
}
