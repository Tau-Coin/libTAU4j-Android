package io.taucoin.torrent.publishing.core.model.data.message;

import java.math.BigInteger;

public class Message {
    private Integer version;                // 标识消息版本
    private BigInteger timestamp;           // 消息时间（可省略，直接使用libTAU内部创建的时间）
    private String sender;                  // 消息的发送者
    private String receiver;                // 消息的接收者
    private String logicHash;               // 用于确认区分逻辑消息，带时间戳（可能连发两次）
    private BigInteger nonce;               // 局部nonce，用于标识切分消息的顺序
    private Integer type;                   // 可以标识消息类型
    private byte[] content;                 // 加密消息体

    public static Message createTextMessage(long timestamp, String sender, String receiver,
                                            String logicMsgHash, long nonce, byte[] content) {
        return new Message(MessageVersion.VERSION1.getV(), BigInteger.valueOf(timestamp), sender,
                receiver, logicMsgHash, BigInteger.valueOf(nonce), MessageType.TEXT.getType(), content);
    }

    public Message(Integer version, BigInteger timestamp, String sender, String receiver,
                   String logicMsgHash, BigInteger nonce, Integer type, byte[] content) {
        this.version = version;
        this.timestamp = timestamp;
        this.sender = sender;
        this.receiver = receiver;
        this.logicHash = logicMsgHash;
        this.nonce = nonce;
        this.type = type;
        this.content = content;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public BigInteger getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(BigInteger timestamp) {
        this.timestamp = timestamp;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getLogicHash() {
        return logicHash;
    }

    public void setLogicHash(String logicHash) {
        this.logicHash = logicHash;
    }

    public BigInteger getNonce() {
        return nonce;
    }

    public void setNonce(BigInteger nonce) {
        this.nonce = nonce;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
