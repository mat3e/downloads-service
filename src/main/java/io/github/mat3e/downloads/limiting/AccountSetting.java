package io.github.mat3e.downloads.limiting;

import io.github.mat3e.downloads.exceptionhandling.BusinessException;
import io.github.mat3e.downloads.limiting.api.AccountId;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table("downloading_accounts")
class AccountSetting {
    static AccountSetting newFor(AccountId accountId, int limit) {
        assertAllowedLimit(limit);
        return new AccountSetting(accountId.getId(), limit, null);
    }

    @Id
    @EqualsAndHashCode.Include
    private final String id;
    @Column("limitation")
    private Integer limit;
    @Version
    private final Integer version;

    AccountId id() {
        return AccountId.valueOf(id);
    }

    int limit() {
        return limit;
    }

    void overrideLimit(int newLimit) {
        assertAllowedLimit(newLimit);
        this.limit = newLimit;
    }

    private static void assertAllowedLimit(int limit) {
        if (limit < 0) {
            throw new BusinessException("Limit must be non-negative");
        }
    }
}
