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

package bisq.core.trade.protocol;

import bisq.core.trade.BuyerAsMakerTrade;
import bisq.core.trade.Trade;
import bisq.core.trade.messages.DelayedPayoutTxSignatureRequest;
import bisq.core.trade.messages.DepositResponse;
import bisq.core.trade.messages.DepositTxAndDelayedPayoutTxMessage;
import bisq.core.trade.messages.InitMultisigRequest;
import bisq.core.trade.messages.InitTradeRequest;
import bisq.core.trade.messages.PaymentAccountPayloadRequest;
import bisq.core.trade.messages.PayoutTxPublishedMessage;
import bisq.core.trade.messages.SignContractRequest;
import bisq.core.trade.messages.SignContractResponse;
import bisq.core.trade.protocol.tasks.ProcessDepositResponse;
import bisq.core.trade.protocol.tasks.ProcessInitMultisigRequest;
import bisq.core.trade.protocol.tasks.ProcessInitTradeRequest;
import bisq.core.trade.protocol.tasks.ProcessPaymentAccountPayloadRequest;
import bisq.core.trade.protocol.tasks.ProcessSignContractRequest;
import bisq.core.trade.protocol.tasks.ProcessSignContractResponse;
import bisq.core.trade.protocol.tasks.SendSignContractRequestAfterMultisig;
import bisq.core.trade.protocol.tasks.TradeTask;
import bisq.core.trade.protocol.tasks.buyer.BuyerFinalizesDelayedPayoutTx;
import bisq.core.trade.protocol.tasks.buyer.BuyerProcessDelayedPayoutTxSignatureRequest;
import bisq.core.trade.protocol.tasks.buyer.BuyerSendsDelayedPayoutTxSignatureResponse;
import bisq.core.trade.protocol.tasks.buyer.BuyerSignsDelayedPayoutTx;
import bisq.core.trade.protocol.tasks.buyer.BuyerVerifiesPreparedDelayedPayoutTx;
import bisq.core.trade.protocol.tasks.maker.MakerRemovesOpenOffer;
import bisq.core.trade.protocol.tasks.maker.MakerSendsInitTradeRequestIfUnreserved;
import bisq.core.trade.protocol.tasks.maker.MakerVerifyTakerFeePayment;
import bisq.core.util.Validator;

import bisq.network.p2p.NodeAddress;

import bisq.common.handlers.ErrorMessageHandler;
import bisq.common.handlers.ResultHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BuyerAsMakerProtocol extends BuyerProtocol implements MakerProtocol {

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    public BuyerAsMakerProtocol(BuyerAsMakerTrade trade) {
        super(trade);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // MakerProtocol
    ///////////////////////////////////////////////////////////////////////////////////////////

    // TODO (woodser): these methods are duplicated with SellerAsMakerProtocol due to single inheritance

    @Override
    public void handleInitTradeRequest(InitTradeRequest message,
                                       NodeAddress peer,
                                       ErrorMessageHandler errorMessageHandler) {
        this.errorMessageHandler = errorMessageHandler;
        expect(phase(Trade.Phase.INIT)
                .with(message)
                .from(peer))
                .setup(tasks(
                        ProcessInitTradeRequest.class,
                        //ApplyFilter.class, // TODO (woodser): these checks apply when maker signs availability request, but not here
                        //VerifyPeersAccountAgeWitness.class, // TODO (woodser): these checks apply after in multisig, means if rejected need to reimburse other's fee
                        MakerSendsInitTradeRequestIfUnreserved.class)
                .using(new TradeTaskRunner(trade,
                        () -> {
                            handleTaskRunnerSuccess(peer, message);
                        },
                        errorMessage -> {
                            errorMessageHandler.handleErrorMessage(errorMessage);
                            handleTaskRunnerFault(peer, message, errorMessage);
                        }))
                .withTimeout(30))
                .executeTasks();
    }
    
    @Override
    public void handleInitMultisigRequest(InitMultisigRequest request, NodeAddress sender) {
      System.out.println("BuyerAsMakerProtocol.handleInitMultisigRequest()");
      Validator.checkTradeId(processModel.getOfferId(), request);
      processModel.setTradeMessage(request); // TODO (woodser): synchronize access since concurrent requests processed
      expect(anyPhase(Trade.Phase.INIT)
              .with(request)
              .from(sender))
              .setup(tasks(
                      ProcessInitMultisigRequest.class,
                      SendSignContractRequestAfterMultisig.class)
              .using(new TradeTaskRunner(trade,
                  () -> {
                    handleTaskRunnerSuccess(sender, request);
                  },
                  errorMessage -> {
                      errorMessageHandler.handleErrorMessage(errorMessage);
                      handleTaskRunnerFault(sender, request, errorMessage);
                  }))
              .withTimeout(30))
              .executeTasks();
    }
    
    @Override
    public void handleSignContractRequest(SignContractRequest message, NodeAddress sender) {
        System.out.println("BuyerAsMakerProtocol.handleSignContractRequest()");
        Validator.checkTradeId(processModel.getOfferId(), message);
        processModel.setTradeMessage(message);
        expect(anyPhase(Trade.Phase.INIT)
                .with(message)
                .from(sender))
                .setup(tasks(
                        // TODO (woodser): validate request
                        ProcessSignContractRequest.class)
                .using(new TradeTaskRunner(trade,
                    () -> {
                      handleTaskRunnerSuccess(sender, message);
                    },
                    errorMessage -> {
                        errorMessageHandler.handleErrorMessage(errorMessage);
                        handleTaskRunnerFault(sender, message, errorMessage);
                    }))
                .withTimeout(30))
                .executeTasks();
    }

    @Override
    public void handleSignContractResponse(SignContractResponse message, NodeAddress sender) {
        System.out.println("BuyerAsMakerProtocol.handleSignContractResponse()");
        Validator.checkTradeId(processModel.getOfferId(), message);
        processModel.setTradeMessage(message); // TODO (woodser): synchronize access since concurrent requests processed
        expect(anyPhase(Trade.Phase.INIT)
                .with(message)
                .from(sender))
                .setup(tasks(
                        // TODO (woodser): validate request
                        ProcessSignContractResponse.class)
                .using(new TradeTaskRunner(trade,
                    () -> {
                      handleTaskRunnerSuccess(sender, message);
                    },
                    errorMessage -> {
                        errorMessageHandler.handleErrorMessage(errorMessage);
                        handleTaskRunnerFault(sender, message, errorMessage);
                    }))
                .withTimeout(30))
                .executeTasks();
    }
    
    @Override
    public void handleDepositResponse(DepositResponse response, NodeAddress sender) {
        System.out.println("BuyerAsMakerProtocol.handleDepositResponse()");
        Validator.checkTradeId(processModel.getOfferId(), response);
        processModel.setTradeMessage(response);
        expect(anyPhase(Trade.Phase.INIT, Trade.Phase.DEPOSIT_PUBLISHED)
                .with(response)
                .from(sender)) // TODO (woodser): ensure this asserts sender == response.getSenderNodeAddress()
                .setup(tasks(
                        // TODO (woodser): validate request
                        ProcessDepositResponse.class)
                .using(new TradeTaskRunner(trade,
                    () -> {
                      handleTaskRunnerSuccess(sender, response);
                    },
                    errorMessage -> {
                        errorMessageHandler.handleErrorMessage(errorMessage);
                        handleTaskRunnerFault(sender, response, errorMessage);
                    }))
                .withTimeout(30))
                .executeTasks();
    }
    
    @Override
    public void handlePaymentAccountPayloadRequest(PaymentAccountPayloadRequest request, NodeAddress sender) {
        System.out.println("BuyerAsMakerProtocol.handlePaymentAccountPayloadRequest()");
        Validator.checkTradeId(processModel.getOfferId(), request);
        processModel.setTradeMessage(request);
        expect(anyPhase(Trade.Phase.INIT, Trade.Phase.DEPOSIT_PUBLISHED)
                .with(request)
                .from(sender)) // TODO (woodser): ensure this asserts sender == response.getSenderNodeAddress()
                .setup(tasks(
                        // TODO (woodser): validate request
                        ProcessPaymentAccountPayloadRequest.class,
                        MakerRemovesOpenOffer.class)
                .using(new TradeTaskRunner(trade,
                    () -> {
                        stopTimeout();
                        handleTaskRunnerSuccess(sender, request);
                    },
                    errorMessage -> {
                        errorMessageHandler.handleErrorMessage(errorMessage);
                        handleTaskRunnerFault(sender, request, errorMessage);
                    }))
                .withTimeout(30))
                .executeTasks();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // User interaction
    ///////////////////////////////////////////////////////////////////////////////////////////

    // We keep the handler here in as well to make it more transparent which events we expect
    @Override
    public void onPaymentStarted(ResultHandler resultHandler, ErrorMessageHandler errorMessageHandler) {
        super.onPaymentStarted(resultHandler, errorMessageHandler);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Incoming message Payout tx
    ///////////////////////////////////////////////////////////////////////////////////////////

    // We keep the handler here in as well to make it more transparent which messages we expect
    @Override
    protected void handle(PayoutTxPublishedMessage message, NodeAddress peer) {
        super.handle(message, peer);
    }

    @Override
    protected Class<? extends TradeTask> getVerifyPeersFeePaymentClass() {
        return MakerVerifyTakerFeePayment.class;
    }

    // TODO (woodser): remove or ignore any unsupported requests

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Incoming messages Take offer process
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void handle(DelayedPayoutTxSignatureRequest message, NodeAddress peer) {
        expect(phase(Trade.Phase.TAKER_FEE_PUBLISHED)
                .with(message)
                .from(peer))
                .setup(tasks(
                        MakerRemovesOpenOffer.class,
                        BuyerProcessDelayedPayoutTxSignatureRequest.class,
                        BuyerVerifiesPreparedDelayedPayoutTx.class,
                        BuyerSignsDelayedPayoutTx.class,
                        BuyerFinalizesDelayedPayoutTx.class,
                        BuyerSendsDelayedPayoutTxSignatureResponse.class)
                        .withTimeout(60))
                .executeTasks();
    }

    // We keep the handler here in as well to make it more transparent which messages we expect
    @Override
    protected void handle(DepositTxAndDelayedPayoutTxMessage message, NodeAddress peer) {
        super.handle(message, peer);
    }
}
