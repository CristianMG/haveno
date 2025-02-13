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

package bisq.core.payment;

import bisq.core.locale.FiatCurrency;
import bisq.core.payment.payload.FasterPaymentsAccountPayload;
import bisq.core.payment.payload.PaymentAccountPayload;
import bisq.core.payment.payload.PaymentMethod;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public final class FasterPaymentsAccount extends PaymentAccount {
    public FasterPaymentsAccount() {
        super(PaymentMethod.FASTER_PAYMENTS);
        setSingleTradeCurrency(new FiatCurrency("GBP"));
    }

    @Override
    protected PaymentAccountPayload createPayload() {
        return new FasterPaymentsAccountPayload(paymentMethod.getId(), id);
    }

    public void setHolderName(String value) {
        ((FasterPaymentsAccountPayload) paymentAccountPayload).setHolderName(value);
    }

    public String getHolderName() {
        return ((FasterPaymentsAccountPayload) paymentAccountPayload).getHolderName();
    }

    public void setSortCode(String value) {
        ((FasterPaymentsAccountPayload) paymentAccountPayload).setSortCode(value);
    }

    public String getSortCode() {
        return ((FasterPaymentsAccountPayload) paymentAccountPayload).getSortCode();
    }

    public void setAccountNr(String value) {
        ((FasterPaymentsAccountPayload) paymentAccountPayload).setAccountNr(value);
    }

    public String getAccountNr() {
        return ((FasterPaymentsAccountPayload) paymentAccountPayload).getAccountNr();
    }
}
