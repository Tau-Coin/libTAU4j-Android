package io.taucoin.torrent.publishing.core.model.data.message;

import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.rlp.RLP;
import io.taucoin.torrent.publishing.core.utils.rlp.RLPList;

/**
 * 官方领导者邀请内容类
 */
public class LeaderInvitationContent extends TxContent {
    private byte[] title;

    public LeaderInvitationContent(String title, String description) {
        super(TxType.LEADER_INVITATION.getType(), Utils.textStringToBytes(description));
        this.title = Utils.textStringToBytes(title);
    }

    public LeaderInvitationContent(byte[] encode) {
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
        this.type = RLP.decodeInteger(messageList, 1, TxType.LEADER_INVITATION.getType());
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

    public byte[] getTitle() {
        return title;
    }
}
