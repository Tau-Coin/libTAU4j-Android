package io.taucoin.torrent.publishing.core.model.data.message;

import io.taucoin.torrent.publishing.core.utils.rlp.RLP;
import io.taucoin.torrent.publishing.core.utils.rlp.RLPList;

/**
 * 交易内容类
 */
public class TxContent {
    private int version;                    // 标识消息版本
    private int type;                       // 可以标识消息类型
    private byte[] content;                 // 原始消息体

    public TxContent(int type, byte[] content) {
        this.version = TxVersion.VERSION1.getV();
        this.type = type;
        this.content = content;
    }

    private TxContent(int version, int type, byte[] content) {
        this.version = version;
        this.type = type;
        this.content = content;
    }

    public TxContent(byte[] encode) {
        if (encode != null) {
            parseRLP(encode);
        }
    }

    private void parseRLP(byte[] encode) {
        RLPList params = RLP.decode2(encode);
        RLPList messageList = (RLPList) params.get(0);

        this.version = RLP.decodeInteger(messageList, 0, TxVersion.VERSION1.getV());
        this.type = RLP.decodeInteger(messageList, 1, TxType.WRING_TX.getType());
        this.content = RLP.decodeElement(messageList, 2);
    }

    public byte[] getEncoded() {
        byte[] version = RLP.encodeInteger(this.version);
        byte[] type = RLP.encodeInteger(this.type);
        byte[] content = RLP.encodeElement(this.content);

       return RLP.encodeList(version, type, content);
    }

    public int getVersion() {
        return version;
    }

    public int getType() {
        return type;
    }

    public byte[] getContent() {
        return content;
    }
}
