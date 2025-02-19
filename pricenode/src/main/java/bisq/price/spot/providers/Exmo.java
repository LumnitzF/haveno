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

import org.knowm.xchange.exmo.ExmoExchange;

import org.springframework.stereotype.Component;

import java.time.Duration;

import java.util.Set;

@Component
class Exmo extends ExchangeRateProvider {

    public Exmo() {
        // API rate limit = 10 calls / second from the same IP ( see https://exmo.com/en/api )
        super("EXMO", "exmo", Duration.ofMinutes(1));
    }

    @Override
    public Set<ExchangeRate> doGet() {
        // Supported fiat: EUR, PLN, RUB, UAH (Ukrainian hryvnia), USD
        // Supported alts: DASH, DOGE, ETC, ETH, LTC, XMR, ZEC
        return doGet(ExmoExchange.class);
    }

}
