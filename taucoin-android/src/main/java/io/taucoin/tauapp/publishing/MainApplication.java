package io.taucoin.tauapp.publishing;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;

import com.luck.picture.lib.io.ArrayPoolProvide;
import com.yalantis.ucrop.UCropActivity;

import java.util.concurrent.Executors;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;
import androidx.work.Configuration;
import androidx.work.WorkManager;
import io.taucoin.tauapp.publishing.core.Constants;
import io.taucoin.tauapp.publishing.core.log.LogConfigurator;
import io.taucoin.tauapp.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.tauapp.publishing.core.storage.RepositoryHelper;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.User;
import io.taucoin.tauapp.publishing.core.utils.CrashHandler;
import io.taucoin.tauapp.publishing.core.utils.FixMemLeak;
import io.taucoin.tauapp.publishing.ui.TauNotifier;

public class MainApplication extends MultiDexApplication {
    static {
        /* Vector Drawable support in ImageView for API < 21 */
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private static MainApplication instance;
    private User currentUser; // 当前用户
    private int activityNumber = 0; // 当前Activity个数
    private SettingsRepository settingsRepo;
    public Handler applicationHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        applicationHandler = new Handler(this.getApplicationContext().getMainLooper());
        // 初始化日志配置
        LogConfigurator.configure();
        // Crash处理
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(getApplicationContext());

        TauNotifier.getInstance(this).makeNotifyChannels();
        registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
        settingsRepo = RepositoryHelper.getSettingsRepository(this);

        // 首先在AndroidManifest.xml中禁用默认提供程序
        // 自定义一个线程池, 根据项目中需要同时作业的Worker数而定
        Configuration configuration = new Configuration.Builder()
                .setExecutor(Executors.newFixedThreadPool(4))
                .build();
        WorkManager.initialize(this, configuration);

        FixMemLeak.fixSamSungEmergencyModeLeak(getApplicationContext());
    }

    public static MainApplication getInstance(){
        return instance;
    }

    /**
     * 获取全局参数 当前用户的publicKey
     * @return  publicKey 公钥
     */
    public String getPublicKey() {
        if (currentUser != null) {
            return currentUser.publicKey;
        }
        return null;
    }

    /**
     * 获取全局参数 当前用户的publicKey
     * @return  publicKey 公钥
     */
    public String getSeed() {
        if (currentUser != null) {
            return currentUser.seed;
        }
        return null;
    }

    /**
     * 设置全局参数 当前用户
     * @param user 当前用户
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * 返回全局参数 当前用户
     */
    public User getCurrentUser() {
        return this.currentUser;
    }

    /**
     * Activity 生命周期监听，用于监控app前后台状态切换
     */
    ActivityLifecycleCallbacks activityLifecycleCallbacks = new ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

        }

        @Override
        public void onActivityStarted(Activity activity) {
            if (activityNumber == 0) {
                // app回到前台
                settingsRepo.setBooleanValue(activity.getString(
                        R.string.pref_key_foreground_running), true);
            }
            activityNumber++;
        }

        @Override
        public void onActivityResumed(Activity activity) {
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
            activityNumber--;
            if (activityNumber == 0) {
                // app回到后台
                settingsRepo.setBooleanValue(activity.getString(
                        R.string.pref_key_foreground_running), false);
            }
            // 清除裁剪页面内存
            if (activity instanceof UCropActivity) {
                ArrayPoolProvide.getInstance().clearMemory();
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            FixMemLeak.fixLeak(activity);
        }
    };

    @Override
    public Resources getResources() {
        Resources resources = super.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        settingsRepo = RepositoryHelper.getSettingsRepository(this);
        String fontKey = resources.getString(R.string.pref_key_font_scale_size);
        float fontScaleSize = settingsRepo.getFloatValue(fontKey, Constants.DEFAULT_FONT_SCALE_SIZE);
        android.content.res.Configuration configuration = resources.getConfiguration();
        if (fontScaleSize > 0) {
            configuration.fontScale = fontScaleSize;
        } else {
            configuration.fontScale = 1.0f;
        }
        resources.updateConfiguration(configuration, dm);
        return resources;
    }
}