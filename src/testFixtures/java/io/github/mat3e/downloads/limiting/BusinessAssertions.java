package io.github.mat3e.downloads.limiting;

import io.github.mat3e.downloads.limiting.api.AccountId;
import io.github.mat3e.downloads.limiting.api.Asset;
import org.assertj.core.api.BDDAssertions;
import org.assertj.core.api.ListAssert;

class BusinessAssertions extends BDDAssertions {

    static ListAssert<Asset> thenFoundIn(LimitingFacade facade, AccountId id) {
        var potentialAccountAssets = facade.findForAccount(id);
        assertThat(potentialAccountAssets).isPresent();
        return assertThat(potentialAccountAssets.get());
    }
}
