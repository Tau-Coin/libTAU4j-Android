package io.taucoin.torrent.publishing.core.model.data.message;

import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.rlp.RLP;
import io.taucoin.torrent.publishing.core.utils.rlp.RLPList;

/**
 * 交易内容类
 */
public class AirdropTxContent extends TxContent {
    private byte[] coinName;                // 币名
    private byte[] link;                    // link信息

    public AirdropTxContent(String coinName, String link, String description) {
        super(TxType.AIRDROP_TX.getType(), Utils.textStringToBytes(description));

        this.coinName = Utils.textStringToBytes(coinName);
        this.link = Utils.textStringToBytes(link);
    }

    public AirdropTxContent(byte[] encode) {
        super(encode);

        if (encode != null) {
            parseRLP(encode);
        }
    }

    @Override
    public void parseRLP(byte[] encode) {
        RLPList params = RLP.decode2(encode);
        RLPList messageList = (RLPList) params.get(0);

        this.version = RLP.decodeInteger(messageList, 0, TxVersion.VERSION1.getV());
        this.type = RLP.decodeInteger(messageList, 1, TxType.AIRDROP_TX.getType());
        this.memo = RLP.decodeElement(messageList, 2);
        this.coinName = RLP.decodeElement(messageList, 3);
        this.link = RLP.decodeElement(messageList, 4);

    }

    @Override
    public byte[] getEncoded() {
        byte[] version = RLP.encodeInteger(this.version);
        byte[] type = RLP.encodeInteger(this.type);
        byte[] memo = RLP.encodeElement(this.memo);
        byte[] coinName = RLP.encodeElement(this.coinName);
        byte[] link = RLP.encodeElement(this.link);

       return RLP.encodeList(version, type, memo, coinName, link);
    }

    public byte[] getCoinName() {
        return coinName;
    }

    public byte[] getLink() {
        return link;
    }
}
