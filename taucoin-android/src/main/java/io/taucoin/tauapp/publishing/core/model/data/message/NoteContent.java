package io.taucoin.tauapp.publishing.core.model.data.message;

import io.taucoin.tauapp.publishing.core.utils.Utils;
import io.taucoin.tauapp.publishing.core.utils.rlp.ByteUtil;
import io.taucoin.tauapp.publishing.core.utils.rlp.RLP;
import io.taucoin.tauapp.publishing.core.utils.rlp.RLPList;

/**
 * 交易内容类
 */
public class NoteContent extends TxContent {
    private byte[]  link;
    private byte[] repliedHash;

    public NoteContent(String memo, String link, String repliedHash) {
        super(TxType.NOTE_TX.getType(), Utils.textStringToBytes(memo));
        this.link = Utils.textStringToBytes(link);
        this.repliedHash = ByteUtil.toByte(repliedHash);
    }

    public NoteContent(byte[] encode) {
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
        this.type = RLP.decodeInteger(messageList, 1, TxType.NEWS_TX.getType());
        this.memo = RLP.decodeElement(messageList, 2);
        this.repliedHash = RLP.decodeElement(messageList, 3);
        this.link = RLP.decodeElement(messageList, 5);

    }

    @Override
    public byte[] getEncoded() {
        byte[] version = RLP.encodeInteger(this.version);
        byte[] type = RLP.encodeInteger(this.type);
        byte[] memo = RLP.encodeElement(this.memo);
        byte[] repliedHash = RLP.encodeElement(this.repliedHash);
        byte[] link = RLP.encodeElement(this.link);

       return RLP.encodeList(version, type, memo, repliedHash, link);
    }

    public String getRepliedHashStr() {
        if (repliedHash != null) {
            return ByteUtil.toHexString(repliedHash);
        }
        return null;
    }

    public byte[] getRepliedHash() {
        return repliedHash;
    }

    public String getLinkStr() {
        if (link != null) {
            return ByteUtil.toHexString(link);
        }
        return null;
    }

    public byte[] getLink() {
        return link;
    }
}
