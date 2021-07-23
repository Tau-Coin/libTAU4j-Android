package io.taucoin.torrent.publishing.core.storage;

import android.content.Context;
import androidx.annotation.NonNull;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepositoryImpl;
import io.taucoin.torrent.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepositoryImpl;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.CommunityRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.CommunityRepositoryImpl;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.DeviceRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.DeviceRepositoryImpl;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.FavoriteRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.FavoriteRepositoryImpl;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.FriendRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.FriendRepositoryImpl;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.MemberRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.MemberRepositoryImpl;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.MsgRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.MsgRepositoryImpl;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.NotificationRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.NotificationRepositoryImpl;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.StatisticRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.StatisticRepositoryImpl;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.TxRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.TxRepositoryImpl;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepositoryImpl;

/**
 * RepositoryHelper创建数据库操作单例
 */
public class RepositoryHelper {
    private static CommunityRepositoryImpl communityRepo;
    private static MemberRepositoryImpl memberRepo;
    private static UserRepositoryImpl userRepo;
    private static TxRepositoryImpl txRepo;
    private static MsgRepositoryImpl msgRepo;
    private static SettingsRepositoryImpl settingsRepo;
    private static FavoriteRepositoryImpl favoriteRepo;
    private static NotificationRepositoryImpl notificationRepo;
    private static FriendRepositoryImpl friendRepo;
    private static ChatRepositoryImpl chatRepo;
    private static DeviceRepositoryImpl deviceRepo;
    private static StatisticRepositoryImpl statisticRepo;

    /**
     * 获取CommunityRepository单例
     * @param appContext 上下文
     * @return CommunityRepository
     */
    public synchronized static CommunityRepository getCommunityRepository(@NonNull Context appContext) {
        if (communityRepo == null)
            communityRepo = new CommunityRepositoryImpl(appContext,
                    AppDatabase.getInstance(appContext));

        return communityRepo;
    }

    /**
     * 获取MemberRepository单例
     * @param appContext 上下文
     * @return MemberRepository
     */
    public synchronized static MemberRepository getMemberRepository(@NonNull Context appContext) {
        if (memberRepo == null)
            memberRepo = new MemberRepositoryImpl(appContext,
                    AppDatabase.getInstance(appContext));

        return memberRepo;
    }

    /**
     * 获取UserRepository单例
     * @param appContext 上下文
     * @return UserRepository
     */
    public synchronized static UserRepository getUserRepository(@NonNull Context appContext) {
        if (userRepo == null)
            userRepo = new UserRepositoryImpl(appContext,
                    AppDatabase.getInstance(appContext));

        return userRepo;
    }

    /**
     * 获取TxRepository单例
     * @param appContext 上下文
     * @return TxRepository
     */
    public synchronized static TxRepository getTxRepository(@NonNull Context appContext) {
        if (txRepo == null)
            txRepo = new TxRepositoryImpl(appContext,
                    AppDatabase.getInstance(appContext));

        return txRepo;
    }
    /**
     * 获取MsgRepository单例
     * @param appContext 上下文
     * @return TxRepository
     */
    public synchronized static MsgRepository getMsgRepository(@NonNull Context appContext) {
        if (msgRepo == null)
            msgRepo = new MsgRepositoryImpl(appContext,
                    AppDatabase.getInstance(appContext));

        return msgRepo;
    }

    /**
     * 获取SettingsRepository单例
     * @param appContext 上下文
     * @return TxRepository
     */
    public synchronized static SettingsRepository getSettingsRepository(@NonNull Context appContext) {
        if (settingsRepo == null)
            settingsRepo = new SettingsRepositoryImpl(appContext);

        return settingsRepo;
    }

    /**
     * 获取FavoriteRepository单例
     * @param appContext 上下文
     * @return FavoriteRepository
     */
    public synchronized static FavoriteRepository getFavoriteRepository(@NonNull Context appContext) {
        if (favoriteRepo == null)
            favoriteRepo = new FavoriteRepositoryImpl(appContext,
                    AppDatabase.getInstance(appContext));

        return favoriteRepo;
    }

    /**
     * 获取FavoriteRepository单例
     * @param appContext 上下文
     * @return NotificationRepository
     */
    public synchronized static NotificationRepository getNotificationRepository(@NonNull Context appContext) {
        if (notificationRepo == null)
            notificationRepo = new NotificationRepositoryImpl(appContext,
                    AppDatabase.getInstance(appContext));

        return notificationRepo;
    }

    /**
     * 获取FriendRepository单例
     * @param appContext 上下文
     * @return FriendRepository
     */
    public synchronized static FriendRepository getFriendsRepository(@NonNull Context appContext) {
        if (friendRepo == null)
            friendRepo = new FriendRepositoryImpl(appContext,
                    AppDatabase.getInstance(appContext));

        return friendRepo;
    }

    /**
     * 获取ChatRepository单例
     * @param appContext 上下文
     * @return ChatRepository
     */
    public synchronized static ChatRepository getChatRepository(@NonNull Context appContext) {
        if (chatRepo == null)
            chatRepo = new ChatRepositoryImpl(appContext,
                    AppDatabase.getInstance(appContext));

        return chatRepo;
    }

    /**
     * 获取DeviceRepository单例
     * @param appContext 上下文
     * @return DeviceRepository
     */
    public synchronized static DeviceRepository getDeviceRepository(@NonNull Context appContext) {
        if (deviceRepo == null)
            deviceRepo = new DeviceRepositoryImpl(appContext,
                    AppDatabase.getInstance(appContext));

        return deviceRepo;
    }

    /**
     * 获取StatisticRepository单例
     * @param appContext 上下文
     * @return DeviceRepository
     */
    public synchronized static StatisticRepository getStatisticRepository(@NonNull Context appContext) {
        if (statisticRepo == null)
            statisticRepo = new StatisticRepositoryImpl(appContext,
                    AppDatabase.getInstance(appContext));

        return statisticRepo;
    }
}
