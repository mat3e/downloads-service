package io.github.mat3e.downloads.limiting.rest;

import io.github.mat3e.downloads.limiting.LimitingFacade;
import io.github.mat3e.downloads.limiting.LimitingFacade.AccountLimitExceeded;
import io.github.mat3e.downloads.limiting.api.AccountId;
import io.github.mat3e.downloads.limiting.api.Asset;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/accounts/{accountId}/assets")
@RequiredArgsConstructor
class LimitingController {
    private final LimitingFacade facade;

    @GetMapping
    ResponseEntity<List<Asset>> readAssets(@Valid @PathVariable AccountId accountId) {
        return ResponseEntity.of(facade.findForAccount(accountId));
    }

    @PostMapping
    ResponseEntity<Void> addAsset(@Valid @PathVariable AccountId accountId, @Valid @RequestBody Asset asset) {
        facade.assignDownloadedAsset(accountId, asset);
        return ResponseEntity.created(URI.create("/")).build();
    }

    @SuppressWarnings("java:S6856") // Spring builds Asset from path and query, no need to declare @PathVariable
    @DeleteMapping(path = "/{id}", params = "countryCode")
    ResponseEntity<Void> removeAsset(@Valid @PathVariable AccountId accountId, @Valid Asset asset) {
        facade.removeDownloadedAsset(accountId, asset);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(AccountLimitExceeded.class)
    ResponseEntity<ExceededLimit> handleAccountLimitExceeded(AccountLimitExceeded exception) {
        return ResponseEntity.unprocessableEntity().body(ExceededLimit.from(exception));
    }

    @Value
    private static class ExceededLimit {
        static ExceededLimit from(AccountLimitExceeded exception) {
            return new ExceededLimit(exception.getMessage(), exception.getLimit());
        }

        String message;
        int limit;
    }
}
