package io.taucoin.torrent.publishing.core.storage.sp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.Set;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposables;
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
        static final long heap_sample = 5;                  // Heap采样大小
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
    public long lastTxFee(String chainID){
        String key = appContext.getString(R.string.pref_key_last_tx_fee) + chainID;
        return pref.getLong(key, 0);
    }

    @Override
    public void lastTxFee(String chainID, long fee){
        String key = appContext.getString(R.string.pref_key_last_tx_fee) + chainID;
        edit.putLong(key, fee).apply();
    }

    @Override
    public void doNotShowBanDialog(boolean isShow) {
        String key = appContext.getString(R.string.pref_key_do_not_show_ban_dialog);
        edit.putBoolean(key, isShow).apply();
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
    }

    @Override
    public float getCpuUsage() {
        return pref.getFloat(appContext.getString(R.string.pref_key_cpu_usage), 0);
    }

    @Override
    public void setMemoryUsage(long usage) {
        edit.putLong(appContext.getString(R.string.pref_key_memory_usage), usage)
                .apply();
    }

    @Override
    public long getMemoryUsage() {
        return pref.getLong(appContext.getString(R.string.pref_key_memory_usage), 0);
    }

    @Override
    public void setCurrentHeapSize(long heapSize) {
        edit.putLong(appContext.getString(R.string.pref_key_current_heap_size), heapSize)
                .apply();
    }

    @Override
    public long getCurrentHeapSize() {
        return pref.getLong(appContext.getString(R.string.pref_key_current_heap_size), 0);
    }

    /**
     * 获取网络接口
     */
    @Override
    public String getNetworkInterfaces() {
        String key = appContext.getString(R.string.pref_key_network_interfaces);
        return pref.getString(key, "");
    }

    /**
     * 设置网络接口
     */
    @Override
    public void setNetworkInterfaces(String interfaces) {
        edit.putString(appContext.getString(R.string.pref_key_network_interfaces), interfaces).apply();
    }

    @Override
    public void initData() {
        setUPnpMapped(false);
        setNATPMPMapped(false);
        setNetworkInterfaces("");
        edit.putFloat(appContext.getString(R.string.pref_key_cpu_usage), 0).apply();
        edit.putLong(appContext.getString(R.string.pref_key_current_heap_size), 0).apply();
        // 初始化主循环频率

        edit.putLong(appContext.getString(R.string.pref_key_dht_invoked_requests), 0).apply();
        edit.putLong(appContext.getString(R.string.pref_key_dht_nodes), 0).apply();
    }

    @Override
    public Set<String> getFiltersSelected() {
        String key = appContext.getString(R.string.pref_key_community_filters_selected);
        return pref.getStringSet(key, null);
    }

    @Override
    public void setFiltersSelected(Set<String> filters) {
        edit.putStringSet(appContext.getString(R.string.pref_key_community_filters_selected),
                filters).apply();
    }

    @Override
    public void setTauDozeTime(long time, boolean isForeground) {
        String key;
        if (isForeground) {
            key = appContext.getString(R.string.pref_key_tau_fore_doze_time);
        } else {
            key = appContext.getString(R.string.pref_key_tau_back_doze_time);
        }
        edit.putLong(key, time).apply();
    }

    @Override
    public void updateTauDozeTime(long time, boolean isForeground) {
        long dozeTime = getTauDozeTime(isForeground) + time;
        setTauDozeTime(dozeTime, isForeground);
    }

    @Override
    public long getTauDozeTime(boolean isForeground) {
        String key;
        if (isForeground) {
            key = appContext.getString(R.string.pref_key_tau_fore_doze_time);
        } else {
            key = appContext.getString(R.string.pref_key_tau_back_doze_time);
        }
        return pref.getLong(key, 0);
    }
}