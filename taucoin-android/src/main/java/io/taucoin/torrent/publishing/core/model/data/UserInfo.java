package io.taucoin.torrent.publishing.core.model.data;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.rlp.RLP;
import io.taucoin.torrent.publishing.core.utils.rlp.RLPList;

public class UserInfo {
    private byte[] deviceID;
    private byte[] nickname;
    private BigInteger onlineTime;
    private BigInteger updateNNTime;
    private double longitude;
    private double latitude;
    private BigInteger updateLocationTime;
    private byte[] profile;
    private BigInteger updateProfileTime;
    private final List<byte[]> communities = new ArrayList<>();

    private byte[] rlpEncoded;          // 编码数据

    public UserInfo(String deviceID, User friend, List<String> communities, long onlineTime) {
        this.deviceID = Utils.textStringToBytes(deviceID);
        this.onlineTime = BigInteger.valueOf(onlineTime);
        this.nickname = Utils.textStringToBytes(friend.nickname);
        this.updateNNTime = BigInteger.valueOf(friend.updateNNTime);
        this.longitude = friend.longitude;
        this.latitude = friend.latitude;
        this.updateLocationTime = BigInteger.valueOf(friend.updateLocationTime);
        this.profile = Utils.textStringToBytes(friend.profile);
        this.updateProfileTime = BigInteger.valueOf(friend.updatePFTime);
        if (communities != null && communities.size() > 0) {
            for (String community : communities) {
                this.communities.add(ChainIDUtil.encode(community));
            }
        }
    }

    public UserInfo(byte[] rlpEncoded) {
        if (rlpEncoded != null) {
            this.rlpEncoded = rlpEncoded;
            parseRLP();
        }
    }

    public byte[] getNickname() {
        return nickname;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public byte[] getDeviceID() {
        return deviceID;
    }

    public BigInteger getUpdateNNTime() {
        return updateNNTime;
    }

    public BigInteger getUpdateLocationTime() {
        return updateLocationTime;
    }

    public List<byte[]> getCommunities() {
        return communities;
    }

    public BigInteger getOnlineTime() {
        return onlineTime;
    }

    public byte[] getProfile() {
        return profile;
    }

    public BigInteger getUpdateProfileTime() {
        return updateProfileTime;
    }

    /**
     * parse rlp encode
     */
    public void parseRLP() {
        RLPList params = RLP.decode2(this.rlpEncoded);
        RLPList list = (RLPList) params.get(0);

        this.deviceID = list.get(0).getRLPData();
        byte[] onlineTimeBytes = list.get(1).getRLPData();
        this.onlineTime = (null == onlineTimeBytes) ? BigInteger.ZERO: new BigInteger(1, onlineTimeBytes);
        this.nickname = list.get(2).getRLPData();
        byte[] nnTimeBytes = list.get(3).getRLPData();
        this.updateNNTime = (null == nnTimeBytes) ? BigInteger.ZERO: new BigInteger(1, nnTimeBytes);
        this.longitude = RLP.decodeDouble(list, 4, 0);
        this.latitude = RLP.decodeDouble(list, 5, 0);
        byte[] locationTimeBytes = list.get(6).getRLPData();
        this.updateLocationTime = (null == locationTimeBytes) ? BigInteger.ZERO: new BigInteger(1, locationTimeBytes);
        this.profile = list.get(7).getRLPData();
        byte[] profileTimeBytes = list.get(8).getRLPData();
        this.updateProfileTime = (null == profileTimeBytes) ? BigInteger.ZERO: new BigInteger(1, profileTimeBytes);

        byte[] communitiesBytes = list.get(9).getRLPData();
        if (communitiesBytes != null) {
            RLPList paramsList = RLP.decode2(communitiesBytes);
            RLPList cList = (RLPList) paramsList.get(0);
            if (cList != null) {
                for (int i = 0; i < cList.size(); i++) {
                    this.communities.add(cList.get(i).getRLPData());
                }
            }
        }
    }

    /**
     * get encoded hash list
     * @return encode
     */
    public byte[] getEncoded() {
        if (null == rlpEncoded) {
            byte[] deviceID = RLP.encodeElement(this.deviceID);
            byte[] onlineTime = RLP.encodeBigInteger(this.onlineTime);
            byte[] nickname = RLP.encodeElement(this.nickname);
            byte[] updateNNTime = RLP.encodeBigInteger(this.updateNNTime);
            byte[] longitude = RLP.encodeDouble(this.longitude);
            byte[] latitude = RLP.encodeDouble(this.latitude);
            byte[] updateLocationTime = RLP.encodeBigInteger(this.updateLocationTime);
            byte[] profile = RLP.encodeElement(this.profile);
            byte[] updateProfileTime = RLP.encodeBigInteger(this.updateProfileTime);
            byte[] communities = RLP.encodeList(this.communities);

            this.rlpEncoded = RLP.encodeList(deviceID, onlineTime, nickname, updateNNTime, longitude,
                    latitude, updateLocationTime, profile, updateProfileTime, communities);
        }
        return rlpEncoded;
    }
}
