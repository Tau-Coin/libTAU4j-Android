package io.taucbd.news.publishing.core.meetup;

import java.util.Date;

public class Meetup {

    private String  mId;            // meetup id, sha1 hash(mOrganizer, mTopic, timestamp of created for this meetup)
    private String  mOrganizer;     // 发起人公钥
    private String  mTopic;         // meetup主题
    private Date    mData;          // meetup具体时间日期
    private String  mLocation;      // 位置信息
    private int     mPtcpNumber;    // The number of participants
    private String  mExtraInfo;     // 附加条件
    private String  mScheme;        // open ai产生的meetup具体方案
    private Date    mSchemeDate;    // mScheme产生的日期时间

    public Meetup(String  organizer, String  topic, String  location, int ptcpNumber, String  extraInfo) {
        // TODO: generate id
        // mId = id;
        mOrganizer = organizer;
        mTopic = topic;
        mLocation = location;
        mPtcpNumber = ptcpNumber;
        mExtraInfo = extraInfo;
    }

    public String getId() { return mId; }

    public void setScheme(String scheme, Date date) {
        mScheme = scheme;
        mSchemeDate = date;
    }
}
