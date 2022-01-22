package io.taucoin.torrent.publishing.core.model.data.message;

import io.taucoin.torrent.publishing.core.utils.rlp.RLP;
import io.taucoin.torrent.publishing.core.utils.rlp.RLPList;

/**
 * 交易内容类
 */
public class TxContent {
    protected int version;                    // 标识消息版本
    protected int type;                       // 可以标识消息类型
    protected byte[] memo;                      // 原始消息体

    public TxContent(int type, byte[] memo) {
        this.version = TxVersion.VERSION1.getV();
        this.type = type;
        this.memo = memo;
    }

    private TxContent(int version, int type, byte[] memo) {
        this.version = version;
        this.type = type;
        this.memo = memo;
    }

    public TxContent(byte[] encode) {
        if (encode != null) {
            parseRLP(encode);
        }
    }

    public void parseRLP(byte[] encode) {
        RLPList params = RLP.decode2(encode);
        RLPList messageList = (RLPList) params.get(0);

        this.version = RLP.decodeInteger(messageList, 0, TxVersion.VERSION1.getV());
        this.type = RLP.decodeInteger(messageList, 1, TxType.WIRING_TX.getType());
        this.memo = RLP.decodeElement(messageList, 2);
    }

    public byte[] getEncoded() {
        byte[] version = RLP.encodeInteger(this.version);
        byte[] type = RLP.encodeInteger(this.type);
        byte[] memo = RLP.encodeElement(this.memo);

       return RLP.encodeList(version, type, memo);
    }

    public int getVersion() {
        return version;
    }

    public int getType() {
        return type;
    }

    public byte[] getContent() {
        return memo;
    }
}
