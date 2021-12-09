package io.taucoin.torrent.publishing.ui.setting;

import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.primitives.Ints;

import java.util.List;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.DrawablesUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.Formatter;
import io.taucoin.torrent.publishing.core.utils.FrequencyUtil;
import io.taucoin.torrent.publishing.core.utils.MoneyValueFilter;
import io.taucoin.torrent.publishing.core.utils.NetworkSetting;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.TrafficUtil;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivityDataCostBinding;
import io.taucoin.torrent.publishing.databinding.EditFixedFrequencyDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;

/**
 * 仪表板页面
 */
public class DataCostActivity extends BaseActivity implements DailyQuotaAdapter.OnCheckedChangeListener,
        CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    private ActivityDataCostBinding binding;
    private SettingsRepository settingsRepo;
    private CompositeDisposable disposables = new CompositeDisposable();
    private DailyQuotaAdapter adapterMetered;
    private DailyQuotaAdapter adapterWiFi;
    private CommonDialog commonDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsRepo = RepositoryHelper.getSettingsRepository(getApplicationContext());
        binding = DataBindingUtil.setContentView(this, R.layout.activity_data_cost);
        binding.setListener(this);
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
                DailyQuotaAdapter.TYPE_METERED, NetworkSetting.getMeteredLimit());
        binding.rvMeteredDailyQuota.setAdapter(adapterMetered);
        int[] meteredLimits = getResources().getIntArray(R.array.metered_limit);
        List<Integer> meteredList = Ints.asList(meteredLimits);
        adapterMetered.submitList(meteredList);

        LinearLayoutManager layoutManagerWiFi = new LinearLayoutManager(this);
        layoutManagerWiFi.setOrientation(RecyclerView.HORIZONTAL);
        binding.rvWifiDailyQuota.setLayoutManager(layoutManagerWiFi);
        adapterWiFi = new DailyQuotaAdapter(this,
                DailyQuotaAdapter.TYPE_WIFI, NetworkSetting.getWiFiLimit());
        binding.rvWifiDailyQuota.setAdapter(adapterWiFi);
        int[] wifiLimits = getResources().getIntArray(R.array.wifi_limit);
        List<Integer> wifiList = Ints.asList(wifiLimits);
        adapterWiFi.submitList(wifiList);

        refreshAllData();
        changeTabView(R.id.tab_foreground);
        handleSettingsChanged(getString(R.string.pref_key_wifi_fixed_frequency));
        handleSettingsChanged(getString(R.string.pref_key_metered_fixed_frequency));

        DrawablesUtil.setEndDrawable(binding.tvMeteredFixedFrequency, R.mipmap.icon_arrow,
                getResources().getDimension(R.dimen.widget_size_12));
        DrawablesUtil.setEndDrawable(binding.tvWifiFixedFrequency, R.mipmap.icon_arrow,
                getResources().getDimension(R.dimen.widget_size_12));
    }

    private void refreshAllData() {
        handleSettingsChanged(getString(R.string.pref_key_is_metered_network));
        handleSettingsChanged(getString(R.string.pref_key_current_speed));
        handleSettingsChanged(getString(R.string.pref_key_main_loop_frequency));
        handleSettingsChanged(getString(R.string.pref_key_foreground_running_time));

        // 先更新，再显示
        NetworkSetting.updateMeteredSpeedLimit();
        handleSettingsChanged(getString(R.string.pref_key_metered_average_speed));
        handleSettingsChanged(getString(R.string.pref_key_metered_available_data));
        NetworkSetting.updateWiFiSpeedLimit();
        handleSettingsChanged(getString(R.string.pref_key_wifi_average_speed));
        handleSettingsChanged(getString(R.string.pref_key_wifi_available_data));
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        NetworkSetting.enableBackgroundMode(isChecked);
    }

    @Override
    public void onCheckedChanged(int type, int limit) {
        if (type == DailyQuotaAdapter.TYPE_METERED) {
            NetworkSetting.setMeteredLimit(limit, true);
            NetworkSetting.updateMeteredSpeedLimit();
        } else if (type == DailyQuotaAdapter.TYPE_WIFI) {
            NetworkSetting.setWiFiLimit(limit, true);
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
        if(StringUtil.isEquals(key, getString(R.string.pref_key_current_speed))) {
            long currentSpeed = NetworkSetting.getCurrentSpeed();
            String currentSpeedStr = getString(R.string.setting_metered_network_limit_speed,
                    Formatter.formatFileSize(this, currentSpeed).toUpperCase());
            String noSpeedStr = getString(R.string.setting_metered_network_limit_speed,
                    Formatter.formatFileSize(this, 0).toUpperCase());
            boolean internetState = settingsRepo.internetState();
            boolean meteredNetwork = NetworkSetting.isMeteredNetwork();
            binding.tvMeteredCurrentSpeed.setText(internetState && meteredNetwork ? currentSpeedStr : noSpeedStr);
            binding.tvWifiCurrentSpeed.setText(internetState && !meteredNetwork ? currentSpeedStr : noSpeedStr);
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_metered_average_speed))) {
            long averageSpeed = NetworkSetting.getMeteredAverageSpeed();
            String averageSpeedStr = Formatter.formatFileSize(this, averageSpeed).toUpperCase();
            averageSpeedStr = getString(R.string.setting_metered_network_limit_speed, averageSpeedStr);
            binding.tvMeteredAverageSpeed.setText(averageSpeedStr);
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_wifi_average_speed))) {
            long averageSpeed = NetworkSetting.getWifiAverageSpeed();
            String averageSpeedStr = Formatter.formatFileSize(this, averageSpeed).toUpperCase();
            averageSpeedStr = getString(R.string.setting_metered_network_limit_speed, averageSpeedStr);
            binding.tvWifiAverageSpeed.setText(averageSpeedStr);
        } else if (key.equals(getString(R.string.pref_key_is_metered_network))) {
            boolean internetState = settingsRepo.internetState();
            boolean meteredNetwork = NetworkSetting.isMeteredNetwork();
            binding.ivMeteredState.setVisibility(internetState && meteredNetwork ? View.VISIBLE : View.INVISIBLE);
            binding.ivWifiState.setVisibility(internetState && !meteredNetwork ? View.VISIBLE : View.INVISIBLE);

            if (meteredNetwork) {
                binding.llRoot.removeView(binding.llMetered);
                binding.llRoot.addView(binding.llMetered, 2);
            } else {
                binding.llRoot.removeView(binding.llWifi);
                binding.llRoot.addView(binding.llWifi, 2);
            }
        } else if (key.equals(getString(R.string.pref_key_internet_state))) {
            handleSettingsChanged(getString(R.string.pref_key_is_metered_network));
        } else if (key.equals(getString(R.string.pref_key_metered_available_data))) {
            long availableData = NetworkSetting.getMeteredAvailableData();
            String availableDataStr = Formatter.formatFileSize(this, availableData).toUpperCase();
            binding.tvMeteredAvailableData.setText(availableDataStr);
        } else if (key.equals(getString(R.string.pref_key_wifi_available_data))) {
            long availableData = NetworkSetting.getWiFiAvailableData();
            String availableDataStr = Formatter.formatFileSize(this, availableData).toUpperCase();
            binding.tvWifiAvailableData.setText(availableDataStr);
        } else if (key.equals(getString(R.string.pref_key_metered_limit)) ||
                key.equals(getString(R.string.pref_key_metered_prompt_limit))) {
            adapterMetered.updateSelectLimit(NetworkSetting.getMeteredLimit());
            NetworkSetting.updateMeteredSpeedLimit();
        } else if (key.equals(getString(R.string.pref_key_wifi_limit)) ||
                key.equals(getString(R.string.pref_key_wifi_prompt_limit))) {
            adapterWiFi.updateSelectLimit(NetworkSetting.getWiFiLimit());
            NetworkSetting.updateWiFiSpeedLimit();
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_main_loop_frequency))) {
            double frequency = FrequencyUtil.getMainLoopFrequency();
            binding.tvWorkingFrequency.setRightText(FmtMicrometer.formatTwoDecimal(frequency));
        } else if (StringUtil.isEquals(key, getString(R.string.pref_key_foreground_running_time))) {
            int foregroundTime = NetworkSetting.getForegroundRunningTime();
            String foregroundTimeStr = DateUtil.getFormatTime(foregroundTime);
            binding.tvForeRunningTime.setRightText(foregroundTimeStr);
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_background_running_time))) {
            int backgroundTime = NetworkSetting.getBackgroundRunningTime();
            String backgroundTimeStr = DateUtil.getFormatTime(backgroundTime);
            binding.tvBgRunningTime.setRightText(backgroundTimeStr);
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_doze_running_time))) {
            int dozeTime = NetworkSetting.getDozeTime();
            String dozeTimeStr = DateUtil.getFormatTime(dozeTime);
            binding.tvDozeRunningTime.setRightText(dozeTimeStr);
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_metered_fixed_frequency))) {
            double frequency = FrequencyUtil.getMeteredFixedFrequency();
            binding.tvMeteredFixedFrequency.setText(FmtMicrometer.formatTwoDecimal(frequency));
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_wifi_fixed_frequency))) {
            double frequency = FrequencyUtil.getWifiFixedFrequency();
            binding.tvWifiFixedFrequency.setText(FmtMicrometer.formatTwoDecimal(frequency));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tab_foreground:
            case R.id.tab_background:
                changeTabView(v.getId());
                break;
            case R.id.tv_metered_fixed_frequency:
            case R.id.tv_wifi_fixed_frequency:
                showEditFixedFrequencyDialog((TextView) v);
                break;
        }
    }

    private void changeTabView(int id) {
        boolean isForeground = id == R.id.tab_foreground;
        int colorSelected = getResources().getColor(R.color.color_white);
        int colorDefault = getResources().getColor(R.color.colorPrimary);

        binding.tabForeground.setBackgroundColor(isForeground ? colorSelected : colorDefault);
        binding.tabForeground.setTextColor(!isForeground ? colorSelected : colorDefault);
        binding.tabBackground.setBackgroundColor(!isForeground ? colorSelected : colorDefault);
        binding.tabBackground.setTextColor(isForeground ? colorSelected : colorDefault);

        showDataDetailView(binding.llMetered, isForeground);
        showDataDetailView(binding.llWifi, isForeground);
    }

    private void showDataDetailView(LinearLayout groupView, boolean isForeground) {
        int childCount = groupView.getChildCount();
        for (int i = 1; i < childCount - 1; i++) {
            View child = groupView.getChildAt(i);
            if (i >= childCount - 3 ) {
                // Fixed Frequency
                child.setVisibility(!isForeground ? View.GONE : View.VISIBLE);
            } else {
                child.setVisibility(isForeground ? View.GONE : View.VISIBLE);
            }
        }
    }

    /**
     * 显示编辑固定频率的对话框
     */
    void showEditFixedFrequencyDialog(TextView textView) {
        String currentValue = ViewUtils.getText(textView);
        EditFixedFrequencyDialogBinding binding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.edit_fixed_frequency_dialog, null, false);
        binding.etValue.setText(currentValue);
        binding.etValue.setFilters(new InputFilter[]{new MoneyValueFilter()});

        commonDialog = new CommonDialog.Builder(this)
                .setContentView(binding.getRoot())
                .setPositiveButton(R.string.common_submit, (dialog, which) -> {
                    dialog.cancel();
                    float value = ViewUtils.getFloatText(binding.etValue);
                    if (value > 0) {
                        textView.setText(FmtMicrometer.formatTwoDecimal(value));
                        if (textView.getId() == R.id.tv_wifi_fixed_frequency) {
                            FrequencyUtil.setWifiFixedFrequency(value);
                        } else {
                            FrequencyUtil.setMeteredFixedFrequency(value);
                        }
                    }
                })
                .create();
        commonDialog.show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (commonDialog != null && commonDialog.isShowing()) {
            commonDialog.closeDialog();
        }
    }
}