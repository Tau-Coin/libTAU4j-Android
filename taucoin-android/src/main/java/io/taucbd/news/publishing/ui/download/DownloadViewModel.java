package io.taucbd.news.publishing.ui.download;

import android.app.Application;
import android.content.Intent;

import com.google.common.collect.Maps;
import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucbd.news.publishing.BuildConfig;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.model.data.DataResult;
import io.taucbd.news.publishing.core.model.data.Version;
import io.taucbd.news.publishing.core.storage.sp.SettingsRepository;
import io.taucbd.news.publishing.core.storage.RepositoryHelper;
import io.taucbd.news.publishing.core.utils.AppUtil;
import io.taucbd.news.publishing.core.utils.FileUtil;
import io.taucbd.news.publishing.core.utils.HttpUtil;
import io.taucbd.news.publishing.core.utils.ToastUtils;
import io.taucbd.news.publishing.ui.BaseActivity;
import io.taucbd.news.publishing.ui.constant.IntentExtra;
import okhttp3.Response;

/**
 * 下载相关的ViewModel
 */
public class DownloadViewModel extends AndroidViewModel {

    private static final Logger logger = LoggerFactory.getLogger("DownloadViewModel");
    private static final String BASE_URL = "https://taucoin.io/";
    private static final String CHECK_VERSION_URL = BASE_URL + "versions/";

    private SettingsRepository settingsRepository;
    private LocalDownloadManager localDownloadManager;
    private Disposable disposable;
    public DownloadViewModel(@NonNull Application application) {
        super(application);
        settingsRepository = RepositoryHelper.getSettingsRepository(application);
        localDownloadManager = new LocalDownloadManager(application);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
        localDownloadManager.unregisterContentObserver(getApplication());
    }

    MutableLiveData<Float> getOnProgress(){
        return localDownloadManager.getOnProgress();
    }

    /**
     * 设置是否需要提示用户升级
     * @param isNeedPrompt
     */
    public void setNeedPromptUser(boolean isNeedPrompt){
        settingsRepository.setNeedPromptUser(isNeedPrompt);
    }

    /**
     * 检查APP版本，查看是否需要版本升级
     */
    public void checkAppVersion(BaseActivity activity) {
        checkAppVersion(activity, false);
    }

    /**
     * 检查APP版本，查看是否需要版本升级
     * @param activity
     * @param showNoUpdates 显示无需更新
     */
    public void checkAppVersion(BaseActivity activity, boolean showNoUpdates) {
        if (disposable != null && !disposable.isDisposed()) {
            return;
        }
        disposable = Observable.create((ObservableOnSubscribe<Version>) emitter -> {
            int versionCode = AppUtil.getVersionCode();
            Map<String, Integer> map = Maps.newHashMap();
            map.put("version", versionCode);
            map.put("id", BuildConfig.TAU_LANGUAGE);
            try {
                Response response = HttpUtil.httpPost(CHECK_VERSION_URL, map);
                if (response.isSuccessful() && response.code() == 200) {
                    if (response.body() != null) {
                        String result = response.body().string();
                        DataResult dataResult = new Gson().fromJson(result, DataResult.class);
                        if (dataResult != null && dataResult.getData() != null) {
                            logger.info("Response data::{}", dataResult.getData());
                            String data = new Gson().toJson(dataResult.getData());
                            Version version = new Gson().fromJson(data, Version.class);
                            if (!emitter.isDisposed()) {
                                emitter.onNext(version);
                            }
                        } else {
                            if (!emitter.isDisposed()) {
                                emitter.onError(new Throwable("response body Gson error"));
                            }
                        }
                    } else {
                        if (!emitter.isDisposed()) {
                            emitter.onError(new Throwable("response body empty"));
                        }
                    }
                } else {
                    if (!emitter.isDisposed()) {
                        emitter.onError(new Throwable("response code != 200"));
                    }
                }
            } catch (Exception e) {
                logger.error("Error::Check app version::{}", e.getMessage());
                Throwable throwable = e.getCause();
                if (null == throwable) {
                    throwable = new Throwable("Error::" + e.getMessage());
                }
                if (!emitter.isDisposed()) {
                    emitter.onError(throwable);
                }
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(version -> handleVersionUpgrade(activity, version, showNoUpdates), it -> {
                    activity.closeProgressDialog();
                    if (showNoUpdates) {
                        ToastUtils.showShortToast(R.string.app_upgrade_failed);
                    }
                });
    }

    /**
     * 处理版本升级
     * @param activity
     * @param version
     */
    private void handleVersionUpgrade(BaseActivity activity, Version version, boolean showNoUpdates) {
        if (showNoUpdates) {
            activity.closeProgressDialog();
        }
        if (null == version) {
            return;
        }
        // 是否需要升级
        boolean isNeedUpgrade = version.getNumber() > AppUtil.getVersionCode();
        if (!isNeedUpgrade) {
            if (showNoUpdates) {
                ToastUtils.showShortToast(R.string.app_upgrade_latest);
            }
            // 不需要升级，删除本地的apk
            removeOldApk();
            return;
        }
        // 是否强制升级
        if(!version.isForced()){
            version.setForced(version.getForcedNum() > 0);
        }

        boolean isNeedPrompt = settingsRepository.isNeedPromptUser();
        // 非强制升级且不需要提示
        if(!version.isForced() && !isNeedPrompt && !showNoUpdates){
            return;
        }

        String filePath = FileUtil.getDownloadFilePath();
        String fileName = activity.getString(R.string.app_name);
        fileName += version.getNumber() + ".apk";
        version.setDownloadFilePath(filePath);
        version.setDownloadFileName(fileName);
        openUpgradeActivity(activity, version);
    }

    /**
     * 移除旧的APK文件
     */
    private void removeOldApk() {
        FileUtil.deleteFile(FileUtil.getDownloadFilePath());
    }

    public SettingsRepository getSettingsRepository() {
        return settingsRepository;
    }

    /**
     * 打开升级页面
     * @param activity
     * @param version
     */
    private void openUpgradeActivity(AppCompatActivity activity, Version version) {
        String className = activity.getClass().getName();
        if(AppUtil.isForeground(activity, className)){
            Intent intent = new Intent(activity, UpgradeActivity.class);
            intent.putExtra(IntentExtra.BEAN, version);
            activity.startActivity(intent);
        }
    }

    /**
     * 利用系统DownloadManager实现下载升级APK
     * @param version
     */
    void downLoadUpgradeApk(Version version) {
        String storagePath = version.getDownloadFilePath() + version.getDownloadFileName();
        localDownloadManager.downLoadUpgradeApk(getApplication(), version.getLink(), storagePath);
        // 当前本地的apk
        if(!AppUtil.isApkFileExists(getApplication(), storagePath)){
            long oldDownloadID = settingsRepository.getApkDownloadID();
            if(oldDownloadID != -1){
                localDownloadManager.removeDownloadID(oldDownloadID);
            }
            removeOldApk();
        }
        settingsRepository.setApkDownloadID(localDownloadManager.getDownloadID());
    }

    /**
     * 安装APK
     * @param activity
     * @param version
     */
    void installApk(AppCompatActivity activity, Version version) {
        String apkPath = version.getDownloadFilePath() + version.getDownloadFileName();
        AppUtil.installApk(activity, apkPath);
    }

    /**
     * 关闭下载
     */
    void closeDownloading() {
        localDownloadManager.closeDownloading();
    }

    /**
     * 关闭查询调度
     */
    void closeQuerySchedule() {
        localDownloadManager.closeQuerySchedule();
    }
}