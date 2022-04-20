package io.taucoin.torrent.publishing.ui.transaction;

import android.content.Context;
import android.text.SpannableStringBuilder;

import org.libTAU4j.Block;
import org.libTAU4j.Transaction;

import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.BlockAndTx;
import io.taucoin.torrent.publishing.core.model.data.TxQueueAndStatus;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.model.data.message.AirdropTxContent;
import io.taucoin.torrent.publishing.core.model.data.message.AnnouncementContent;
import io.taucoin.torrent.publishing.core.model.data.message.SellTxContent;
import io.taucoin.torrent.publishing.core.model.data.message.TrustContent;
import io.taucoin.torrent.publishing.core.model.data.message.TxContent;
import io.taucoin.torrent.publishing.core.model.data.message.TxType;
import io.taucoin.torrent.publishing.core.model.data.message.QueueOperation;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.TxQueue;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.HashUtil;
import io.taucoin.torrent.publishing.core.utils.SpanUtils;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.rlp.ByteUtil;

public class TxUtils {

    public static SpannableStringBuilder createTxSpan(UserAndTx tx) {
        return createTxSpan(tx, CommunityTabFragment.TAB_NOTES);
    }

    /**
     * 从区块里解析展示交易
     */
    public static SpannableStringBuilder createBlockTxSpan(Tx tx) {
        Context context = MainApplication.getInstance();
        int titleColor = context.getResources().getColor(R.color.gray_dark);
        SpanUtils msg = new SpanUtils()
            .append(context.getString(R.string.community_tip_block_hash))
                .setForegroundColor(titleColor)
                .append(HashUtil.hashMiddleHide(tx.txID))
                .append("\n");
        return msg.create().append(createTxSpan(tx, CommunityTabFragment.TAB_NOTES));
    }

    public static SpannableStringBuilder createTxSpan(Tx tx, int tab) {
        switch (TxType.valueOf(tx.txType)) {
            case NOTE_TX:
                return createSpanNoteTx(tx, tab);
            case WIRING_TX:
                return createSpanWiringTx(tx, tab == CommunityTabFragment.TAB_CHAIN);
            case SELL_TX:
                return createSpanSellTx(tx, tab);
            case AIRDROP_TX:
                return createSpanAirdropTx(tx, tab);
            case TRUST_TX:
                return createSpanTrustTx(tx, tab);
            case ANNOUNCEMENT:
                return createSpanLeaderTx(tx, tab);
        }
        return new SpanUtils().create();
    }

    private static SpannableStringBuilder createSpanNoteTx(Tx tx, int tab) {
        SpanUtils msg = new SpanUtils();
        if (tab == CommunityTabFragment.TAB_CHAIN) {
            Context context = MainApplication.getInstance();
            int titleColor = context.getResources().getColor(R.color.gray_dark);
            String coinName = ChainIDUtil.getCoinName(tx.chainID);
            if (tx.txStatus == 1) {
                msg.append("Status: ").setForegroundColor(titleColor)
                    .append(context.getString(R.string.community_block_on_chain))
                    .append("\n");
            }
            msg.append("Message: ").setForegroundColor(titleColor)
                    .append(tx.memo)
                    .append("\n").append("Fee: ").setForegroundColor(titleColor)
                    .append(FmtMicrometer.fmtFeeValue(tx.fee))
                    .append(" ").append(coinName)
                    .append("\n").append("From: ").setForegroundColor(titleColor)
                    .append(HashUtil.hashMiddleHide(tx.senderPk));
            if (tx.txStatus == 1) {
                msg.append("\n").append("Blocknumber: ").setForegroundColor(titleColor)
                        .append(FmtMicrometer.fmtLong(tx.blockNumber));
            }
        } else {
            msg.append(tx.memo);
        }
        return msg.create();
    }

    private static SpannableStringBuilder createSpanWiringTx(Tx tx, boolean showStatus) {
        Context context = MainApplication.getInstance();
        int titleColor = context.getResources().getColor(R.color.gray_dark);
        String coinName = ChainIDUtil.getCoinName(tx.chainID);
        SpanUtils msg = new SpanUtils();
        if (tx.txStatus == 1 && showStatus) {
            msg.append("Status: ").setForegroundColor(titleColor)
                    .append(context.getString(R.string.community_block_on_chain))
                    .append("\n");
        }
        msg.append("Amount: ").setForegroundColor(titleColor)
                .append(FmtMicrometer.fmtBalance(tx.amount))
                .append(" ").append(coinName)
                .append("\n").append("Fee: ").setForegroundColor(titleColor)
                .append(FmtMicrometer.fmtFeeValue(tx.fee))
                .append(" ").append(coinName)
                .append("\n").append("From: ").setForegroundColor(titleColor)
                .append(HashUtil.hashMiddleHide(tx.senderPk))
                .append("\n").append("To: ").setForegroundColor(titleColor)
                .append(HashUtil.hashMiddleHide(tx.receiverPk))
                .append("\n").append("Hash: ").setForegroundColor(titleColor)
                .append(HashUtil.hashMiddleHide(tx.txID));
        if (tx.txStatus == 1 && showStatus) {
            msg.append("\n").append("Blocknumber: ").setForegroundColor(titleColor)
                    .append(FmtMicrometer.fmtLong(tx.blockNumber));
        }
        msg.append("\n").append("Nonce: ").setForegroundColor(titleColor)
                .append(FmtMicrometer.fmtLong(tx.nonce))
                .append("\n").append("Memo: ").setForegroundColor(titleColor)
                .append(tx.memo);
        return msg.create();
    }

    private static SpannableStringBuilder createSpanSellTx(Tx tx, int tab) {
        Context context = MainApplication.getInstance();
        int titleColor = context.getResources().getColor(R.color.gray_dark);
        SpanUtils msg = new SpanUtils();
        if ((tab == CommunityTabFragment.TAB_CHAIN ||
                tab == CommunityTabFragment.TAB_MARKET) && tx.txStatus == 1) {
            msg.append("Status: ").setForegroundColor(titleColor)
                    .append(context.getString(R.string.community_block_on_chain))
                    .append("\n");
        }
        msg.append("Selling: ").setForegroundColor(titleColor)
                .append(tx.coinName)
                .append("\n").append("Quantity: ").setForegroundColor(titleColor);
        if (tx.quantity > 0) {
            msg.append(FmtMicrometer.fmtBalance(tx.quantity));
        } else {
            msg.append("TBD");
        }
        if (StringUtil.isNotEmpty(tx.link) && tab != CommunityTabFragment.TAB_MARKET) {
            msg.append("\n").append("Link: ").setForegroundColor(titleColor)
                    .append(tx.link);
        }
        if (StringUtil.isNotEmpty(tx.location) && tab != CommunityTabFragment.TAB_MARKET) {
            msg.append("\n").append("Location: ").setForegroundColor(titleColor)
                    .append(tx.location);
        }
        if (StringUtil.isNotEmpty(tx.memo)) {
            msg.append("\n").append("Description: ").setForegroundColor(titleColor)
                    .append(tx.memo);
        }

        if (tab == CommunityTabFragment.TAB_CHAIN) {
            String coinName = ChainIDUtil.getCoinName(tx.chainID);
            msg.append("\n").append("Fee: ").setForegroundColor(titleColor)
                    .append(FmtMicrometer.fmtFeeValue(tx.fee))
                    .append(" ").append(coinName)
                    .append("\n").append("From: ").setForegroundColor(titleColor)
                    .append(HashUtil.hashMiddleHide(tx.senderPk));
            if (tx.txStatus == 1) {
                msg.append("\n").append("Blocknumber: ").setForegroundColor(titleColor)
                        .append(FmtMicrometer.fmtLong(tx.blockNumber));
            }
        }
        return msg.create();
    }

    private static SpannableStringBuilder createSpanAirdropTx(Tx tx, int tab) {
        Context context = MainApplication.getInstance();
        int titleColor = context.getResources().getColor(R.color.gray_dark);
        SpanUtils msg = new SpanUtils();
        if ((tab == CommunityTabFragment.TAB_CHAIN ||
                tab == CommunityTabFragment.TAB_MARKET) && tx.txStatus == 1) {
            msg.append("Status: ").setForegroundColor(titleColor)
                    .append(context.getString(R.string.community_block_on_chain))
                    .append("\n");
        }
        msg.append("Link: ").setForegroundColor(titleColor)
                    .append(tx.link);
        if (StringUtil.isNotEmpty(tx.memo)) {
            msg.append("\n").append("Description: ").setForegroundColor(titleColor)
                    .append(tx.memo);
        }

        if (tab == CommunityTabFragment.TAB_CHAIN) {
            String coinName = ChainIDUtil.getCoinName(tx.chainID);
            msg.append("\n").append("Fee: ").setForegroundColor(titleColor)
                    .append(FmtMicrometer.fmtFeeValue(tx.fee))
                    .append(" ").append(coinName)
                    .append("\n").append("From: ").setForegroundColor(titleColor)
                    .append(HashUtil.hashMiddleHide(tx.senderPk));
            if (tx.txStatus == 1) {
                msg.append("\n").append("Blocknumber: ").setForegroundColor(titleColor)
                        .append(FmtMicrometer.fmtLong(tx.blockNumber));
            }
        }
        return msg.create();
    }

    private static SpannableStringBuilder createSpanLeaderTx(Tx tx, int tab) {
        Context context = MainApplication.getInstance();
        int titleColor = context.getResources().getColor(R.color.gray_dark);
        SpanUtils msg = new SpanUtils();
        if ((tab == CommunityTabFragment.TAB_CHAIN ||
                tab == CommunityTabFragment.TAB_MARKET) && tx.txStatus == 1) {
            msg.append("Status: ").setForegroundColor(titleColor)
                    .append(context.getString(R.string.community_block_on_chain))
                    .append("\n");
        }
        msg.append("Title: ").setForegroundColor(titleColor)
            .append(tx.coinName).setForegroundColor(titleColor);
        if (StringUtil.isNotEmpty(tx.memo)) {
            msg.append("\n").append("Description: ").setForegroundColor(titleColor)
                    .append(tx.memo);
        }

        if (tab == CommunityTabFragment.TAB_CHAIN) {
            String coinName = ChainIDUtil.getCoinName(tx.chainID);
            msg.append("\n").append("Fee: ").setForegroundColor(titleColor)
                    .append(FmtMicrometer.fmtFeeValue(tx.fee))
                    .append(" ").append(coinName)
                    .append("\n").append("From: ").setForegroundColor(titleColor)
                    .append(HashUtil.hashMiddleHide(tx.senderPk));
            if (tx.txStatus == 1) {
                msg.append("\n").append("Blocknumber: ").setForegroundColor(titleColor)
                        .append(FmtMicrometer.fmtLong(tx.blockNumber));
            }
        }
        return msg.create();
    }

    public static SpannableStringBuilder createSpanTxQueue(TxQueueAndStatus tx, boolean isShowNonce) {
        return createSpanTxQueue(tx, tx.nonce, isShowNonce, null);
    }

    public static SpannableStringBuilder createSpanTxQueue(TxQueue tx, QueueOperation operation) {
        return createSpanTxQueue(tx, 0, false, operation);
    }

    private static SpannableStringBuilder createSpanTxQueue(TxQueue tx, long nonce, boolean isShowNonce,
                                                            QueueOperation operation) {
        String coinName = ChainIDUtil.getCoinName(tx.chainID);
        SpanUtils msg = new SpanUtils();
        if (tx.content != null) {
            TxContent txContent = new TxContent(tx.content);
            int txType = txContent.getType();
            if (operation != null) {
                msg.append("Status: ");
                if (operation == QueueOperation.INSERT) {
                    msg.append("Added");
                } else if (operation == QueueOperation.UPDATE) {
                    msg.append("Modified");
                } else {
                    msg.append("Deleted");
                }
                msg.append("\n").append("Community: ")
                    .append(ChainIDUtil.getName(tx.chainID))
                    .append("\n");
            }
            if (txType == TxType.WIRING_TX.getType()) {
                msg.append("Amount: ").append(FmtMicrometer.fmtBalance(tx.amount))
                        .append(" ").append(coinName)
                        .append("\n");
            }
            msg.append("Fee: ").append(FmtMicrometer.fmtFeeValue(tx.fee))
                    .append(" ").append(coinName);
            if (isShowNonce && nonce > 0) {
                msg.append("\n").append("Nonce: ").append(FmtMicrometer.fmtLong(nonce));
            }
            if (txType == TxType.WIRING_TX.getType()) {
                if (null == operation) {
                    msg.append("\n").append("To: ").append(HashUtil.hashMiddleHide(tx.receiverPk));
                }
                if (StringUtil.isNotEmpty(txContent.getMemo())) {
                    msg.append("\n").append("Description: ").append(txContent.getMemo());
                } else if (StringUtil.isNotEmpty(tx.memo)) {
                    msg.append("\n").append("Description: ").append(tx.memo);
                }
            } else if (txType == TxType.AIRDROP_TX.getType()) {
                AirdropTxContent content = new AirdropTxContent(tx.content);
                msg.append("\n").append("Link: ").append(content.getLink());
                if (StringUtil.isNotEmpty(txContent.getMemo())) {
                    msg.append("\n").append("Description: ").append(txContent.getMemo());
                }
            } else if (txType == TxType.ANNOUNCEMENT.getType()) {
                AnnouncementContent content = new AnnouncementContent(tx.content);
                msg.append("\n").append("Title: ").append(content.getTitle());
                if (StringUtil.isNotEmpty(txContent.getMemo())) {
                    msg.append("\n").append("Description: ").append(txContent.getMemo());
                }
            } else if (txType == TxType.SELL_TX.getType()) {
                SellTxContent content = new SellTxContent(tx.content);
                msg.append("\n").append("Selling: ")
                        .append(content.getCoinName())
                        .append("\n").append("Quantity: ");
                if (content.getQuantity() > 0) {
                    msg.append(String.valueOf(content.getQuantity()));
                } else {
                    msg.append("TBD");
                }
                if (StringUtil.isNotEmpty(content.getLink())) {
                    msg.append("\n").append("Link: ").append(content.getLink());
                }
                if (StringUtil.isNotEmpty(content.getLocation())) {
                    msg.append("\n").append("Location: ").append(content.getLocation());
                }
                if (StringUtil.isNotEmpty(content.getMemo())) {
                    msg.append("\n").append("Description: ").append(content.getMemo());
                }
            } else if (txType == TxType.TRUST_TX.getType()) {
                TrustContent trustContent = new TrustContent(tx.content);
                msg.append("\n").append("Trust: ").append(HashUtil.hashMiddleHide(trustContent.getTrustedPkStr()));
            }
        }
        return msg.create();
    }

    private static SpannableStringBuilder createSpanTrustTx(Tx tx, int tab) {
        Context context = MainApplication.getInstance();
        int titleColor = context.getResources().getColor(R.color.gray_dark);
        SpanUtils msg = new SpanUtils();
        if (tab == CommunityTabFragment.TAB_CHAIN && tx.txStatus == 1) {
            msg.append("Status: ").setForegroundColor(titleColor)
                    .append(context.getString(R.string.community_block_on_chain))
                    .append("\n");
        }
        msg.append("Trust: ")
                .setForegroundColor(titleColor)
                .append(HashUtil.hashMiddleHide(tx.receiverPk));
        if (tab == CommunityTabFragment.TAB_CHAIN) {
            String coinName = ChainIDUtil.getCoinName(tx.chainID);
            msg.append("\n").append("Fee: ").setForegroundColor(titleColor)
                    .append(FmtMicrometer.fmtFeeValue(tx.fee))
                    .append(" ").append(coinName)
                    .append("\n").append("From: ").setForegroundColor(titleColor)
                    .append(HashUtil.hashMiddleHide(tx.senderPk));
            if (tx.txStatus == 1) {
                msg.append("\n").append("Blocknumber: ").setForegroundColor(titleColor)
                        .append(FmtMicrometer.fmtLong(tx.blockNumber));
            }
        }
        return msg.create();
    }

    public static SpannableStringBuilder createBlockSpan(BlockAndTx block, boolean isShowTx) {
        Context context = MainApplication.getInstance();
        int titleColor = context.getResources().getColor(R.color.gray_dark);
        SpanUtils msg = new SpanUtils();
        if (isShowTx) {
            msg.append(context.getString(R.string.community_tip_block_txs))
                .setForegroundColor(titleColor);
            if (block.tx != null) {
                msg.append(HashUtil.hashMiddleHide(block.tx.txID));
            } else {
                msg.append(context.getString(R.string.community_tip_block_txs_nothing));
            }
            msg.append("\n");
        }
        msg.append(context.getString(R.string.community_tip_block_timestamp))
                .setForegroundColor(titleColor)
                .append(DateUtil.formatTime(block.timestamp, DateUtil.pattern6))
                .append("\n").append(context.getString(R.string.community_tip_block_miner))
                .setForegroundColor(titleColor)
                .append(HashUtil.hashMiddleHide(block.miner))
                .append("\n").append(context.getString(R.string.community_tip_block_reward))
                .setForegroundColor(titleColor)
                .append(FmtMicrometer.fmtBalance(block.rewards))
                .append(" ")
                .append(ChainIDUtil.getCoinName(block.chainID));
         if (StringUtil.isNotEmpty(block.previousBlockHash)) {
             msg.append("\n").append(context.getString(R.string.community_tip_previous_hash))
                     .setForegroundColor(titleColor)
                     .append(HashUtil.hashMiddleHide(block.previousBlockHash));
         }
        msg.append("\n").append(context.getString(R.string.community_tip_block_hash))
                .setForegroundColor(titleColor)
                .append(HashUtil.hashMiddleHide(block.blockHash))
                .append("\n").append(context.getString(R.string.community_tip_block_difficulty))
                .setForegroundColor(titleColor)
                .append(FmtMicrometer.fmtDecimal(block.difficulty));
        return msg.create();
    }

    public static SpannableStringBuilder createBlockSpan(Block block) {
        Transaction tx = block.getTx();
        byte[] payload = tx.getPayload();

        boolean isHaveTx = payload != null && payload.length > 0;
        String blockReward = FmtMicrometer.fmtBalance(tx.getFee());
        if (block.getBlockNumber() <= 0) {
            blockReward = FmtMicrometer.fmtBalance(block.getMinerBalance());
        }
        String chainID = ChainIDUtil.decode(block.getChainID());
        blockReward += " " + ChainIDUtil.getCoinName(chainID);

        Context context = MainApplication.getInstance();
        int titleColor = context.getResources().getColor(R.color.gray_dark);
        SpanUtils msg = new SpanUtils()
                .append(context.getString(R.string.community_tip_block_txs))
                .setForegroundColor(titleColor);
        if (isHaveTx) {
            msg.append(HashUtil.hashMiddleHide(tx.getTxID().to_hex()));
        } else {
            msg.append(context.getString(R.string.community_tip_block_txs_nothing));
        }
        msg.append("\n").append(context.getString(R.string.community_tip_block_timestamp))
                .setForegroundColor(titleColor)
                .append(DateUtil.formatTime(block.getTimestamp(), DateUtil.pattern6))
                .append("\n").append(context.getString(R.string.community_tip_block_miner))
                .setForegroundColor(titleColor)
                .append(HashUtil.hashMiddleHide(ByteUtil.toHexString(block.getMiner())))
                .append("\n").append(context.getString(R.string.community_tip_block_reward))
                .setForegroundColor(titleColor)
                .append(blockReward);
        if (block.getPreviousBlockHash() != null) {
            String previousHash = ByteUtil.toHexString(block.getPreviousBlockHash());
            msg.append("\n").append(context.getString(R.string.community_tip_previous_hash))
                    .setForegroundColor(titleColor)
                    .append(HashUtil.hashMiddleHide(previousHash));
        }
        msg.append("\n").append(context.getString(R.string.community_tip_block_hash))
                .setForegroundColor(titleColor)
                .append(HashUtil.hashMiddleHide(block.Hash()))
                .append("\n").append(context.getString(R.string.community_tip_block_difficulty))
                .setForegroundColor(titleColor)
                .append(FmtMicrometer.fmtDecimal(block.getCumulativeDifficulty().longValue()));
        return msg.create();
    }
}
