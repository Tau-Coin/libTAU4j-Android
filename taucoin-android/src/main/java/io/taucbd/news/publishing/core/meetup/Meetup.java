package io.taucbd.news.publishing.core.meetup;

public class Meetup {

    private String  mId;            // meetup id, news transaction hash)
    private String  mOrganizer;     // 发起人公钥
    private String  mTopic;         // meetup主题
    private long    mDate;          // meetup具体时间日期
    private String  mLocation;      // 位置信息
    private int     mPtcpNumber;    // The number of participants
    private String  mExtraInfo;     // 附加条件
    private String  mScheme;        // open ai产生的meetup具体方案
    private long    mSchemeDate;    // mScheme产生的日期时间

    public Meetup(String id, String organizer, String topic, String location,
                  int ptcpNumber, String  extraInfo) {
        mId = id;
        mOrganizer = organizer;
        mTopic = topic;
        mDate = System.currentTimeMillis() / 1000L;
        mLocation = location;
        mPtcpNumber = ptcpNumber;
        mExtraInfo = extraInfo;
    }

    public Meetup(String id, String organizer, String topic, String location, long date,
                  int ptcpNumber, String extraInfo, String scheme, long schemeDate) {
        mId = id;
        mOrganizer = organizer;
        mTopic = topic;
        mDate = date;
        mLocation = location;
        mPtcpNumber = ptcpNumber;
        mExtraInfo = extraInfo;
        mScheme = scheme;
        mSchemeDate = schemeDate;
    }

    public String getId() {
        return mId;
    }

    public String getOrganizer() {
        return mOrganizer;
    }

    public void setOrganizer(String organizer) {
        mOrganizer = organizer;
    }

    public String getTopic() {
        return mTopic;
    }

    public void setTopic(String topic) {
        mTopic = topic;
    }

    public long getDate() {
        return mDate;
    }

    public void setDate(long date) {
        mDate = date;
    }

    public String getLocation() {
        return mLocation;
    }

    public void setLocation(String location) {
        mLocation = location;
    }

    public int getPtcpNumber() {
        return mPtcpNumber;
    }

    public void setPtcpNumber(int pn) {
        mPtcpNumber = pn;
    }

    public String getExtraInfo() {
        return mExtraInfo;
    }

    public void setExtraInfo(String ei) {
        mExtraInfo = ei;
    }

    public String getScheme() {
        return mScheme;
    }

    public long getSchemeDate() {
        return mSchemeDate;
    }

    public void setScheme(String scheme, long date) {
        mScheme = scheme;
        mSchemeDate = date;
    }
}
