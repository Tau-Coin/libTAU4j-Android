package io.taucbd.news.publishing.ui.setting;

import android.os.Bundle;
import android.view.View;

import java.util.Locale;

import androidx.databinding.DataBindingUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucbd.news.publishing.BuildConfig;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.storage.RepositoryHelper;
import io.taucbd.news.publishing.core.storage.sp.SettingsRepository;
import io.taucbd.news.publishing.core.utils.ActivityUtil;
import io.taucbd.news.publishing.core.utils.DateUtil;
import io.taucbd.news.publishing.core.utils.Formatter;
import io.taucbd.news.publishing.core.utils.NetworkSetting;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.databinding.ActivityWorkingConditionBinding;
import io.taucbd.news.publishing.ui.BaseActivity;

/**
 * 工况页面
 */
public class WorkingConditionActivity extends BaseActivity implements View.OnClickListener {

    private ActivityWorkingConditionBinding binding;
    private SettingsRepository settingsRepo;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsRepo = RepositoryHelper.getSettingsRepository(getApplicationContext());
        binding = DataBindingUtil.setContentView(this, R.layout.activity_working_condition);
        binding.setListener(this);
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

        if (!BuildConfig.DEBUG) {
            binding.llCpu.setVisibility(View.GONE);
            binding.lineCpu.setVisibility(View.GONE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        handleSettingsChanged(getString(R.string.pref_key_charging_state));
        handleSettingsChanged(getString(R.string.pref_key_internet_state));
        handleSettingsChanged(getString(R.string.pref_key_upnp_mapped));
        handleSettingsChanged(getString(R.string.pref_key_nat_pmp_mapped));
        handleSettingsChanged(getString(R.string.pref_key_is_wifi_network));
        handleSettingsChanged(getString(R.string.pref_key_network_interfaces));
        handleSettingsChanged(getString(R.string.pref_key_dht_invoked_requests));
        handleSettingsChanged(getString(R.string.pref_key_dht_nodes));
        handleSettingsChanged(getString(R.string.pref_key_cpu_usage));
        handleSettingsChanged(getString(R.string.pref_key_memory_usage));
        handleSettingsChanged(getString(R.string.pref_key_current_heap_size));
        handleSettingsChanged(getString(R.string.pref_key_current_speed));
        handleSettingsChanged(getString(R.string.pref_key_foreground_running_time));
        handleSettingsChanged(getString(R.string.pref_key_background_running_time));
        handleSettingsChanged(getString(R.string.pref_key_doze_running_time));

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
        } else if (StringUtil.isEquals(key, getString(R.string.pref_key_internet_state))) {
            boolean internetState = settingsRepo.internetState();
            binding.tvInternet.setText(internetState ? R.string.common_on : R.string.common_off);
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_upnp_mapped))) {
            boolean isMapped = settingsRepo.isUPnpMapped();
            binding.tvUpnp.setText(isMapped ? R.string.common_on : R.string.common_off);
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_nat_pmp_mapped))) {
            boolean isMapped = settingsRepo.isNATPMPMapped();
            binding.tvNatPmp.setText(isMapped ? R.string.common_on : R.string.common_off);
        } else if (StringUtil.isEquals(key, getString(R.string.pref_key_is_wifi_network))) {
            boolean isMeteredNetwork = !NetworkSetting.isWiFiNetwork();
            binding.tvNetworkType.setText(isMeteredNetwork ? R.string.setting_metered : R.string.setting_wifi);
        } else if (StringUtil.isEquals(key, getString(R.string.pref_key_network_interfaces))) {
            String networkInterfaces = settingsRepo.getStringValue(key, "");
            if (StringUtil.isEquals(networkInterfaces, "0.0.0.0")) {
                networkInterfaces = "";
            }
            binding.tvNetworkInterfaces.setText(networkInterfaces);
        } else if (StringUtil.isEquals(key, getString(R.string.pref_key_dht_invoked_requests))) {
            long invoke = settingsRepo.getLongValue(key, 0);
            binding.tvDhtInvoke.setText(String.valueOf(invoke));
        } else if (StringUtil.isEquals(key, getString(R.string.pref_key_dht_nodes))) {
            long nodes = settingsRepo.getLongValue(key, 0);
            binding.tvPeers.setText(String.valueOf(nodes));
        } else if (StringUtil.isEquals(key, getString(R.string.pref_key_cpu_usage))) {
            float cpuUsage = settingsRepo.getCpuUsage();
            String cpuUsageStr = String.format(Locale.CHINA, "%.2f", cpuUsage) + "%";
            binding.tvCpu.setText(cpuUsageStr);
        } else if (StringUtil.isEquals(key, getString(R.string.pref_key_memory_usage))) {
            long memoryUsage = settingsRepo.getMemoryUsage();
            String memoryUsageStr = Formatter.formatFileSize(this, memoryUsage);
            binding.tvMemory.setText(memoryUsageStr);
        } else if (StringUtil.isEquals(key, getString(R.string.pref_key_current_heap_size))) {
            long heapSize = settingsRepo.getCurrentHeapSize();
            String heapSizeStr = Formatter.formatFileSize(this, heapSize);
            binding.tvHeapSize.setText(heapSizeStr);
        } else if (StringUtil.isEquals(key, getString(R.string.pref_key_foreground_running_time))) {
            int foreRun = NetworkSetting.getForegroundRunningTime();
            String foregroundTimeStr = DateUtil.getFormatTime(foreRun);
            binding.tvForeRunningTime.setText(foregroundTimeStr);
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_background_running_time))) {
            int backgroundTime = NetworkSetting.getBackgroundRunningTime();
            long androidDoze = NetworkSetting.getDozeTime();
            long bgRun = backgroundTime - androidDoze;
            bgRun = bgRun < 0 ? 0 : bgRun;
            String backgroundTimeStr = DateUtil.getFormatTime(bgRun);
            binding.tvBgRunningTime.setText(backgroundTimeStr);
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_doze_running_time))) {
            int dozeTime = NetworkSetting.getDozeTime();
            String dozeTimeStr = DateUtil.getFormatTime(dozeTime);
            binding.tvDozeRunningTime.setText(dozeTimeStr);
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_doze_running_time))) {
            int dozeTime = NetworkSetting.getDozeTime();
            String dozeTimeStr = DateUtil.getFormatTime(dozeTime);
            binding.tvDozeRunningTime.setText(dozeTimeStr);
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_current_speed))) {
            boolean internetState = settingsRepo.internetState();
            if (internetState) {
                long currentSpeed = NetworkSetting.getCurrentSpeed();
                String currentSpeedStr = getString(R.string.setting_metered_network_limit_speed,
                        Formatter.formatFileSize(this, currentSpeed).toUpperCase());
                binding.tvSpeed.setText(currentSpeedStr);
            } else {
                String noSpeedStr = getString(R.string.setting_metered_network_limit_speed,
                        Formatter.formatFileSize(this, 0).toUpperCase());
                binding.tvSpeed.setText(noSpeedStr);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.ll_journal) {
            ActivityUtil.startActivity(this, JournalActivity.class);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }
}