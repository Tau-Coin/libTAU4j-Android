package io.taucoin.news.publishing.ui.setting;

import android.os.Bundle;
import android.widget.SeekBar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.databinding.DataBindingUtil;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.Constants;
import io.taucoin.news.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.news.publishing.core.storage.RepositoryHelper;
import io.taucoin.news.publishing.databinding.ActivityFontSizeBinding;
import io.taucoin.news.publishing.ui.BaseActivity;

/**
 * 字体大小设置页面
 */
public class FontSizeActivity extends BaseActivity {

    public static final int REQUEST_CODE_FONT_SIZE = 0x0001;
    private static final Logger logger = LoggerFactory.getLogger("FontSizeActivity");
    private ActivityFontSizeBinding binding;
    private SettingsRepository settingsRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_font_size);
        settingsRepo = RepositoryHelper.getSettingsRepository(this);
        initView();
    }

    @Override
    protected void refreshAllView() {
        binding = DataBindingUtil.setContentView(FontSizeActivity.this, R.layout.activity_font_size);
        initView();
        setResult(RESULT_OK);
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.setting_font_size);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        String[] scaleTitles = getResources().getStringArray(R.array.font_scale_title);
        String[] scaleSizes = getResources().getStringArray(R.array.font_scale_size);

        binding.seekBar.setFontScaleSizes(scaleSizes);
        binding.seekBar.setFontScaleTitles(scaleTitles);

        String fontKey = getString(R.string.pref_key_font_scale_size);
        float fontScaleSize = settingsRepo.getFloatValue(fontKey, Constants.DEFAULT_FONT_SCALE_SIZE);
        binding.seekBar.setFontScaleSize(fontScaleSize);
        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float scaleSize = binding.seekBar.getFontScaleSize(progress);
                logger.debug("scaleSize::{}", scaleSize);
                settingsRepo.setFloatValue("pref_key_font_scale_size", scaleSize);
                refreshAllView();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
}