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
import bisq.price.util.bitpay.BitpayMarketData;
import bisq.price.util.bitpay.BitpayTicker;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Component
class Bitpay extends ExchangeRateProvider {

    private final RestTemplate restTemplate = new RestTemplate();

    public Bitpay() {
        super("BITPAY", "bitpay", Duration.ofMinutes(1));
    }

    @Override
    public Set<ExchangeRate> doGet() {

        Set<ExchangeRate> result = new HashSet<ExchangeRate>();

        Predicate<BitpayTicker> isDesiredFiatPair = t -> SUPPORTED_FIAT_CURRENCIES.contains(t.getCode());
        Predicate<BitpayTicker> isDesiredCryptoPair = t -> SUPPORTED_CRYPTO_CURRENCIES.contains(t.getCode());

        getTickers()
                .filter(isDesiredFiatPair.or(isDesiredCryptoPair))
                .forEach(ticker -> {
                    boolean useInverseRate = false;
                    if (SUPPORTED_CRYPTO_CURRENCIES.contains(ticker.getCode())) {
                        // Use inverse rate for alts, because the API returns the
                        // conversion rate in the opposite direction than what we need
                        // API returns the BTC/Alt rate, we need the Alt/BTC rate
                        useInverseRate = true;
                    }

                    BigDecimal rate = ticker.getRate();
                    // Find the inverse rate, while using enough decimals to reflect very
                    // small exchange rates
                    BigDecimal inverseRate = (rate.compareTo(BigDecimal.ZERO) > 0) ?
                            BigDecimal.ONE.divide(rate, 8, RoundingMode.HALF_UP) :
                            BigDecimal.ZERO;

                    result.add(new ExchangeRate(
                            ticker.getCode(),
                            (useInverseRate ? inverseRate : rate),
                            new Date(),
                            this.getName()
                    ));
                });

        return result;
    }

    private Stream<BitpayTicker> getTickers() {
        BitpayMarketData marketData = restTemplate.exchange(
                RequestEntity
                        .get(UriComponentsBuilder
                                .fromUriString("https://bitpay.com/rates").build()
                                .toUri())
                        .build(),
                new ParameterizedTypeReference<BitpayMarketData>() {
                }
        ).getBody();

        return Arrays.stream(marketData.getData());
    }
}
