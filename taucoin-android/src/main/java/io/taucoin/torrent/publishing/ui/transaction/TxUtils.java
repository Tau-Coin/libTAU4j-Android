package io.taucoin.torrent.publishing.ui.transaction;

import android.content.Context;
import android.text.SpannableStringBuilder;

import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.message.TxType;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.SpanUtils;
import io.taucoin.torrent.publishing.core.utils.StringUtil;

public class TxUtils {

    public static SpannableStringBuilder createTxSpan(Tx tx) {
        switch (TxType.valueOf(tx.txType)) {
            case NOTE_TX:
                return createSpanNoteTx(tx);
            case WRING_TX:
                return createSpanWringTx(tx);
            case SELL_TX:
                return createSpanSellTx(tx);
        }
        return new SpanUtils().create();
    }

    private static SpannableStringBuilder createSpanNoteTx(Tx tx) {
        SpanUtils msg = new SpanUtils()
                .append(tx.memo);
        return msg.create();
    }

    private static SpannableStringBuilder createSpanWringTx(Tx tx) {
        Context context = MainApplication.getInstance();
        int titleColor = context.getResources().getColor(R.color.gray_dark);
        int blueColor = context.getResources().getColor(R.color.color_blue_link);
        SpanUtils msg = new SpanUtils()
                .append("Amount: ").setForegroundColor(titleColor)
                .append(FmtMicrometer.fmtBalance(tx.amount))
                .append(" ")
                .append(ChainIDUtil.getCoinName(tx.chainID))
                .append("\n").append("Fee:").setForegroundColor(titleColor)
                .append(FmtMicrometer.fmtFeeValue(tx.fee))
                .append("\n").append("To:").setForegroundColor(titleColor)
                .append(tx.receiverPk).setForegroundColor(blueColor)
                .append("\n").append("Hash:").setForegroundColor(titleColor)
                .append(tx.txID).setForegroundColor(blueColor);
        if (tx.txStatus == 1) {
            msg.append("\n").append("Blocknumber:").setForegroundColor(titleColor)
                    .append(FmtMicrometer.fmtLong(tx.blockNumber));
        }
        msg.append("\n").append("Nonce:").setForegroundColor(titleColor)
                .append(FmtMicrometer.fmtLong(tx.nonce))
                .append("\n").append("Memo:").setForegroundColor(titleColor)
                .append(tx.memo);
        return msg.create();
    }

    private static SpannableStringBuilder createSpanSellTx(Tx tx) {
        Context context = MainApplication.getInstance();
        int titleColor = context.getResources().getColor(R.color.gray_dark);
        int blueColor = context.getResources().getColor(R.color.color_blue_link);
        SpanUtils msg = new SpanUtils()
                .append("Sell: ").setForegroundColor(titleColor)
                .append(tx.coinName);
        if (StringUtil.isNotEmpty(tx.link)) {
            msg.append("\n").append("Link: ").setForegroundColor(titleColor)
                    .append(tx.link).setForegroundColor(blueColor);
        }
        if (StringUtil.isNotEmpty(tx.location)) {
            msg.append("\n").append("Location: ").setForegroundColor(titleColor)
                    .append(tx.location);
        }
        if (StringUtil.isNotEmpty(tx.memo)) {
            msg.append("\n").append("Description: ").setForegroundColor(titleColor)
                    .append(tx.memo);
        }
        return msg.create();
    }
}
