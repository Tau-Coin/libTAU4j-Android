package io.taucoin.torrent.publishing.core.model.data;

import java.math.BigInteger;
import java.util.Arrays;

import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.rlp.ByteUtil;
import io.taucoin.torrent.publishing.core.utils.rlp.RLP;
import io.taucoin.torrent.publishing.core.utils.rlp.RLPList;

public class FriendInfo {
    private byte[] deviceID;
    private byte[] pubKey;
    private byte[] nickname;
    private BigInteger updateNNTime;
    private byte[] headPic;
    private BigInteger updateHPTime;
    private double longitude;
    private double latitude;
    private BigInteger updateLocationTime;

    private byte[] rlpEncoded; // 编码数据
    private boolean parsed = false; // 解析标志

    public FriendInfo(String deviceID, User friend) {
        this.deviceID = Utils.textStringToBytes(deviceID);
        this.pubKey = ByteUtil.toByte(friend.publicKey);
        this.nickname = Utils.textStringToBytes(friend.nickname);
        this.updateNNTime = BigInteger.valueOf(friend.updateNNTime);
        this.headPic = friend.headPic;
        this.updateHPTime = BigInteger.valueOf(friend.updateHPTime);
        this.longitude = friend.longitude;
        this.latitude = friend.latitude;
        this.updateLocationTime = BigInteger.valueOf(friend.updateLocationTime);

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

    public byte[] getHeadPic() {
        if (!parsed) {
            parseRLP();
        }

        return headPic;
    }

    public double getLongitude() {
        if (!parsed) {
            parseRLP();
        }

        return longitude;
    }

    public double getLatitude() {
        if (!parsed) {
            parseRLP();
        }

        return latitude;
    }

    public byte[] getDeviceID() {
        if (!parsed) {
            parseRLP();
        }

        return deviceID;
    }

    public BigInteger getUpdateNNTime() {
        if (!parsed) {
            parseRLP();
        }

        return updateNNTime;
    }

    public BigInteger getUpdateHPTime() {
        if (!parsed) {
            parseRLP();
        }

        return updateHPTime;
    }

    public BigInteger getUpdateLocationTime() {
        if (!parsed) {
            parseRLP();
        }

        return updateLocationTime;
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
        byte[] nnTimeBytes = list.get(3).getRLPData();
        this.updateNNTime = (null == nnTimeBytes) ? BigInteger.ZERO: new BigInteger(1, nnTimeBytes);
        this.headPic = list.get(4).getRLPData();
        byte[] hpTimeBytes = list.get(5).getRLPData();
        this.updateHPTime = (null == hpTimeBytes) ? BigInteger.ZERO: new BigInteger(1, hpTimeBytes);
        this.longitude = RLP.decodeDouble(list, 6, 0);
        this.latitude = RLP.decodeDouble(list, 7, 0);
        byte[] locationTimeBytes = list.get(8).getRLPData();
        this.updateLocationTime = (null == locationTimeBytes) ? BigInteger.ZERO: new BigInteger(1, locationTimeBytes);

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
            byte[] updateNNTime = RLP.encodeBigInteger(this.updateNNTime);
            byte[] headPic = RLP.encodeElement(this.headPic);
            byte[] updateHPTime = RLP.encodeBigInteger(this.updateHPTime);
            byte[] longitude = RLP.encodeDouble(this.longitude);
            byte[] latitude = RLP.encodeDouble(this.latitude);
            byte[] updateLocationTime = RLP.encodeBigInteger(this.updateLocationTime);

            this.rlpEncoded = RLP.encodeList(deviceID, pubKey, nickname, updateNNTime, headPic, updateHPTime,
                    longitude, latitude, updateLocationTime);
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
