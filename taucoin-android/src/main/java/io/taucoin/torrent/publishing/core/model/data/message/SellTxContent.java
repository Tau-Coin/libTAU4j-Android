package io.taucoin.torrent.publishing.core.model.data.message;

import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.rlp.RLP;
import io.taucoin.torrent.publishing.core.utils.rlp.RLPList;

/**
 * 交易内容类
 */
public class SellTxContent {
    private int version;                    // 标识消息版本
    private int type;                       // 可以标识消息类型
    private byte[] coinName;                // 币名
    private byte[] link;                    // link信息
    private byte[] location;                // 位置
    private byte[] description;             // 描述

    public SellTxContent(String coinName, String link, String location, String description) {
        this.version = TxVersion.VERSION1.getV();
        this.type = TxType.SELL_TX.getType();
        this.coinName = Utils.textStringToBytes(coinName);
        this.link = Utils.textStringToBytes(link);
        this.location = Utils.textStringToBytes(location);
        this.description = Utils.textStringToBytes(description);
    }

    public SellTxContent(byte[] encode) {
        if (encode != null) {
            parseRLP(encode);
        }
    }

    private void parseRLP(byte[] encode) {
        RLPList params = RLP.decode2(encode);
        RLPList messageList = (RLPList) params.get(0);

        this.version = RLP.decodeInteger(messageList, 0, TxVersion.VERSION1.getV());
        this.type = RLP.decodeInteger(messageList, 1, TxType.SELL_TX.getType());
        this.coinName = RLP.decodeElement(messageList, 2);
        this.link = RLP.decodeElement(messageList, 3);
        this.location = RLP.decodeElement(messageList, 4);
        this.description = RLP.decodeElement(messageList, 5);
    }

    public byte[] getEncoded() {
        byte[] version = RLP.encodeInteger(this.version);
        byte[] type = RLP.encodeInteger(this.type);
        byte[] coinName = RLP.encodeElement(this.coinName);
        byte[] link = RLP.encodeElement(this.link);
        byte[] location = RLP.encodeElement(this.location);
        byte[] description = RLP.encodeElement(this.description);

       return RLP.encodeList(version, type, coinName, link, location, description);
    }

    public int getVersion() {
        return version;
    }

    public int getType() {
        return type;
    }

    public byte[] getCoinName() {
        return coinName;
    }

    public byte[] getLink() {
        return link;
    }

    public byte[] getLocation() {
        return location;
    }

    public byte[] getDescription() {
        return description;
    }
}
