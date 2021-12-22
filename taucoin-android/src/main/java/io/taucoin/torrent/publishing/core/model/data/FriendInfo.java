package io.taucoin.torrent.publishing.core.model.data;

import java.math.BigInteger;
import java.util.Arrays;

import io.taucoin.torrent.publishing.core.utils.rlp.RLP;
import io.taucoin.torrent.publishing.core.utils.rlp.RLPList;

public class FriendInfo {
    byte[] pubKey;
    byte[] nickname;
    byte[] remark;
    byte[] deviceID;
    byte[] avatar;
    BigInteger timestamp;

    private byte[] rlpEncoded; // 编码数据
    private boolean parsed = false; // 解析标志

    public FriendInfo(byte[] deviceID, byte[] pubKey, byte[] nickname, byte[] remark, byte[] avatar,
                      BigInteger timestamp) {
        this.deviceID = deviceID;
        this.pubKey = pubKey;
        this.nickname = nickname;
        this.remark = remark;
        this.avatar = avatar;
        this.timestamp = timestamp;

        this.parsed = true;
    }

    public FriendInfo(byte[] rlpEncoded) {
        this.rlpEncoded = rlpEncoded;
    }

    public byte[] getPubKey() {
        if (!parsed) {
            parseRLP();
        }

        return pubKey;
    }

    public byte[] getNickname() {
        if (!parsed) {
            parseRLP();
        }

        return nickname;
    }

    public byte[] getRemark() {
        if (!parsed) {
            parseRLP();
        }

        return remark;
    }

    public byte[] getDeviceID() {
        if (!parsed) {
            parseRLP();
        }

        return deviceID;
    }

    public BigInteger getTimestamp() {
        if (!parsed) {
            parseRLP();
        }

        return timestamp;
    }

    /**
     * parse rlp encode
     */
    private void parseRLP() {
        RLPList params = RLP.decode2(this.rlpEncoded);
        RLPList list = (RLPList) params.get(0);

        this.deviceID = list.get(0).getRLPData();
        this.pubKey = list.get(1).getRLPData();
        this.nickname = list.get(2).getRLPData();
        this.remark = list.get(3).getRLPData();
        byte[] timeBytes = list.get(4).getRLPData();
        this.timestamp = (null == timeBytes) ? BigInteger.ZERO: new BigInteger(1, timeBytes);
        this.avatar = list.get(5).getRLPData();

        this.parsed = true;
    }

    /**
     * get encoded hash list
     * @return encode
     */
    public byte[] getEncoded(){
        if (null == rlpEncoded) {
            byte[] deviceID = RLP.encodeElement(this.deviceID);
            byte[] pubKey = RLP.encodeElement(this.pubKey);
            byte[] nickname = RLP.encodeElement(this.nickname);
            byte[] remark = RLP.encodeElement(this.remark);
            byte[] avatar = RLP.encodeElement(this.avatar);
            byte[] timestamp = RLP.encodeBigInteger(this.timestamp);

            this.rlpEncoded = RLP.encodeList(deviceID, pubKey, nickname, remark, avatar, timestamp);
        }

        return rlpEncoded;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FriendInfo that = (FriendInfo) o;
        return Arrays.equals(pubKey, that.pubKey);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(pubKey);
    }
}
