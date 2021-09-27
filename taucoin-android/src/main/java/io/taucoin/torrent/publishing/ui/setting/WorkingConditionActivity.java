package io.taucoin.torrent.publishing.ui.setting;

import android.os.Bundle;

import java.util.Locale;

import androidx.databinding.DataBindingUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.utils.AppUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.DeviceUtils;
import io.taucoin.torrent.publishing.core.utils.Formatter;
import io.taucoin.torrent.publishing.core.utils.NetworkSetting;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.ActivityWorkingConditionBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;

/**
 * 工况页面
 */
public class WorkingConditionActivity extends BaseActivity {

    private ActivityWorkingConditionBinding binding;
    private SettingsRepository settingsRepo;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsRepo = RepositoryHelper.getSettingsRepository(getApplicationContext());
        binding = DataBindingUtil.setContentView(this, R.layout.activity_working_condition);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.drawer_working_condition);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        binding.tvProcessors.setText(String.valueOf(availableProcessors));
    }

    @Override
    public void onStart() {
        super.onStart();
        handleSettingsChanged(getString(R.string.pref_key_charging_state));
        handleSettingsChanged(getString(R.string.pref_key_is_metered_network));
        handleSettingsChanged(getString(R.string.pref_key_network_interfaces));
        handleSettingsChanged(getString(R.string.pref_key_dht_invoke));
        handleSettingsChanged(getString(R.string.pref_key_cpu_usage));
        handleSettingsChanged(getString(R.string.pref_key_memory_usage));

        disposables.add(settingsRepo.observeSettingsChanged()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleSettingsChanged));
    }

    /**
     * 用户设置参数变化
     * @param key
     */
    private void handleSettingsChanged(String key) {
        if (StringUtil.isEquals(key, getString(R.string.pref_key_charging_state))) {
            boolean chargingState = settingsRepo.chargingState();
            binding.tvChargingState.setText(chargingState ? R.string.common_on : R.string.common_off);
        } else if (StringUtil.isEquals(key, getString(R.string.pref_key_is_metered_network))) {
            boolean isMeteredNetwork = NetworkSetting.isMeteredNetwork();
            binding.tvNetworkType.setText(isMeteredNetwork ? R.string.setting_metered : R.string.setting_wifi);

            handleSettingsChanged(getString(R.string.pref_key_wifi_limit));
        } else if (StringUtil.isEquals(key, getString(R.string.pref_key_wifi_limit)) ||
                StringUtil.isEquals(key, getString(R.string.pref_key_metered_limit))) {
            String wifiLimitStr = getDataLimit(NetworkSetting.isMeteredNetwork());
            binding.tvDataLimit.setText(wifiLimitStr);
        } else if (StringUtil.isEquals(key, getString(R.string.pref_key_network_interfaces))) {
            String networkInterfaces = settingsRepo.getStringValue(key, "");
            binding.tvNetworkInterfaces.setText(networkInterfaces);
        } else if (StringUtil.isEquals(key, getString(R.string.pref_key_dht_invoke))) {
            int invoke = settingsRepo.getIntValue(key, 0);
            binding.tvDhtInvoke.setText(String.valueOf(invoke));
        } else if (StringUtil.isEquals(key, getString(R.string.pref_key_cpu_usage))) {
            float cpuUsage = settingsRepo.getCpuUsage();
            String cpuUsageStr = String.format(Locale.CHINA, "%.2f", cpuUsage) + "%";
            binding.tvCpu.setText(cpuUsageStr);
        } else if (StringUtil.isEquals(key, getString(R.string.pref_key_memory_usage))) {
            long memoryUsage = settingsRepo.getMemoryUsage();
            String memoryUsageStr = Formatter.formatFileSize(this, memoryUsage);
            binding.tvMemory.setText(memoryUsageStr);
        }
    }

    private String getDataLimit(boolean isMetered) {
        int dataLimit;
        if (isMetered) {
            dataLimit = NetworkSetting.getMeteredLimit();
        } else {
            dataLimit = NetworkSetting.getWiFiLimit();
        }
        if (dataLimit >= 1024) {
            return getString(R.string.setting_daily_quota_unit_g, dataLimit / 1024);
        } else {
            return getString(R.string.setting_daily_quota_unit_m, dataLimit);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }
}