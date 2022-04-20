package io.taucoin.torrent.publishing.core.model.data.message;

import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.rlp.RLP;
import io.taucoin.torrent.publishing.core.utils.rlp.RLPList;

/**
 * 交易内容类
 */
public class SellTxContent extends TxContent {
    private byte[] coinName;                // 币名
    private long quantity;                // 币的数量
    private byte[] link;                    // link信息
    private byte[] location;                // 位置

    public SellTxContent(String coinName, long quantity, String link, String location, String description) {
        super(TxType.SELL_TX.getType(), Utils.textStringToBytes(description));

        this.coinName = Utils.textStringToBytes(coinName);
        this.quantity = quantity;
        this.link = Utils.textStringToBytes(link);
        this.location = Utils.textStringToBytes(location);
    }

    public SellTxContent(byte[] encode) {
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
        this.type = RLP.decodeInteger(messageList, 1, TxType.SELL_TX.getType());
        this.memo = RLP.decodeElement(messageList, 2);
        this.coinName = RLP.decodeElement(messageList, 3);
        this.link = RLP.decodeElement(messageList, 4);
        this.location = RLP.decodeElement(messageList, 5);
        this.quantity = RLP.decodeLong(messageList, 6, 0);

    }

    @Override
    public byte[] getEncoded() {
        byte[] version = RLP.encodeInteger(this.version);
        byte[] type = RLP.encodeInteger(this.type);
        byte[] memo = RLP.encodeElement(this.memo);
        byte[] coinName = RLP.encodeElement(this.coinName);
        byte[] link = RLP.encodeElement(this.link);
        byte[] location = RLP.encodeElement(this.location);
        byte[] quantity = RLP.encodeLong(this.quantity);

       return RLP.encodeList(version, type, memo, coinName, link, location, quantity);
    }

    public String getCoinName() {
        if (coinName != null) {
            return Utils.textBytesToString(coinName);
        }
        return null;
    }

    public String getLink() {
        if (link != null) {
            return Utils.textBytesToString(link);
        }
        return null;
    }

    public String getLocation() {
        if (location != null) {
            return Utils.textBytesToString(location);
        }
        return null;
    }

    public long getQuantity() {
        return quantity;
    }
}
