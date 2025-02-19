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

package bisq.core.trade.protocol.tasks.taker;

import bisq.core.trade.Trade;
import bisq.core.trade.protocol.tasks.TradeTask;

import bisq.common.taskrunner.TaskRunner;

import lombok.extern.slf4j.Slf4j;



import monero.wallet.model.MoneroTxWallet;

@Slf4j
public class TakerPublishFeeTx extends TradeTask {
    public TakerPublishFeeTx(TaskRunner<Trade> taskHandler, Trade trade) {
        super(taskHandler, trade);
    }

    @Override
    protected void run() {
        try {
            runInterceptHook();

            // We committed to be sure the tx gets into the wallet even in the broadcast process it would be
            // committed as well, but if user would close app before success handler returns the commit would not
            // be done.
            MoneroTxWallet takeOfferFeeTx = processModel.getTakeOfferFeeTx();
            processModel.getProvider().getXmrWalletService().getWallet().relayTx(takeOfferFeeTx);
            System.out.println("TAKER PUBLISHED FEE TX");
            System.out.println(takeOfferFeeTx);
            trade.setState(Trade.State.TAKER_PUBLISHED_TAKER_FEE_TX);
            complete();
        } catch (Throwable t) {
            log.error(t.toString());
            t.printStackTrace();
            trade.setErrorMessage("An error occurred.\n Error message:\n" + t.getMessage());
            failed(t);
        }
    }
}
