package io.taucoin.tauapp.publishing.core.model.data.message;

import io.taucoin.tauapp.publishing.core.utils.Utils;
import io.taucoin.tauapp.publishing.core.utils.rlp.RLP;
import io.taucoin.tauapp.publishing.core.utils.rlp.RLPList;

/**
 * 官方领导者邀请内容类
 */
public class AnnouncementContent extends TxContent {
    private byte[] title;

    public AnnouncementContent(String title, String description) {
        super(TxType.ANNOUNCEMENT.getType(), Utils.textStringToBytes(description));
        this.title = Utils.textStringToBytes(title);
    }

    public AnnouncementContent(byte[] encode) {
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
        this.type = RLP.decodeInteger(messageList, 1, TxType.ANNOUNCEMENT.getType());
        this.memo = RLP.decodeElement(messageList, 2);
        this.title = RLP.decodeElement(messageList, 3);

    }

    @Override
    public byte[] getEncoded() {
        byte[] version = RLP.encodeInteger(this.version);
        byte[] type = RLP.encodeInteger(this.type);
        byte[] memo = RLP.encodeElement(this.memo);
        byte[] title = RLP.encodeElement(this.title);

       return RLP.encodeList(version, type, memo, title);
    }

    public String getTitle() {
        if (title != null) {
            return Utils.textBytesToString(title);
        }
        return null;
    }
}
