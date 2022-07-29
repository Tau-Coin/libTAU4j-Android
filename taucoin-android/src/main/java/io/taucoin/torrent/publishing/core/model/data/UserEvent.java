package io.taucoin.torrent.publishing.core.model.data;

import io.taucoin.torrent.publishing.core.utils.rlp.RLP;
import io.taucoin.torrent.publishing.core.utils.rlp.RLPList;

public class UserEvent {
    public enum Event {
        UNKNOWN(0),     // unknown
        FOCUS_FRIEND(1);        // 用户点击朋友详情、进入chat页面触发

        private final int event;
        Event(int event) {
            this.event = event;
        }

        public int getValue() {
            return event;
        }

        public static UserEvent.Event parse(int event) {
            UserEvent.Event[] eventArr = UserEvent.Event.values();
            for (UserEvent.Event e : eventArr) {
                if (event == e.getValue()) {
                    return e;
                }
            }
            return UNKNOWN;
        }
    }
    private int event;
    private byte[] data;

    private byte[] rlpEncoded;          // 编码数据

    public UserEvent(Event event, byte[] data) {
        this.event = event.getValue();
        this.data = data;
    }

    public UserEvent(byte[] rlpEncoded) {
        if (rlpEncoded != null) {
            this.rlpEncoded = rlpEncoded;
            parseRLP();
        }
    }

    public int getEvent() {
        return event;
    }

    public byte[] getData() {
        return data;
    }

    /**
     * parse rlp encode
     */
    private void parseRLP() {
        RLPList params = RLP.decode2(this.rlpEncoded);
        RLPList list = (RLPList) params.get(0);

        byte[] eventBytes = list.get(1).getRLPData();
        int defaultEvent = Event.UNKNOWN.getValue();
        this.event = null == eventBytes ? defaultEvent : RLP.decodeInt(eventBytes, defaultEvent);
        this.data = list.get(1).getRLPData();
    }

    /**
     * get encoded hash list
     * @return encode
     */
    public byte[] getEncoded() {
        if (null == rlpEncoded) {
            byte[] event = RLP.encodeInt(this.event);
            byte[] data = RLP.encodeElement(this.data);

            this.rlpEncoded = RLP.encodeList(event, data);
        }
        return rlpEncoded;
    }
}
