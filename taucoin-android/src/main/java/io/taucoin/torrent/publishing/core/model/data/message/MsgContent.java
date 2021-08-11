package io.taucoin.torrent.publishing.core.model.data.message;

import io.taucoin.torrent.publishing.core.utils.rlp.RLP;
import io.taucoin.torrent.publishing.core.utils.rlp.RLPList;

/**
 * 消息内容类
 */
public class MsgContent {
    private int version;                    // 标识消息版本
    private String logicHash;               // 用于确认区分逻辑消息，带时间戳（可能连发两次）
    private long nonce;                     // 局部nonce，用于标识切分消息的顺序
    private int type;                       // 可以标识消息类型
    private byte[] content;                 // 加密消息体

    public static MsgContent createTextContent(String logicMsgHash, long nonce, byte[] content) {
        return new MsgContent(MessageVersion.VERSION1.getV(), logicMsgHash, nonce, MessageType.TEXT.getType(), content);
    }

    private MsgContent(int version, String logicMsgHash, long nonce, int type, byte[] content) {
        this.version = version;
        this.logicHash = logicMsgHash;
        this.nonce = nonce;
        this.type = type;
        this.content = content;
    }

    public MsgContent(byte[] encode) {
        if (encode != null) {
            parseRLP(encode);
        }
    }

    private void parseRLP(byte[] encode) {
        RLPList params = RLP.decode2(encode);
        RLPList messageList = (RLPList) params.get(0);

        this.version = RLP.decodeInteger(messageList, 0, MessageVersion.VERSION1.getV());
        this.logicHash = RLP.decodeString(messageList, 1, null);
        this.nonce = RLP.decodeLong(messageList, 2, 0);
        this.type = RLP.decodeInteger(messageList, 3, MessageType.TEXT.getType());
        this.content = RLP.decodeElement(messageList, 4);
    }

    public byte[] getEncoded() {
        byte[] version = RLP.encodeInteger(this.version);
        byte[] logicHash = RLP.encodeString(this.logicHash);
        byte[] nonce = RLP.encodeLong(this.nonce);
        byte[] type = RLP.encodeInteger(this.type);
        byte[] content = RLP.encodeElement(this.content);

       return RLP.encodeList(version, logicHash, nonce, type, content);
    }

    public int getVersion() {
        return version;
    }

    public String getLogicHash() {
        return logicHash;
    }

    public long getNonce() {
        return nonce;
    }

    public int getType() {
        return type;
    }

    public byte[] getContent() {
        return content;
    }
}
