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

package bisq.core.payment.payload;

import bisq.core.payment.TradeLimits;

import bisq.core.locale.CurrencyUtil;
import bisq.core.locale.Res;
import bisq.core.locale.TradeCurrency;

import bisq.common.proto.persistable.PersistablePayload;

import org.bitcoinj.core.Coin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(exclude = {"maxTradePeriod", "maxTradeLimit"})
@ToString
@Slf4j
public final class PaymentMethod implements PersistablePayload, Comparable<PaymentMethod> {

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Static
    ///////////////////////////////////////////////////////////////////////////////////////////

    // time in blocks (average 10 min for one block confirmation
    //private static final long DAY = TimeUnit.HOURS.toMillis(24);
    private static final long DAY = TimeUnit.MINUTES.toMillis(1);	// TODO (woodser): changed to 1 minute for development, move to config?

    // Default trade limits.
    // We initialize very early before reading persisted data. We will apply later the limit from
    // the DAO param (Param.MAX_TRADE_LIMIT) but that can be only done after the dao is initialized.
    // The default values will be used for deriving the
    // risk factor so the relation between the risk categories stays the same as with the default values.
    // We must not change those values as it could lead to invalid offers if amount becomes lower then new trade limit.
    // Increasing might be ok, but needs more thought as well...
    private static final Coin DEFAULT_TRADE_LIMIT_VERY_LOW_RISK = Coin.parseCoin("100");
    private static final Coin DEFAULT_TRADE_LIMIT_LOW_RISK = Coin.parseCoin("50");
    private static final Coin DEFAULT_TRADE_LIMIT_MID_RISK = Coin.parseCoin("25");
    private static final Coin DEFAULT_TRADE_LIMIT_HIGH_RISK = Coin.parseCoin("12.5");

    public static final String UPHOLD_ID = "UPHOLD";
    public static final String MONEY_BEAM_ID = "MONEY_BEAM";
    public static final String POPMONEY_ID = "POPMONEY";
    public static final String REVOLUT_ID = "REVOLUT";
    public static final String PERFECT_MONEY_ID = "PERFECT_MONEY";
    public static final String SEPA_ID = "SEPA";
    public static final String SEPA_INSTANT_ID = "SEPA_INSTANT";
    public static final String FASTER_PAYMENTS_ID = "FASTER_PAYMENTS";
    public static final String NATIONAL_BANK_ID = "NATIONAL_BANK";
    public static final String JAPAN_BANK_ID = "JAPAN_BANK";
    public static final String AUSTRALIA_PAYID_ID = "AUSTRALIA_PAYID";
    public static final String SAME_BANK_ID = "SAME_BANK";
    public static final String SPECIFIC_BANKS_ID = "SPECIFIC_BANKS";
    public static final String SWISH_ID = "SWISH";
    public static final String ALI_PAY_ID = "ALI_PAY";
    public static final String WECHAT_PAY_ID = "WECHAT_PAY";
    public static final String CLEAR_X_CHANGE_ID = "CLEAR_X_CHANGE";
    public static final String CHASE_QUICK_PAY_ID = "CHASE_QUICK_PAY";
    public static final String INTERAC_E_TRANSFER_ID = "INTERAC_E_TRANSFER";
    public static final String US_POSTAL_MONEY_ORDER_ID = "US_POSTAL_MONEY_ORDER";
    public static final String CASH_DEPOSIT_ID = "CASH_DEPOSIT";
    public static final String MONEY_GRAM_ID = "MONEY_GRAM";
    public static final String WESTERN_UNION_ID = "WESTERN_UNION";
    public static final String HAL_CASH_ID = "HAL_CASH";
    public static final String F2F_ID = "F2F";
    public static final String BLOCK_CHAINS_ID = "BLOCK_CHAINS";
    public static final String PROMPT_PAY_ID = "PROMPT_PAY";
    public static final String ADVANCED_CASH_ID = "ADVANCED_CASH";
    public static final String TRANSFERWISE_ID = "TRANSFERWISE";
    public static final String AMAZON_GIFT_CARD_ID = "AMAZON_GIFT_CARD";
    public static final String BLOCK_CHAINS_INSTANT_ID = "BLOCK_CHAINS_INSTANT";
    public static final String CASH_BY_MAIL_ID = "CASH_BY_MAIL";

    // Cannot be deleted as it would break old trade history entries
    @Deprecated
    public static final String OK_PAY_ID = "OK_PAY";
    @Deprecated
    public static final String CASH_APP_ID = "CASH_APP"; // Removed due too high chargeback risk
    @Deprecated
    public static final String VENMO_ID = "VENMO";  // Removed due too high chargeback risk

    public static PaymentMethod UPHOLD;
    public static PaymentMethod MONEY_BEAM;
    public static PaymentMethod POPMONEY;
    public static PaymentMethod REVOLUT;
    public static PaymentMethod PERFECT_MONEY;
    public static PaymentMethod SEPA;
    public static PaymentMethod SEPA_INSTANT;
    public static PaymentMethod FASTER_PAYMENTS;
    public static PaymentMethod NATIONAL_BANK;
    public static PaymentMethod JAPAN_BANK;
    public static PaymentMethod AUSTRALIA_PAYID;
    public static PaymentMethod SAME_BANK;
    public static PaymentMethod SPECIFIC_BANKS;
    public static PaymentMethod SWISH;
    public static PaymentMethod ALI_PAY;
    public static PaymentMethod WECHAT_PAY;
    public static PaymentMethod CLEAR_X_CHANGE;
    public static PaymentMethod CHASE_QUICK_PAY;
    public static PaymentMethod INTERAC_E_TRANSFER;
    public static PaymentMethod US_POSTAL_MONEY_ORDER;
    public static PaymentMethod CASH_DEPOSIT;
    public static PaymentMethod MONEY_GRAM;
    public static PaymentMethod WESTERN_UNION;
    public static PaymentMethod F2F;
    public static PaymentMethod HAL_CASH;
    public static PaymentMethod BLOCK_CHAINS;
    public static PaymentMethod PROMPT_PAY;
    public static PaymentMethod ADVANCED_CASH;
    public static PaymentMethod TRANSFERWISE;
    public static PaymentMethod AMAZON_GIFT_CARD;
    public static PaymentMethod BLOCK_CHAINS_INSTANT;
    public static PaymentMethod CASH_BY_MAIL;

    // Cannot be deleted as it would break old trade history entries
    @Deprecated
    public static PaymentMethod OK_PAY = getDummyPaymentMethod(OK_PAY_ID);
    @Deprecated
    public static PaymentMethod CASH_APP = getDummyPaymentMethod(CASH_APP_ID); // Removed due too high chargeback risk
    @Deprecated
    public static PaymentMethod VENMO = getDummyPaymentMethod(VENMO_ID); // Removed due too high chargeback risk

    // The limit and duration assignment must not be changed as that could break old offers (if amount would be higher
    // than new trade limit) and violate the maker expectation when he created the offer (duration).
    @Getter
    private final static List<PaymentMethod> paymentMethods = new ArrayList<>(Arrays.asList(
            // EUR
            SEPA = new PaymentMethod(SEPA_ID, 6 * DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
            SEPA_INSTANT = new PaymentMethod(SEPA_INSTANT_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
            MONEY_BEAM = new PaymentMethod(MONEY_BEAM_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),

            // UK
            FASTER_PAYMENTS = new PaymentMethod(FASTER_PAYMENTS_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),

            // Sweden
            SWISH = new PaymentMethod(SWISH_ID, DAY, DEFAULT_TRADE_LIMIT_LOW_RISK),

            // US
            CLEAR_X_CHANGE = new PaymentMethod(CLEAR_X_CHANGE_ID, 4 * DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),

            POPMONEY = new PaymentMethod(POPMONEY_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
            CHASE_QUICK_PAY = new PaymentMethod(CHASE_QUICK_PAY_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
            US_POSTAL_MONEY_ORDER = new PaymentMethod(US_POSTAL_MONEY_ORDER_ID, 8 * DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),

            // Canada
            INTERAC_E_TRANSFER = new PaymentMethod(INTERAC_E_TRANSFER_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),

            // Global
            CASH_DEPOSIT = new PaymentMethod(CASH_DEPOSIT_ID, 4 * DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
            CASH_BY_MAIL = new PaymentMethod(CASH_BY_MAIL_ID, 8 * DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
            MONEY_GRAM = new PaymentMethod(MONEY_GRAM_ID, 4 * DAY, DEFAULT_TRADE_LIMIT_MID_RISK),
            WESTERN_UNION = new PaymentMethod(WESTERN_UNION_ID, 4 * DAY, DEFAULT_TRADE_LIMIT_MID_RISK),
            NATIONAL_BANK = new PaymentMethod(NATIONAL_BANK_ID, 4 * DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
            SAME_BANK = new PaymentMethod(SAME_BANK_ID, 2 * DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
            SPECIFIC_BANKS = new PaymentMethod(SPECIFIC_BANKS_ID, 4 * DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
            HAL_CASH = new PaymentMethod(HAL_CASH_ID, DAY, DEFAULT_TRADE_LIMIT_LOW_RISK),
            F2F = new PaymentMethod(F2F_ID, 4 * DAY, DEFAULT_TRADE_LIMIT_LOW_RISK),
            AMAZON_GIFT_CARD = new PaymentMethod(AMAZON_GIFT_CARD_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),

            // Trans national
            UPHOLD = new PaymentMethod(UPHOLD_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
            REVOLUT = new PaymentMethod(REVOLUT_ID, DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),
            PERFECT_MONEY = new PaymentMethod(PERFECT_MONEY_ID, DAY, DEFAULT_TRADE_LIMIT_LOW_RISK),
            ADVANCED_CASH = new PaymentMethod(ADVANCED_CASH_ID, DAY, DEFAULT_TRADE_LIMIT_VERY_LOW_RISK),
            TRANSFERWISE = new PaymentMethod(TRANSFERWISE_ID, 4 * DAY, DEFAULT_TRADE_LIMIT_HIGH_RISK),

            // Japan
            JAPAN_BANK = new PaymentMethod(JAPAN_BANK_ID, DAY, DEFAULT_TRADE_LIMIT_LOW_RISK),

            // Australia
            AUSTRALIA_PAYID = new PaymentMethod(AUSTRALIA_PAYID_ID, DAY, DEFAULT_TRADE_LIMIT_LOW_RISK),

            // China
            ALI_PAY = new PaymentMethod(ALI_PAY_ID, DAY, DEFAULT_TRADE_LIMIT_LOW_RISK),
            WECHAT_PAY = new PaymentMethod(WECHAT_PAY_ID, DAY, DEFAULT_TRADE_LIMIT_LOW_RISK),

            // Thailand
            PROMPT_PAY = new PaymentMethod(PROMPT_PAY_ID, DAY, DEFAULT_TRADE_LIMIT_LOW_RISK),

            // Altcoins
            BLOCK_CHAINS = new PaymentMethod(BLOCK_CHAINS_ID, DAY, DEFAULT_TRADE_LIMIT_VERY_LOW_RISK),
            // Altcoins with 1 hour trade period
            BLOCK_CHAINS_INSTANT = new PaymentMethod(BLOCK_CHAINS_INSTANT_ID, TimeUnit.HOURS.toMillis(1), DEFAULT_TRADE_LIMIT_VERY_LOW_RISK)
    ));

    static {
        paymentMethods.sort((o1, o2) -> {
            String id1 = o1.getId();
            if (id1.equals(CLEAR_X_CHANGE_ID))
                id1 = "ZELLE";
            String id2 = o2.getId();
            if (id2.equals(CLEAR_X_CHANGE_ID))
                id2 = "ZELLE";
            return id1.compareTo(id2);
        });
    }

    public static PaymentMethod getDummyPaymentMethod(String id) {
        return new PaymentMethod(id, 0, Coin.ZERO);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Instance fields
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Getter
    private final String id;

    // Must not change as old offers would get a new period then and that would violate the makers "contract" or
    // expectation when he created the offer.
    @Getter
    private final long maxTradePeriod;

    // With v0.9.4 we changed context of that field. Before it was the hard coded trade limit. Now it is the default
    // limit based on the risk factor.
    // The risk factor is derived from the maxTradeLimit.
    // As that field is used in protobuffer definitions we cannot change it to reflect better the new context. We prefer
    // to keep the convention that PB fields has the same name as the Java class field (as we could rename it in
    // Java without breaking PB).
    private final long maxTradeLimit;


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    /**
     * @param id             against charge back risk. If Bank do the charge back quickly the Arbitrator and the seller can push another
     *                       double spend tx to invalidate the time locked payout tx. For the moment we set all to 0 but will have it in
     *                       place when needed.
     * @param maxTradePeriod The min. period a trader need to wait until he gets displayed the contact form for opening a dispute.
     * @param maxTradeLimit  The max. allowed trade amount in Bitcoin for that payment method (depending on charge back risk)
     */
    private PaymentMethod(String id, long maxTradePeriod, Coin maxTradeLimit) {
        this.id = id;
        this.maxTradePeriod = maxTradePeriod;
        this.maxTradeLimit = maxTradeLimit.value;
    }

    // Used for dummy entries in payment methods list (SHOW_ALL)
    private PaymentMethod(String id) {
        this(id, 0, Coin.ZERO);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // PROTO BUFFER
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public protobuf.PaymentMethod toProtoMessage() {
        return protobuf.PaymentMethod.newBuilder()
                .setId(id)
                .setMaxTradePeriod(maxTradePeriod)
                .setMaxTradeLimit(maxTradeLimit)
                .build();
    }

    public static PaymentMethod fromProto(protobuf.PaymentMethod proto) {
        return new PaymentMethod(proto.getId(),
                proto.getMaxTradePeriod(),
                Coin.valueOf(proto.getMaxTradeLimit()));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////

    public static PaymentMethod getPaymentMethodById(String id) {
        return paymentMethods.stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
                .orElseGet(() -> new PaymentMethod(Res.get("shared.na")));
    }

    public Coin getMaxTradeLimitAsCoin(String currencyCode) {
        // Hack for SF as the smallest unit is 1 SF ;-( and price is about 3 BTC!
        if (currencyCode.equals("SF"))
            return Coin.parseCoin("4");

        // We use the class field maxTradeLimit only for mapping the risk factor.
        long riskFactor;
        if (maxTradeLimit == DEFAULT_TRADE_LIMIT_VERY_LOW_RISK.value)
            riskFactor = 1;
        else if (maxTradeLimit == DEFAULT_TRADE_LIMIT_LOW_RISK.value)
            riskFactor = 2;
        else if (maxTradeLimit == DEFAULT_TRADE_LIMIT_MID_RISK.value)
            riskFactor = 4;
        else if (maxTradeLimit == DEFAULT_TRADE_LIMIT_HIGH_RISK.value)
            riskFactor = 8;
        else {
            riskFactor = 8;
            log.error("maxTradeLimit is not matching one of our default values. maxTradeLimit=" + Coin.valueOf(maxTradeLimit).toFriendlyString());
        }

        TradeLimits tradeLimits = new TradeLimits();
        long maxTradeLimit = tradeLimits.getMaxTradeLimit().value;
        return Coin.valueOf(tradeLimits.getRoundedRiskBasedTradeLimit(maxTradeLimit, riskFactor));
    }

    public String getShortName() {
        // in cases where translation is not found, Res.get() simply returns the key string
        // so no need for special error-handling code.
        return Res.get(this.id + "_SHORT");
    }

    @Override
    public int compareTo(@NotNull PaymentMethod other) {
        return Res.get(id).compareTo(Res.get(other.id));
    }

    public String getDisplayString() {
        return Res.get(id);
    }

    public boolean isAsset() {
        return this.equals(BLOCK_CHAINS_INSTANT) || this.equals(BLOCK_CHAINS);
    }

    public static boolean hasChargebackRisk(PaymentMethod paymentMethod, List<TradeCurrency> tradeCurrencies) {
        return tradeCurrencies.stream()
                .anyMatch(tradeCurrency -> hasChargebackRisk(paymentMethod, tradeCurrency.getCode()));
    }

    public static boolean hasChargebackRisk(PaymentMethod paymentMethod) {
        return hasChargebackRisk(paymentMethod, CurrencyUtil.getMatureMarketCurrencies());
    }

    public static boolean hasChargebackRisk(PaymentMethod paymentMethod, String currencyCode) {
        if (paymentMethod == null)
            return false;

        String id = paymentMethod.getId();
        return hasChargebackRisk(id, currencyCode);
    }

    public static boolean hasChargebackRisk(String id, String currencyCode) {
        if (CurrencyUtil.getMatureMarketCurrencies().stream()
                .noneMatch(c -> c.getCode().equals(currencyCode)))
            return false;

        return id.equals(PaymentMethod.SEPA_ID) ||
                id.equals(PaymentMethod.SEPA_INSTANT_ID) ||
                id.equals(PaymentMethod.INTERAC_E_TRANSFER_ID) ||
                id.equals(PaymentMethod.CLEAR_X_CHANGE_ID) ||
                id.equals(PaymentMethod.REVOLUT_ID) ||
                id.equals(PaymentMethod.NATIONAL_BANK_ID) ||
                id.equals(PaymentMethod.SAME_BANK_ID) ||
                id.equals(PaymentMethod.SPECIFIC_BANKS_ID) ||
                id.equals(PaymentMethod.CHASE_QUICK_PAY_ID) ||
                id.equals(PaymentMethod.POPMONEY_ID) ||
                id.equals(PaymentMethod.MONEY_BEAM_ID) ||
                id.equals(PaymentMethod.UPHOLD_ID);
    }
}
