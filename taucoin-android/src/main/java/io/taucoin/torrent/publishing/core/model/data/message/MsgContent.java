package io.taucoin.torrent.publishing.core.model.data.message;

import io.taucoin.torrent.publishing.core.utils.rlp.RLP;
import io.taucoin.torrent.publishing.core.utils.rlp.RLPList;

/**
 * 消息内容类
 */
public class MsgContent {
    private int version;                    // 标识消息版本
    private String logicHash;               // 用于确认区分逻辑消息，带时间戳（可能连发两次）
    private int type;                       // 可以标识消息类型
    private byte[] content;                 // 原始消息体
    private String airdropChain;            // 发币的链

    public static MsgContent createTextContent(String logicMsgHash, byte[] content, String airdropChain) {
        return new MsgContent(MessageVersion.VERSION1.getV(), logicMsgHash, MessageType.TEXT.getType(),
                content, airdropChain);
    }

    public static MsgContent createContent(String logicMsgHash, int type, byte[] content, String airdropChain) {
        return new MsgContent(MessageVersion.VERSION1.getV(), logicMsgHash, type, content, airdropChain);
    }

    private MsgContent(int version, String logicMsgHash, int type, byte[] content, String airdropChain) {
        this.version = version;
        this.logicHash = logicMsgHash;
        this.type = type;
        this.content = content;
        this.airdropChain = airdropChain;
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
        this.type = RLP.decodeInteger(messageList, 2, MessageType.TEXT.getType());
        this.content = RLP.decodeElement(messageList, 3);
        this.airdropChain = RLP.decodeString(messageList, 4, null);
    }

    public byte[] getEncoded() {
        byte[] version = RLP.encodeInteger(this.version);
        byte[] logicHash = RLP.encodeString(this.logicHash);
        byte[] type = RLP.encodeInteger(this.type);
        byte[] content = RLP.encodeElement(this.content);
        byte[] airdropChain = RLP.encodeString(this.airdropChain);

       return RLP.encodeList(version, logicHash, type, content, airdropChain);
    }

    public int getVersion() {
        return version;
    }

    public String getLogicHash() {
        return logicHash;
    }

    public int getType() {
        return type;
    }

    public byte[] getContent() {
        return content;
    }

    public String getAirdropChain() {
        return airdropChain;
    }
}
