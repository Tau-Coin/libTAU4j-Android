package io.taucoin.tauapp.publishing.core.model.data.message;

import io.taucoin.tauapp.publishing.core.utils.Utils;
import io.taucoin.tauapp.publishing.core.utils.rlp.RLP;
import io.taucoin.tauapp.publishing.core.utils.rlp.RLPList;

/**
 * 交易内容类
 */
public class AirdropTxContent extends TxContent {
    private byte[] link;                    // link信息

    public AirdropTxContent(String link, String description) {
        super(TxType.AIRDROP_TX.getType(), Utils.textStringToBytes(description));
        this.link = Utils.textStringToBytes(link);
    }

    public AirdropTxContent(byte[] encode) {
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
        this.type = RLP.decodeInteger(messageList, 1, TxType.AIRDROP_TX.getType());
        this.memo = RLP.decodeElement(messageList, 2);
        this.link = RLP.decodeElement(messageList, 3);

    }

    @Override
    public byte[] getEncoded() {
        byte[] version = RLP.encodeInteger(this.version);
        byte[] type = RLP.encodeInteger(this.type);
        byte[] memo = RLP.encodeElement(this.memo);
        byte[] link = RLP.encodeElement(this.link);

       return RLP.encodeList(version, type, memo, link);
    }

    public String getLink() {
        if (link != null) {
            return Utils.toUTF8String(link);
        }
        return null;
    }
}
