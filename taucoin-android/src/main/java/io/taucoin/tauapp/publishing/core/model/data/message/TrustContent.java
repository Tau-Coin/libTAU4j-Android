package io.taucoin.tauapp.publishing.core.model.data.message;

import io.taucoin.tauapp.publishing.core.utils.Utils;
import io.taucoin.tauapp.publishing.core.utils.rlp.ByteUtil;
import io.taucoin.tauapp.publishing.core.utils.rlp.RLP;
import io.taucoin.tauapp.publishing.core.utils.rlp.RLPList;

/**
 * 交易内容类
 */
public class TrustContent extends TxContent {
    private byte[] trustedPk;

    public TrustContent(String memo, String trustedPk) {
        super(TxType.TRUST_TX.getType(), Utils.textStringToBytes(memo));
        this.trustedPk = ByteUtil.toByte(trustedPk);
    }

    public TrustContent(byte[] encode) {
        super(encode);

        if (encode != null) {
            parseRLP(encode);
        }
    }

    @Override
    public void parseRLP(byte[] encode) {
        RLPList params = RLP.decode2(encode);
        RLPList messageList = (RLPList) params.get(0);

        this.version = RLP.decodeInteger(messageList, 0, TxVersion.VERSION1.getV());
        this.type = RLP.decodeInteger(messageList, 1, TxType.SELL_TX.getType());
        this.memo = RLP.decodeElement(messageList, 2);
        this.trustedPk = RLP.decodeElement(messageList, 3);

    }

    @Override
    public byte[] getEncoded() {
        byte[] version = RLP.encodeInteger(this.version);
        byte[] type = RLP.encodeInteger(this.type);
        byte[] memo = RLP.encodeElement(this.memo);
        byte[] trustedPk = RLP.encodeElement(this.trustedPk);

       return RLP.encodeList(version, type, memo, trustedPk);
    }

    public String getTrustedPkStr() {
        if (trustedPk != null) {
            return ByteUtil.toHexString(trustedPk);
        }
        return null;
    }

    public byte[] getTrustedPk() {
        return trustedPk;
    }
}
