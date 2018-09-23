/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.core.payment.validation;

import bisq.core.app.BisqEnvironment;
import bisq.core.btc.network.BaseCurrencyNetwork;
import bisq.core.locale.CurrencyUtil;
import bisq.core.locale.Res;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;



import bisq.asset.AssetRegistry;

public class AltCoinAddressValidatorTest {

    @Test
    public void test() {
        AltCoinAddressValidator validator = new AltCoinAddressValidator(new AssetRegistry());

        BaseCurrencyNetwork baseCurrencyNetwork = BisqEnvironment.getBaseCurrencyNetwork();
        String currencyCode = baseCurrencyNetwork.getCurrencyCode();
        Res.setBaseCurrencyCode(currencyCode);
        Res.setBaseCurrencyName(baseCurrencyNetwork.getCurrencyName());
        CurrencyUtil.setBaseCurrencyCode(currencyCode);

        validator.setCurrencyCode("BTC");
        assertTrue(validator.validate("17VZNX1SN5NtKa8UQFxwQbFeFc3iqRYhem").isValid);

        validator.setCurrencyCode("LTC");
        assertTrue(validator.validate("Lg3PX8wRWmApFCoCMAsPF5P9dPHYQHEWKW").isValid);

        validator.setCurrencyCode("BOGUS");
        try {
            validator.validate("1BOGUSADDR");
            fail("expected validation to fail for unregistered asset 'BOGUS'");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), containsString("'BOGUS' is not a registered asset"));
        }
    }
}
