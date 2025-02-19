/*
 * This file is part of Haveno.
 *
 * Haveno is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Haveno is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Haveno. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.price.spot.providers;

import bisq.price.spot.ExchangeRate;
import bisq.price.spot.ExchangeRateProvider;

import org.knowm.xchange.kraken.KrakenExchange;

import org.springframework.stereotype.Component;

import java.time.Duration;

import java.util.Set;

@Component
class Kraken extends ExchangeRateProvider {

    public Kraken() {
        super("KRAKEN", "kraken", Duration.ofMinutes(1));
    }

    @Override
    public Set<ExchangeRate> doGet() {
        // Supported fiat: AUD, CAD, CHF, EUR, GBP, JPY, USD
        // Supported alts: DASH, DOGE, ETC, ETH, LTC, XMR, ZEC
        return doGet(KrakenExchange.class);
    }

    @Override
    protected boolean requiresFilterDuringBulkTickerRetrieval() {
        return true;
    }
}
