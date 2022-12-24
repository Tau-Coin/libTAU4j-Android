package io.taucoin.news.publishing.core.utils;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import io.taucoin.news.publishing.MainApplication;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.storage.sqlite.entity.User;
import io.taucoin.news.publishing.core.utils.media.MediaUtil;

/**
 * 用户相关逻辑处理类
 */
public class UsersUtil {
    private static final int DEFAULT_NAME_LENGTH = 4;
    private static final int QR_NAME_LENGTH = 3;

    /**
     * 获取默认截取的名字
     * 规则：以T开头，然后取前3个数字
     * @param name 截取前的名字
     * @return 截取后的名字
     */
    public static String getDefaultName(String name) {
        StringBuilder defaultName = new StringBuilder();
        if(StringUtil.isNotEmpty(name)){
            for (int i = 0; i < name.length(); i++) {
                if (!Character.isDigit(name.charAt(i))) {
                    defaultName.append(name.charAt(i));
                    break;
                }
            }
            for (int i = 0; i < name.length(); i++) {
                if (Character.isDigit(name.charAt(i))) {
                    defaultName.append(name.charAt(i));
                    if(defaultName.length() == DEFAULT_NAME_LENGTH) {
                        break;
                    }
                }
            }
        }
        return defaultName.toString().toUpperCase();
    }

    /**
     * 获取中间隐藏的名字
     * @param name 隐藏前的名字
     * @return 隐藏后的名字
     */
    public static String getMidHideName(String name) {
        return getMidHideName(name, 8);
    }

    /**
     * 获取中间隐藏的名字
     * @param name 隐藏前的名字
     * @param lastNum 隐藏小数点后的位数
     * @return 隐藏后的名字
     */
    public static String getMidHideName(String name, int lastNum) {
        if(StringUtil.isNotEmpty(name) && name.length() > lastNum + 3){
            String midHideName = name.substring(0, 3);
            midHideName += "***";
            midHideName += name.substring(name.length() - lastNum);
            return midHideName;
        }
        return name;
    }

    /**
     * 获取显示名字
     * @param user 当前用户
     * @return 显示名字
     */
    public static String getShowName(@NonNull User user) {
        if (null == user) {
            return null;
        }
        if (StringUtil.isNotEmpty(user.remark)) {
            return user.remark;
        } else if (StringUtil.isNotEmpty(user.nickname)) {
            return user.nickname;
        } else {
            return UsersUtil.getDefaultName(user.publicKey);
        }
    }

    public static String getShowNameWithYourself(User user, String publicKey) {
        String showName = getShowName(user, publicKey);
        if (StringUtil.isEquals(publicKey, MainApplication.getInstance().getPublicKey())) {
            showName += MainApplication.getInstance().getString(R.string.contacts_yourself);
        }
        return showName;
    }

    /**
     * 获取显示名字
     * @param user 当前用户
     * @return 显示名字
     */
    public static String getShowName(User user, String publicKey) {
        if(null == user){
            return UsersUtil.getDefaultName(publicKey);
        }else{
            return getShowName(user);
        }
    }

    /**
     * 获取显示当前用户名字
     * @param user 当前用户
     * @return 显示名字
     */
    public static String getCurrentUserName(@NonNull User user) {
        if(StringUtil.isNotEmpty(user.nickname)){
            return user.nickname;
        } else {
            return UsersUtil.getDefaultName(user.publicKey);
        }
    }

    public static String getUserName(User user, String publicKey) {
        if(user != null){
            return getShowName(user);
        }else{
            return UsersUtil.getDefaultName(publicKey);
        }
    }

    /**
     * 获取balance的显示
     * @param balance 余额
     * @return 余额显示
     */
    public static String getShowBalance(long balance) {
        if (balance >= 1000000) {
            return (int)(balance / 1000000) + "m";
        } else if(balance >= 1000) {
            return (int)(balance / 1000) + "k";
        } else {
            return String.valueOf((int)balance);
        }
    }

    /**
     * 获取二维码上图片的名字
     * @param name
     * @return
     */
    public static String getQRCodeName(String name) {
        if (StringUtil.isNotEmpty(name) && name.length() > QR_NAME_LENGTH) {
            return name.substring(0, QR_NAME_LENGTH);
        }
        return name;
    }

    /**
     * 获取公钥后六位
     * @param publicKey
     * @return
     */
    public static String getLastPublicKey(String publicKey) {
        return getLastPublicKey(publicKey, 6);
    }

    public static String getLastPublicKey(String publicKey, int size) {
        if (StringUtil.isNotEmpty(publicKey) && publicKey.length() > size) {
            int length = publicKey.length();
            return publicKey.substring(length - size, length);
        }
        return publicKey;
    }

    /**
     * 获取公钥后四位
     * @param publicKey
     * @return
     */
    public static String getHideLastPublicKey(String publicKey) {
        if (StringUtil.isNotEmpty(publicKey) && publicKey.length() > 4) {
            int length = publicKey.length();
            String hidePublicKey = "***";
            hidePublicKey += publicKey.substring(length - 4, length);
            return hidePublicKey;
        }
        return publicKey;
    }

    /**
     * 获取用户头像
     * @param user 用户信息
     * @return Bitmap
     */
    public static Bitmap getHeadPic(@NonNull User user) {
        if (null == user) {
            return null;
        }
        Bitmap bitmap = null;
        if (user.headPic != null) {
            bitmap = MediaUtil.bytes2Bitmap(user.headPic);
        }
        if (null == bitmap) {
            int bgColor = Utils.getGroupColor(user.publicKey);
            String firstLettersName = StringUtil.getFirstLettersOfName(getShowName(user));
            bitmap = BitmapUtil.createLogoBitmap(bgColor, firstLettersName);
        }
        return bitmap;
    }
}