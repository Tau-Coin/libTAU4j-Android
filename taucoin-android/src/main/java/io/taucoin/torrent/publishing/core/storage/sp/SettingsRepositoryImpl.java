package io.taucoin.torrent.publishing.core.storage.sp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposables;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;

/**
 * SettingsRepository: 用户设置的接口的实现
 */
public class SettingsRepositoryImpl implements SettingsRepository {
    private static class Default {
        static final boolean chargingState = false;
        static final boolean internetState = false;
        static final int internetType = -1;
        static final boolean isShowBanDialog = false;
        static final long cpu_sample = 15;                   // cpu采样大小，单位s
        static final long memory_sample = 15;                // Memory采样大小，单位s
    }

    private Context appContext;
    private SharedPreferences pref;
    private SharedPreferences.Editor edit;

    @SuppressLint("applyPrefEdits")
    public SettingsRepositoryImpl(@NonNull Context appContext) {
        this.appContext = appContext;
        pref = PreferenceManager.getDefaultSharedPreferences(appContext);
        edit = pref.edit();
    }

    /**
     * 观察设置改变的工作流
     * @return  Flowable
     */
    @Override
    public Flowable<String> observeSettingsChanged() {
        return Flowable.create((emitter) -> {
            SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> {
                if (emitter.isCancelled()) {
                    return;
                }
                emitter.onNext(key);
            };
            if (!emitter.isCancelled()) {
                pref.registerOnSharedPreferenceChangeListener(listener);
                emitter.setDisposable(Disposables.fromAction(() ->
                        pref.unregisterOnSharedPreferenceChangeListener(listener)));
            }
        }, BackpressureStrategy.LATEST);
    }

    @Override
    public void chargingState(boolean val) {
        edit.putBoolean(appContext.getString(R.string.pref_key_charging_state),val)
                .apply();
    }

    @Override
    public boolean chargingState() {
        return pref.getBoolean(appContext.getString(R.string.pref_key_charging_state),
                Default.chargingState);
    }

    @Override
    public boolean internetState() {
        return pref.getBoolean(appContext.getString(R.string.pref_key_internet_state),
                Default.internetState);
    }

    @Override
    public void internetState(boolean val) {
        edit.putBoolean(appContext.getString(R.string.pref_key_internet_state),val)
                .apply();
    }

    @Override
    public void setInternetType(int type) {
        edit.putInt(appContext.getString(R.string.pref_key_internet_type), type)
                .apply();
    }

    @Override
    public int getInternetType() {
        return pref.getInt(appContext.getString(R.string.pref_key_internet_type),
                Default.internetType);
    }

    @Override
    public String lastTxFee(String chainID){
        String key = appContext.getString(R.string.pref_key_last_tx_fee) + chainID;
        return pref.getString(key, "");
    }

    @Override
    public void lastTxFee(String chainID, String fee){
        String key = appContext.getString(R.string.pref_key_last_tx_fee) + chainID;
        edit.putString(key, fee)
                .apply();
    }

    @Override
    public void doNotShowBanDialog(boolean isShow) {
        String key = appContext.getString(R.string.pref_key_do_not_show_ban_dialog);
        edit.putBoolean(key, isShow)
                .apply();
    }

    @Override
    public boolean doNotShowBanDialog() {
        String key = appContext.getString(R.string.pref_key_do_not_show_ban_dialog);
        return pref.getBoolean(key, Default.isShowBanDialog);
    }

    @Override
    public void setApkDownloadID(long downloadID) {
        String key = appContext.getString(R.string.pref_key_apk_download_id);
        edit.putLong(key, downloadID)
                .apply();
    }

    @Override
    public long getApkDownloadID() {
        String key = appContext.getString(R.string.pref_key_apk_download_id);
        return pref.getLong(key, -1);
    }


    @Override
    public boolean isNeedPromptUser() {
        return pref.getBoolean(appContext.getString(R.string.pref_key_need_prompt_user), true);
    }

    @Override
    public void setNeedPromptUser(boolean isNeed) {
        edit.putBoolean(appContext.getString(R.string.pref_key_need_prompt_user), isNeed)
                .apply();
    }

    /**
     * UPnP连接是否开启
     */
    @Override
    public boolean isUPnpMapped() {
        return pref.getBoolean(appContext.getString(R.string.pref_key_upnp_mapped), false);
    }

    /**
     * 设置UPnP连接是否开启
     * @param isMapped
     */
    @Override
    public void setUPnpMapped(boolean isMapped) {
        edit.putBoolean(appContext.getString(R.string.pref_key_upnp_mapped), isMapped)
                .apply();
    }

    /**
     * NAT-PMP连接是否开启
     */
    @Override
    public boolean isNATPMPMapped() {
        return pref.getBoolean(appContext.getString(R.string.pref_key_nat_pmp_mapped), false);
    }

    /**
     * 设置NAT-PMP连接是否开启
     * @param isMapped
     */
    @Override
    public void setNATPMPMapped(boolean isMapped) {
        edit.putBoolean(appContext.getString(R.string.pref_key_nat_pmp_mapped), isMapped)
                .apply();
    }

    @Override
    public long getLongValue(String key) {
        return pref.getLong(key, 0);
    }

    @Override
    public long getLongValue(String key, long defValue) {
        return pref.getLong(key, defValue);
    }

    @Override
    public void setLongValue(String key, long value) {
        edit.putLong(key, value)
                .apply();
    }

    @Override
    public int getIntValue(String key) {
        return pref.getInt(key, 0);
    }

    @Override
    public int getIntValue(String key, int defValue) {
        return pref.getInt(key, defValue);
    }

    @Override
    public void setIntValue(String key, int value) {
        edit.putInt(key, value)
                .apply();
    }

    @Override
    public boolean getBooleanValue(String key) {
        return pref.getBoolean(key, false);
    }

    @Override
    public boolean getBooleanValue(String key, boolean defValue) {
        return pref.getBoolean(key, defValue);
    }

    @Override
    public void setBooleanValue(String key, boolean value) {
        edit.putBoolean(key, value)
                .apply();
    }

    @Override
    public String getStringValue(String key, String defValue) {
        return pref.getString(key, defValue);
    }

    @Override
    public void setStringValue(String key, String value) {
        edit.putString(key, value)
                .apply();
    }

    @Override
    public float getFloatValue(String key, float defValue) {
        return pref.getFloat(key, defValue);
    }

    @Override
    public void setFloatValue(String key, float value) {
        edit.putFloat(key, value)
                .apply();
    }

    @Override
    public void setCpuUsage(float usage) {
        edit.putFloat(appContext.getString(R.string.pref_key_cpu_usage), usage)
                .apply();
        setCpuAverageUsage(usage);
    }

    @Override
    public float getCpuUsage() {
        return pref.getFloat(appContext.getString(R.string.pref_key_cpu_usage), 0);
    }

    @Override
    public void setCpuAverageUsage(float usage) {
        String key = appContext.getString(R.string.pref_key_cpu_average_usage);
        float average = pref.getFloat(key, 0);
        if (average > 0) {
            average = (average * Default.cpu_sample + usage) / (Default.cpu_sample + 1);
        } else {
            average = usage;
        }

        edit.putFloat(key, average).apply();
    }

    @Override
    public float getAverageCpuUsage() {
        String key = appContext.getString(R.string.pref_key_cpu_average_usage);
        return pref.getFloat(key, 0);
    }

    @Override
    public void setMemoryUsage(long usage) {
        edit.putLong(appContext.getString(R.string.pref_key_memory_usage), usage)
                .apply();
        setMemoryAverageUsage(usage);
    }

    @Override
    public long getMemoryUsage() {
        return pref.getLong(appContext.getString(R.string.pref_key_memory_usage), 0);
    }

    @Override
    public void setMaxMemoryLimit(long maxLimit) {
        edit.putLong(appContext.getString(R.string.pref_key_memory_max_limit), maxLimit)
                .apply();
    }

    @Override
    public long getMaxMemoryLimit() {
        return pref.getLong(appContext.getString(R.string.pref_key_memory_max_limit), 0);
    }

    @Override
    public void setMemoryAverageUsage(long usage) {
        String key = appContext.getString(R.string.pref_key_memory_average_usage);
        long average = pref.getLong(key, 0);
        if (average > 0) {
            average = (average * Default.memory_sample + usage) / (Default.memory_sample + 1);
        } else {
            average = usage;
        }

        edit.putLong(key, average).apply();
    }

    @Override
    public long getAverageMemoryUsage() {
        String key = appContext.getString(R.string.pref_key_memory_average_usage);
        return pref.getLong(key, 0);
    }

    @Override
    public void initData() {
        Context context = MainApplication.getInstance();
        setUPnpMapped(false);
        setNATPMPMapped(false);
        edit.putFloat(context.getString(R.string.pref_key_cpu_average_usage), 0).apply();
        edit.putLong(context.getString(R.string.pref_key_memory_average_usage), 0).apply();
        edit.putString(context.getString(R.string.pref_key_network_interfaces), "").apply();
    }
}