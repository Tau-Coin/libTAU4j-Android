package io.taucbd.news.publishing.ui.main;

import android.os.Bundle;
import android.text.SpannableStringBuilder;

import androidx.databinding.DataBindingUtil;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.utils.SpanUtils;
import io.taucbd.news.publishing.databinding.ActivityCrytoCitiesBinding;
import io.taucbd.news.publishing.ui.BaseActivity;

public class CryptoCitiesActivity extends BaseActivity {
    private ActivityCrytoCitiesBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_cryto_cities);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.drawer_tau_communities);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        SpannableStringBuilder spannableStringBuilder = new SpanUtils()
                .append("TAU Communities are public places for chats and news. These communities have logos.  The coins economics is as following:")
                .append("\n\n")
                .append("- Initial Volume: ")
                .append("10,000,000")
                .setForegroundColor(getResources().getColor(R.color.color_yellow))
                .setBold()
                .setFontSize(20, true)
                .append(" coins").append("\n")
                .append("- Mining Rewards: ")
                .append("10")
                .setForegroundColor(getResources().getColor(R.color.color_yellow))
                .setBold()
                .setFontSize(20, true)
                .append(" coins each 5 minutes for winner.")
                .create();
        binding.tvLondonPmcDesc.setText(spannableStringBuilder);
    }
}