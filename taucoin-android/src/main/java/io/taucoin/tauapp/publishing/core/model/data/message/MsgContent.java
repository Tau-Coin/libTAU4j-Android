package io.taucoin.tauapp.publishing.core.model.data.message;

import io.taucoin.tauapp.publishing.core.utils.rlp.RLP;
import io.taucoin.tauapp.publishing.core.utils.rlp.RLPList;

/**
 * 消息内容类
 */
public class MsgContent {
    private int version;                    // 标识消息版本
    private String logicHash;               // 用于确认区分逻辑消息，带时间戳（可能连发两次）
    private int type;                       // 可以标识消息类型
    private byte[] content;                 // 原始消息体
    private String airdropChain;            // 发币的链
    private String referralPeer;            // airdrop推荐节点

    public static MsgContent createContent(String logicMsgHash, int type, byte[] content, String airdropChain, String referralPeer) {
        return new MsgContent(MessageVersion.VERSION2.getV(), logicMsgHash, type, content, airdropChain, referralPeer);
    }

    private MsgContent(int version, String logicMsgHash, int type, byte[] content, String airdropChain, String referralPeer) {
        this.version = version;
        this.logicHash = logicMsgHash;
        this.type = type;
        this.content = content;
        this.airdropChain = airdropChain;
        this.referralPeer = referralPeer;
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
        if (this.version == MessageVersion.VERSION2.getV() && messageList.size() > 5)  {
            this.referralPeer = RLP.decodeString(messageList, 5, null);
        }
    }

    public byte[] getEncoded() {
        byte[] version = RLP.encodeInteger(this.version);
        byte[] logicHash = RLP.encodeString(this.logicHash);
        byte[] type = RLP.encodeInteger(this.type);
        byte[] content = RLP.encodeElement(this.content);
        byte[] airdropChain = RLP.encodeString(this.airdropChain);
        byte[] referralPeer = RLP.encodeString(this.referralPeer);

       return RLP.encodeList(version, logicHash, type, content, airdropChain, referralPeer);
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

    public String getReferralPeer() {
        return referralPeer;
    }
}
