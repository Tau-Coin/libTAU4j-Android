package io.taucbd.news.publishing.ui;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import io.taucbd.news.publishing.BuildConfig;
import io.taucbd.news.publishing.MainApplication;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.storage.sqlite.entity.User;
import io.taucbd.news.publishing.core.utils.BitmapUtil;
import io.taucbd.news.publishing.core.utils.ChainIDUtil;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.core.utils.UsersUtil;
import io.taucbd.news.publishing.core.utils.Utils;
import io.taucbd.news.publishing.ui.constant.IntentExtra;
import io.taucbd.news.publishing.ui.main.MainActivity;
import io.taucbd.news.publishing.ui.transaction.CommunityChatActivity;
import io.taucbd.news.publishing.ui.transaction.NewsDetailActivity;

import static android.content.Context.NOTIFICATION_SERVICE;

/*
 * Helper of showing notifications.
 */
public class TauNotifier {
    private static final String TAG = TauNotifier.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(TAG);

    // 前台服务通知渠道ID
    private static final String FOREGROUND_NOTIFY_CHANNEL_ID = BuildConfig.APPLICATION_ID + ".FOREGROUND_NOTIFY_CHANNEL_ID";
    // 默认通知渠道ID
    private static final String MSG_NOTIFY_CHANNEL_ID = BuildConfig.APPLICATION_ID + ".MSG_NOTIFY_CHANNEL_ID";

    // 服务启动的通知ID
    private static final int SERVICE_STARTED_NOTIFICATION_ID = -1;

    private static volatile TauNotifier INSTANCE;

    private Context appContext;
    private NotificationManager notifyManager;

    public static TauNotifier getInstance() {
        return getInstance(MainApplication.getInstance());
    }

    public static TauNotifier getInstance(@NonNull Context appContext) {
        if (INSTANCE == null) {
            synchronized (TauNotifier.class) {
                if (INSTANCE == null)
                    INSTANCE = new TauNotifier(appContext);
            }
        }
        return INSTANCE;
    }

    private TauNotifier(Context appContext) {
        this.appContext = appContext;
        notifyManager = (NotificationManager)appContext.getSystemService(NOTIFICATION_SERVICE);
    }

    /**
     * 创建通知渠道
     */
    public void makeNotifyChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        ArrayList<NotificationChannel> channels = new ArrayList<>();
        // 添加默认通知渠道
        NotificationChannel defaultChannel = new NotificationChannel(MSG_NOTIFY_CHANNEL_ID,
                appContext.getString(R.string.chat_channel), NotificationManager.IMPORTANCE_HIGH);
        defaultChannel.enableVibration(false);
        defaultChannel.enableLights(false);
        defaultChannel.setSound(null, null);
        channels.add(defaultChannel);
        // 添加前台服务通知渠道
        NotificationChannel foregroundChannel = new NotificationChannel(FOREGROUND_NOTIFY_CHANNEL_ID,
                appContext.getString(R.string.foreground_notification), NotificationManager.IMPORTANCE_LOW);
        foregroundChannel.setShowBadge(false);
        channels.add(foregroundChannel);

        notifyManager.createNotificationChannels(channels);
    }

    /**
     * 创建前台通知
     * @param service 对应的Service
     */
    public static void makeForegroundNotify(Service service) {
        /* For starting main activity after click */
        Context context = service.getApplicationContext();
        Intent startupIntent = new Intent(context, MainActivity.class);
        startupIntent.setAction(Intent.ACTION_MAIN);
        startupIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        startupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent startupPendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            startupPendingIntent = PendingIntent.getActivity(context, 0, startupIntent,
                    PendingIntent.FLAG_MUTABLE);
        } else {
            startupPendingIntent = PendingIntent.getActivity(context, 0, startupIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }

        NotificationCompat.Builder foregroundNotify = new NotificationCompat.Builder(context,
                FOREGROUND_NOTIFY_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(startupPendingIntent)
                .setContentTitle(context.getString(R.string.app_running_in_the_background))
                .setTicker(context.getString(R.string.app_running_in_the_background))
                .setWhen(System.currentTimeMillis())
                .setCategory(Notification.CATEGORY_SERVICE);
        /* Disallow killing the service process by system */
        service.startForeground(SERVICE_STARTED_NOTIFICATION_ID, foregroundNotify.build());
    }

    /**
     * 创建聊天消息通知
     * @param friend
     * @param msg
     */
    public void makeChatNotify(User friend, CharSequence msg) {
        String friendPk = friend.publicKey;
        String friendName = UsersUtil.getShowName(friend);
        Bitmap bitmap = UsersUtil.getHeadPic(friend);
        Bitmap roundBitmap = BitmapUtil.toRoundBitmap(bitmap);
        bitmap.recycle();
        // 点击通知后进入的活动
        Intent intent = new Intent(appContext, MainActivity.class);
        // 解决PendingIntent的extra数据不准确问题
        intent.setAction(friendPk);
        intent.putExtra(IntentExtra.CHAIN_ID, friendPk);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(IntentExtra.TYPE, 1);
        intent.putExtra(IntentExtra.BEAN, friend);
//        // 这两句非常重要，使之前的活动不出栈
//        intent.setAction(Intent.ACTION_MAIN);
//        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        makeNotify(friend.publicKey.hashCode(), friendName, msg, roundBitmap, intent);
    }

    /**
     * 创建社区消息通知
     * @param chainID
     * @param newsHash news hash
     * @param txID note交易或者回复交易ID
     * @param news news或回复内容
     * @param chatMsg news的chat讨论内容
     */
    public void makeCommunityNotify(String chainID, String newsHash, String txID, CharSequence news,
                                    CharSequence chatMsg) {
        String communityName = ChainIDUtil.getName(chainID);
        int bgColor = Utils.getGroupColor(chainID);
        String firstLettersName = StringUtil.getFirstLettersOfName(communityName);
        Bitmap bitmap = BitmapUtil.createLogoBitmap(bgColor, firstLettersName);
        // 点击通知后进入的活动
        Intent intent;
        if (StringUtil.isEmpty(chatMsg)) {
            intent = new Intent(appContext, NewsDetailActivity.class);
        } else {
            intent = new Intent(appContext, CommunityChatActivity.class);
        }
        // 解决PendingIntent的extra数据不准确问题
        intent.setAction(chainID + newsHash);
        intent.putExtra(IntentExtra.CHAIN_ID, chainID);
        intent.putExtra(IntentExtra.HASH, newsHash);
        intent.putExtra(IntentExtra.ID, txID);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        // 这两句非常重要，使之前的活动不出栈
//        intent.setAction(Intent.ACTION_MAIN);
//        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        makeNotify(chainID.hashCode(), communityName + "-" + news,
                chatMsg, bitmap, intent);
    }

    private void makeNotify(int id, CharSequence title, CharSequence text, Bitmap largeIcon, Intent intent) {
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(appContext, 0, intent,
                    PendingIntent.FLAG_MUTABLE); // 允许更新
        } else {
            pendingIntent = PendingIntent.getActivity(appContext, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT); // 允许更新
        }
        NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(appContext, MSG_NOTIFY_CHANNEL_ID)
                .setAutoCancel(true)
                .setContentTitle(title);
        if (StringUtil.isNotEmpty(text)) {
            notifyBuilder.setContentText(text);
        }
        notifyBuilder.setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setDefaults(Notification.FLAG_ONLY_ALERT_ONCE)
                // 悬浮框
                .setTicker(title)
                .setPriority(Notification.PRIORITY_HIGH)
                .setLargeIcon(largeIcon)
                .setContentIntent(pendingIntent);
        // 3条以上通知取消合并
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notifyBuilder.setGroupSummary(false)
                    .setGroup("group");
        }
        notifyManager.notify(id, notifyBuilder.build());
    }

    /**
     * 关闭通知
     * @param id 通知id
     */
    public void cancelNotify(String id) {
        cancelNotify(id.hashCode());
    }

    private void cancelNotify(int id) {
        notifyManager.cancel(id);
    }

    /**
     * 关闭当前的所有通知
     */
    public void cancelAllNotify() {
        notifyManager.cancelAll();
    }
}
