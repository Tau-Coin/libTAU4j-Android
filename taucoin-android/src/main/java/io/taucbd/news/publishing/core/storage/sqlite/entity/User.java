package io.taucbd.news.publishing.core.storage.sqlite.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 数据库存储User实体类
 */
@Entity(tableName = "Users")
public class User implements Parcelable {
    @NonNull
    @PrimaryKey
    public String publicKey;                // 用户的公钥
    public String seed;                     // 用户的seed
    public String remark;                   // 用户备注
    public String nickname;                 // 用户昵称
    public long updateNNTime;               // 更新昵称时间
    public double longitude;                // 经纬度
    public double latitude;                 // 纬度
    public long updateLocationTime;         // 更新位置时间
    public byte[] headPic;                  // 头像图片
    public long updateHPTime;               // 更新头像图片时间
    public boolean isCurrentUser = false;   // 是否是当前用户
    public boolean isBanned = false;        // 用户是否被用户拉入黑名单
    public boolean isMemBanned = false;     // 社区成员是否被用户拉入黑名单
    public String profile;                  // 用户简介
    public long updatePFTime;               // 更新用户简介时间

    public User(@NonNull String publicKey, String seed, String nickname, boolean isCurrentUser){
        this.publicKey = publicKey;
        this.seed = seed;
        this.nickname = nickname;
        this.isCurrentUser = isCurrentUser;
    }

    @Ignore
    public User(@NonNull String publicKey, String nickname){
        this.publicKey = publicKey;
        this.nickname = nickname;
    }

    @Ignore
    public User(@NonNull String publicKey){
        this.publicKey = publicKey;
    }

    @Ignore
    protected User(Parcel in) {
        publicKey = in.readString();
        seed = in.readString();
        remark = in.readString();
        nickname = in.readString();
        updateNNTime = in.readLong();
        isCurrentUser = in.readByte() != 0;
        isBanned = in.readByte() != 0;
        isMemBanned = in.readByte() != 0;
        longitude = in.readDouble();
        latitude = in.readDouble();
        updateLocationTime = in.readLong();
        int length = in.readInt();
        if (length > 0) {
            this.headPic = new byte[length];
            in.readByteArray(this.headPic);
        }
        profile = in.readString();
        updatePFTime = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(publicKey);
        dest.writeString(seed);
        dest.writeString(remark);
        dest.writeString(nickname);
        dest.writeLong(updateNNTime);
        dest.writeByte((byte) (isCurrentUser ? 1 : 0));
        dest.writeByte((byte) (isBanned ? 1 : 0));
        dest.writeByte((byte) (isMemBanned ? 1 : 0));
        dest.writeDouble(longitude);
        dest.writeDouble(latitude);
        dest.writeLong(updateLocationTime);
        int length = headPic != null ? headPic.length : 0;
        dest.writeInt(length);
        if (length > 0) {
            dest.writeByteArray(headPic);
        }
        dest.writeString(profile);
        dest.writeLong(updatePFTime);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    @Override
    public int hashCode() {
        return publicKey.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof User && (o == this || publicKey.equals(((User)o).publicKey));
    }
}
