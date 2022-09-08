package io.taucoin.tauapp.publishing.ui.setting;

import android.os.Bundle;
import android.view.View;

import com.google.common.primitives.Ints;

import java.util.List;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.tauapp.publishing.core.storage.RepositoryHelper;
import io.taucoin.tauapp.publishing.core.utils.DateUtil;
import io.taucoin.tauapp.publishing.core.utils.Formatter;
import io.taucoin.tauapp.publishing.core.utils.NetworkSetting;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.core.utils.TrafficUtil;
import io.taucoin.tauapp.publishing.databinding.ActivityDataCostBinding;
import io.taucoin.tauapp.publishing.ui.BaseActivity;

/**
 * 仪表板页面
 */
public class DataCostActivity extends BaseActivity implements DailyQuotaAdapter.OnCheckedChangeListener {

    private ActivityDataCostBinding binding;
    private SettingsRepository settingsRepo;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private DailyQuotaAdapter adapterMetered;
    private DailyQuotaAdapter adapterWiFi;
    private Object isUnlimitedNetwork = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsRepo = RepositoryHelper.getSettingsRepository(getApplicationContext());
        binding = DataBindingUtil.setContentView(this, R.layout.activity_data_cost);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.drawer_data_cost);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        String resetTime = getString(R.string.setting_data_reset_time_value, TrafficUtil.TRAFFIC_RESET_TIME);
        binding.tvResetTime.setRightText(resetTime);

        LinearLayoutManager layoutManagerMetered = new LinearLayoutManager(this);
        layoutManagerMetered.setOrientation(RecyclerView.HORIZONTAL);
        binding.rvMeteredDailyQuota.setLayoutManager(layoutManagerMetered);
        adapterMetered = new DailyQuotaAdapter(this,
                DailyQuotaAdapter.TYPE_METERED, NetworkSetting.getMeteredLimitPos());
        binding.rvMeteredDailyQuota.setAdapter(adapterMetered);
        int[] meteredLimits = NetworkSetting.getMeteredLimits();
        List<Integer> meteredList = Ints.asList(meteredLimits);
        adapterMetered.submitList(meteredList);

        updateUnlimitedNetwork();

        LinearLayoutManager layoutManagerWiFi = new LinearLayoutManager(this);
        layoutManagerWiFi.setOrientation(RecyclerView.HORIZONTAL);
        binding.rvWifiDailyQuota.setLayoutManager(layoutManagerWiFi);
        adapterWiFi = new DailyQuotaAdapter(this,
                DailyQuotaAdapter.TYPE_WIFI, NetworkSetting.getWiFiLimitPos());
        binding.rvWifiDailyQuota.setAdapter(adapterWiFi);
        int[] wifiLimits =  NetworkSetting.getWifiLimits();
        List<Integer> wifiList = Ints.asList(wifiLimits);
        adapterWiFi.submitList(wifiList);

        refreshAllData();

        NetworkSetting.getDevelopCountry().observe(this, developed -> {
            updateUnlimitedNetwork();
        });
        binding.tvResetTime.setOnClickListener(view -> {
            NetworkSetting.updateDevelopCountryTest();
        });
    }

    private void updateUnlimitedNetwork() {
        boolean isUnlimitedNetwork = NetworkSetting.isUnlimitedNetwork();
        if (null == this.isUnlimitedNetwork || Boolean.parseBoolean(this.isUnlimitedNetwork.toString()) != isUnlimitedNetwork) {
            binding.llWifiDailyQuota.setVisibility(isUnlimitedNetwork ? View.GONE : View.VISIBLE);
            binding.llWifiAvailableData.setVisibility(isUnlimitedNetwork ? View.GONE : View.VISIBLE);
            binding.llWifiLine.setVisibility(isUnlimitedNetwork ? View.GONE : View.VISIBLE);
            if (this.isUnlimitedNetwork != null) {
                adapterMetered = new DailyQuotaAdapter(this,
                        DailyQuotaAdapter.TYPE_METERED, NetworkSetting.getMeteredLimitPos());
                binding.rvMeteredDailyQuota.setAdapter(adapterMetered);
                int[] newMeteredLimits = NetworkSetting.getMeteredLimits();
                List<Integer> newMeteredList = Ints.asList(newMeteredLimits);
                adapterMetered.submitList(newMeteredList);
            }
            this.isUnlimitedNetwork = isUnlimitedNetwork;
        }
    }

    private void refreshAllData() {
        handleSettingsChanged(getString(R.string.pref_key_current_speed));
        handleSettingsChanged(getString(R.string.pref_key_foreground_running_time));
        handleSettingsChanged(getString(R.string.pref_key_is_wifi_network));

        // 先更新，再显示
        NetworkSetting.updateMeteredSpeedLimit();
        handleSettingsChanged(getString(R.string.pref_key_metered_available_data));
        NetworkSetting.updateWiFiSpeedLimit();
        handleSettingsChanged(getString(R.string.pref_key_wifi_available_data));
        handleSettingsChanged(getString(R.string.pref_key_charging_state));
    }

    @Override
    public void onCheckedChanged(int type, int pos) {
        if (type == DailyQuotaAdapter.TYPE_METERED) {
            NetworkSetting.setMeteredLimitPos(pos, true);
            NetworkSetting.updateMeteredSpeedLimit();
        } else if (type == DailyQuotaAdapter.TYPE_WIFI) {
            NetworkSetting.setWiFiLimitPos(pos, true);
            NetworkSetting.updateWiFiSpeedLimit();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // 处理从后台切换到前台的情况下，后台时间不更新的问题
        refreshAllData();
        handleSettingsChanged(getString(R.string.pref_key_background_running_time));
        handleSettingsChanged(getString(R.string.pref_key_doze_running_time));
        handleSettingsChanged(getString(R.string.pref_key_tau_fore_doze_time));
        handleSettingsChanged(getString(R.string.pref_key_tau_back_doze_time));
        disposables.add(settingsRepo.observeSettingsChanged()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleSettingsChanged));
    }

    /**
     * 用户设置参数变化
     */
    private void handleSettingsChanged(String key) {
        if(StringUtil.isEquals(key, getString(R.string.pref_key_current_speed))) {
            long currentSpeed = NetworkSetting.getCurrentSpeed();
            String currentSpeedStr = getString(R.string.setting_metered_network_limit_speed,
                    Formatter.formatFileSize(this, currentSpeed).toUpperCase());
            String noSpeedStr = getString(R.string.setting_metered_network_limit_speed,
                    Formatter.formatFileSize(this, 0).toUpperCase());
            boolean internetState = settingsRepo.internetState();
            boolean meteredNetwork = !NetworkSetting.isWiFiNetwork();
            binding.tvMeteredCurrentSpeed.setText(internetState && meteredNetwork ? currentSpeedStr : noSpeedStr);
            binding.tvWifiCurrentSpeed.setText(internetState && !meteredNetwork ? currentSpeedStr : noSpeedStr);
        } else if (key.equals(getString(R.string.pref_key_is_wifi_network))) {
            boolean internetState = settingsRepo.internetState();
            boolean meteredNetwork = !NetworkSetting.isWiFiNetwork();
            binding.ivMeteredState.setVisibility(internetState && meteredNetwork ? View.VISIBLE : View.INVISIBLE);
            binding.ivWifiState.setVisibility(internetState && !meteredNetwork ? View.VISIBLE : View.INVISIBLE);

            if (meteredNetwork) {
                binding.llRoot.removeView(binding.llMetered);
                binding.llRoot.addView(binding.llMetered, 1);
            } else {
                binding.llRoot.removeView(binding.llWifi);
                binding.llRoot.addView(binding.llWifi, 1);
            }
            updateUnlimitedNetwork();
        } else if (key.equals(getString(R.string.pref_key_internet_state))) {
            handleSettingsChanged(getString(R.string.pref_key_is_wifi_network));
        } else if (key.equals(getString(R.string.pref_key_metered_available_data))) {
            long availableData = NetworkSetting.getMeteredAvailableData();
            String availableDataStr = Formatter.formatFileSize(this, availableData).toUpperCase();
            binding.tvMeteredAvailableData.setText(availableDataStr);
            updateUnlimitedNetwork();
        } else if (key.equals(getString(R.string.pref_key_wifi_available_data))) {
            long availableData = NetworkSetting.getWiFiAvailableData();
            String availableDataStr = Formatter.formatFileSize(this, availableData).toUpperCase();
            binding.tvWifiAvailableData.setText(availableDataStr);
        } else if (key.equals(getString(R.string.pref_key_metered_limit)) ||
                key.equals(getString(R.string.pref_key_metered_prompt_limit))) {
            adapterMetered.updateSelectLimitPos(NetworkSetting.getMeteredLimitPos());
            NetworkSetting.updateMeteredSpeedLimit();
        } else if (key.equals(getString(R.string.pref_key_wifi_limit)) ||
                key.equals(getString(R.string.pref_key_wifi_prompt_limit))) {
            adapterWiFi.updateSelectLimitPos(NetworkSetting.getWiFiLimitPos());
            NetworkSetting.updateWiFiSpeedLimit();
        } else if (StringUtil.isEquals(key, getString(R.string.pref_key_foreground_running_time))) {
            int foregroundTime = NetworkSetting.getForegroundRunningTime();
            long dozeTime = settingsRepo.getTauDozeTime(true);
            long foreRun = foregroundTime - dozeTime;
            foreRun = foreRun < 0 ? 0 : foreRun;
            String foregroundTimeStr = DateUtil.getFormatTime(foreRun);
            binding.tvForeRunningTime.setRightText(foregroundTimeStr);
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_background_running_time))) {
            int backgroundTime = NetworkSetting.getBackgroundRunningTime();
            long tauDoze = settingsRepo.getTauDozeTime(false);
            long androidDoze = NetworkSetting.getDozeTime();
            long bgRun = backgroundTime - tauDoze - androidDoze;
            bgRun = bgRun < 0 ? 0 : bgRun;
            String backgroundTimeStr = DateUtil.getFormatTime(bgRun);
            binding.tvBgRunningTime.setRightText(backgroundTimeStr);
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_doze_running_time))) {
            int dozeTime = NetworkSetting.getDozeTime();
            String dozeTimeStr = DateUtil.getFormatTime(dozeTime);
            binding.tvDozeRunningTime.setRightText(dozeTimeStr);
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_tau_fore_doze_time))) {
            long dozeTime = settingsRepo.getTauDozeTime(true);
            String dozeTimeStr = DateUtil.getFormatTime(dozeTime);
            binding.tvForeDozeTime.setRightText(dozeTimeStr);
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_tau_back_doze_time))) {
            long dozeTime = settingsRepo.getTauDozeTime(false);
            String dozeTimeStr = DateUtil.getFormatTime(dozeTime);
            binding.tvBackDozeTime.setRightText(dozeTimeStr);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }
}