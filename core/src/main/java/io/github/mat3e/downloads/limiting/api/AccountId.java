package io.github.mat3e.downloads.limiting.api;

import lombok.Value;

import jakarta.validation.constraints.NotBlank;

@Value(staticConstructor = "valueOf")
public class AccountId {
    @NotBlank
    String id;
}
