package io.taucoin.news.publishing.ui.transaction;

import android.text.SpannableStringBuilder;
import io.taucoin.news.publishing.core.Constants;
import io.taucoin.news.publishing.core.model.data.message.TxType;
import io.taucoin.news.publishing.core.model.data.message.QueueOperation;
import io.taucoin.news.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.news.publishing.core.utils.ChainIDUtil;
import io.taucoin.news.publishing.core.utils.FmtMicrometer;
import io.taucoin.news.publishing.core.utils.LinkUtil;
import io.taucoin.news.publishing.core.utils.SpanUtils;
import io.taucoin.news.publishing.core.utils.StringUtil;
import io.taucoin.news.publishing.core.utils.Utils;

public class WiringCoinsMsg {

    //构建Wiring消息内容，交易通知
    /* 0. Funds update:
     * 1. Community: 
     * 2. Transmission ID: 
     * 3. Amount: 
     * 4. Fee: 
     * 5. Description: 
     * 6. Community Link:
     */
    public static SpannableStringBuilder createWiringCoinsMsg(Tx tx, QueueOperation operation) {
        String coinName = ChainIDUtil.getCoinName(tx.chainID);
        SpanUtils msg = new SpanUtils();
        int txType = tx.txType;
        if (operation != null) {
            msg.append("Funds update: ");
            if (operation == QueueOperation.INSERT) {
                if (tx.txType == TxType.WIRING_TX.getType()) {
                    msg.append(FmtMicrometer.fmtBalance(tx.amount))
                            .append(" ")
                            .append(coinName);
                    msg.append(" is sent to you.");
                } else {
                    msg.append("a news transaction is posted.");
                }
            } else if (operation == QueueOperation.UPDATE) {
                msg.append("transaction pending on settlement");
            } else if (operation == QueueOperation.DELETE) {
                msg.append("sender cancels wiring");
            }
            msg.append("\n").append("Community: ")
                .append(ChainIDUtil.getName(tx.chainID))
                .append("\n").append("Transmission ID: ")
                .append(String.valueOf(tx.txID))
                .append("\n");
        }
        //转账交易, 交易金额
        if (txType == TxType.WIRING_TX.getType() && tx.amount > 0) {
            msg.append("Amount: ").append(FmtMicrometer.fmtBalance(tx.amount))
                    .append(" ").append(coinName)
                    .append("\n");
        }
        //交易, 交易费
        msg.append("Fee: ").append(FmtMicrometer.fmtFeeValue(tx.fee))
                .append(" ").append(coinName);

        if (txType == TxType.WIRING_TX.getType()) {
            if (StringUtil.isNotEmpty(tx.memo)) {
                msg.append("\n").append("Description: ").append(tx.memo);
            }
            if (operation == QueueOperation.INSERT) {
                msg.append("\n").append("Community Link: ")
                   .append(LinkUtil.encodeChain(tx.senderPk, tx.chainID, tx.senderPk));
            }
        }
        //交易时间
        msg.append("\n").append("Tx Created Time: ").append(FmtMicrometer.fmtFeeValue(tx.timestamp));
        return msg.create();
    }

    public static String decodeContentStr(byte[] content) {
        String contentStr = Utils.textBytesToString(content);
		int pos = contentStr.indexOf("Tx Created Time: ");
		if (pos >= 0)
			return contentStr.substring(0, pos);
		else 
			return contentStr;
    }
}
