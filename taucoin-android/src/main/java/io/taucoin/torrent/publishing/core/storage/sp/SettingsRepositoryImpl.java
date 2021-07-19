package io.taucoin.torrent.publishing.core.storage.sp;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposables;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.StringUtil;

/**
 * SettingsRepository: 用户设置的接口的实现
 */
public class SettingsRepositoryImpl implements SettingsRepository {
    private static class Default {
        static final boolean bootStart = true;
        static final boolean chargingState = false;
        static final boolean internetState = false;
        static final int internetType = -1;
        static final boolean wakeLock = false;
        static final boolean isShowBanDialog = false;
    }

    private Context appContext;
    private SharedPreferences pref;

    public SettingsRepositoryImpl(@NonNull Context appContext) {
        this.appContext = appContext;
        pref = PreferenceManager.getDefaultSharedPreferences(appContext);
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
    public boolean bootStart() {
        return pref.getBoolean(appContext.getString(R.string.pref_key_boot_start),
                Default.bootStart);
    }

    @Override
    public void bootStart(boolean val) {
        pref.edit().putBoolean(appContext.getString(R.string.pref_key_boot_start),val)
                .apply();
    }

    @Override
    public void chargingState(boolean val) {
        pref.edit().putBoolean(appContext.getString(R.string.pref_key_charging_state),val)
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
        pref.edit().putBoolean(appContext.getString(R.string.pref_key_internet_state),val)
                .apply();
    }

    @Override
    public void setInternetType(int type) {
        pref.edit().putInt(appContext.getString(R.string.pref_key_internet_type), type)
                .apply();
    }

    @Override
    public int getInternetType() {
        return pref.getInt(appContext.getString(R.string.pref_key_internet_type),
                Default.internetType);
    }

    @Override
    public boolean wakeLock() {
        return pref.getBoolean(appContext.getString(R.string.pref_key_wake_lock),
                Default.wakeLock);
    }

    @Override
    public void wakeLock(boolean val) {
        pref.edit().putBoolean(appContext.getString(R.string.pref_key_wake_lock),val)
                .apply();
    }

    @Override
    public String lastTxFee(String chainID){
        String key = appContext.getString(R.string.pref_key_last_tx_fee) + chainID;
        return pref.getString(key, "");
    }

    @Override
    public void lastTxFee(String chainID, String fee){
        String key = appContext.getString(R.string.pref_key_last_tx_fee) + chainID;
        pref.edit().putString(key, fee)
                .apply();
    }

    @Override
    public void doNotShowBanDialog(boolean isShow) {
        String key = appContext.getString(R.string.pref_key_do_not_show_ban_dialog);
        pref.edit().putBoolean(key, isShow)
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
        pref.edit().putLong(key, downloadID)
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
        pref.edit().putBoolean(appContext.getString(R.string.pref_key_need_prompt_user), isNeed)
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
        pref.edit().putBoolean(appContext.getString(R.string.pref_key_upnp_mapped), isMapped)
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
        pref.edit().putBoolean(appContext.getString(R.string.pref_key_nat_pmp_mapped), isMapped)
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
        pref.edit().putLong(key, value)
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
        pref.edit().putInt(key, value)
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
        pref.edit().putBoolean(key, value)
                .apply();
    }

    @Override
    public <T> void setListData(String key, List<T> list) {
        try {
            Gson gson = new Gson();
            String jsonArray = gson.toJson(list);
            pref.edit().putString(key, jsonArray)
                    .apply();
        } catch (Exception ignore) {
        }
    }

    @Override
    public <T> List<T> getListData(String key, Class<T> cls) {
        List<T> list = new ArrayList<>();
        String jsonStr = pref.getString(key, "");
        if (StringUtil.isNotEmpty(jsonStr)) {
            try {
                Gson gson = new Gson();
                JsonArray array = JsonParser.parseString(jsonStr).getAsJsonArray();
                for (JsonElement elem : array) {
                    list.add(gson.fromJson(elem, cls));
                }
            } catch (Exception ignore) {
            }
        }
        return list;
    }

    @Override
    public String getStringValue(String key, String defValue) {
        return pref.getString(key, defValue);
    }

    @Override
    public void setStringValue(String key, String value) {
        pref.edit().putString(key, value)
                .apply();
    }

    @Override
    public float getFloatValue(String key, float defValue) {
        return pref.getFloat(key, defValue);
    }

    @Override
    public void setFloatValue(String key, float value) {
        pref.edit().putFloat(key, value)
                .apply();
    }

    @Override
    public void setChattingFriend(String friend) {
        pref.edit().putString(appContext.getString(R.string.pref_key_chatting_friend), friend)
                .apply();
    }

    @Override
    public String getChattingFriend() {
        return pref.getString(appContext.getString(R.string.pref_key_chatting_friend), null);
    }

    @Override
    public void setCpuUsage(String usage) {
        pref.edit().putString(appContext.getString(R.string.pref_key_cpu_usage), usage)
                .apply();
    }

    @Override
    public String getCpuUsage() {
        return pref.getString(appContext.getString(R.string.pref_key_cpu_usage), "0%");
    }

    @Override
    public void setMemoryUsage(long usage) {
        pref.edit().putLong(appContext.getString(R.string.pref_key_memory_usage), usage)
                .apply();
    }

    @Override
    public long getMemoryUsage() {
        return pref.getLong(appContext.getString(R.string.pref_key_memory_usage), 0);
    }
}