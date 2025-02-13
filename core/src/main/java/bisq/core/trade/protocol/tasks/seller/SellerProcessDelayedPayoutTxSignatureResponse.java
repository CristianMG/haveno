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

package bisq.core.trade.protocol.tasks.seller;

import bisq.core.trade.Trade;
import bisq.core.trade.messages.DelayedPayoutTxSignatureResponse;
import bisq.core.trade.protocol.tasks.TradeTask;

import bisq.common.taskrunner.TaskRunner;

import lombok.extern.slf4j.Slf4j;

import static bisq.core.util.Validator.checkTradeId;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class SellerProcessDelayedPayoutTxSignatureResponse extends TradeTask {
    public SellerProcessDelayedPayoutTxSignatureResponse(TaskRunner<Trade> taskHandler, Trade trade) {
        super(taskHandler, trade);
    }

    @Override
    protected void run() {
        try {
            runInterceptHook();
            DelayedPayoutTxSignatureResponse response = (DelayedPayoutTxSignatureResponse) processModel.getTradeMessage();
            checkNotNull(response);
            checkTradeId(processModel.getOfferId(), response);

            trade.getTradingPeer().setDelayedPayoutTxSignature(checkNotNull(response.getDelayedPayoutTxBuyerSignature()));

            processModel.getTradeWalletService().sellerAddsBuyerWitnessesToDepositTx(
                    processModel.getDepositTx(),
                    processModel.getBtcWalletService().getTxFromSerializedTx(response.getDepositTx())
            );

            // update to the latest peer address of our peer if the message is correct
            trade.setTradingPeerNodeAddress(processModel.getTempTradingPeerNodeAddress());

            processModel.getTradeManager().requestPersistence();

            complete();
        } catch (Throwable t) {
            failed(t);
        }
    }
}
