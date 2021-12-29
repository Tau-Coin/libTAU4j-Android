package io.taucoin.torrent.publishing.ui.constant;

import android.graphics.Bitmap;

import com.google.gson.annotations.Expose;

/**
 * QR内容
 */
public class QRContent {
//    @Expose
//    private Bitmap headPic;
    private String nickName;

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

//    public Bitmap getHeadPic() {
//        return headPic;
//    }
//
//    public void setHeadPic(Bitmap headPic) {
//        this.headPic = headPic;
//    }
}