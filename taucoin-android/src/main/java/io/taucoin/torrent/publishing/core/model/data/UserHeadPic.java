package io.taucoin.torrent.publishing.core.model.data;

import java.math.BigInteger;
import io.taucoin.torrent.publishing.core.utils.rlp.RLP;
import io.taucoin.torrent.publishing.core.utils.rlp.RLPList;

public class UserHeadPic {
    private BigInteger updateHPTime;
    private byte[] headPic;

    private byte[] rlpEncoded;          // 编码数据

    public UserHeadPic(byte[] headPic, long updateHPTime) {
        this.updateHPTime = BigInteger.valueOf(updateHPTime);
        this.headPic = headPic;
    }

    public UserHeadPic(byte[] rlpEncoded) {
        if (rlpEncoded != null) {
            this.rlpEncoded = rlpEncoded;
            parseRLP();
        }
    }

    public BigInteger getUpdateHPTime() {
        return updateHPTime;
    }

    public byte[] getHeadPic() {
        return headPic;
    }

    /**
     * parse rlp encode
     */
    public void parseRLP() {
        RLPList params = RLP.decode2(this.rlpEncoded);
        RLPList list = (RLPList) params.get(0);

        byte[] hpTimeBytes = list.get(0).getRLPData();
        this.updateHPTime = (null == hpTimeBytes) ? BigInteger.ZERO: new BigInteger(1, hpTimeBytes);
        this.headPic = list.get(1).getRLPData();
    }

    /**
     * get encoded hash list
     * @return encode
     */
    public byte[] getEncoded(){
        if (null == rlpEncoded) {
            byte[] updateHPTime = RLP.encodeBigInteger(this.updateHPTime);
            byte[] headPic = RLP.encodeElement(this.headPic);

            this.rlpEncoded = RLP.encodeList(updateHPTime, headPic);
        }

        return rlpEncoded;
    }
}
