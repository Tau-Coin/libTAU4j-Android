package io.taucoin.tauapp.publishing.ui.main;

import android.content.res.Configuration;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.util.DisplayMetrics;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.utils.SpanUtils;
import io.taucoin.tauapp.publishing.core.utils.Utils;
import io.taucoin.tauapp.publishing.databinding.ActivityCrytoCitiesBinding;
import io.taucoin.tauapp.publishing.ui.BaseActivity;

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
        binding.toolbarInclude.toolbar.setTitle(R.string.drawer_crypto_cities);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        SpannableStringBuilder spannableStringBuilder = new SpanUtils()
                .append("1. Initial Volume: ").append("\n")
                .append("1,000,000")
                .setForegroundColor(getResources().getColor(R.color.color_yellow))
                .setBold()
                .setFontSize(20, true)
                .append(" coins").append("\n")
                .append("2. Airdrop: ")
                .append("60%")
                .setForegroundColor(getResources().getColor(R.color.color_yellow))
                .setBold()
                .setFontSize(20, true)
                .append(" for early adopters.").append("\n")
                .append("3. TAU company reserves: ")
                .append("40%").append("\n")
                .append("4. Mining success reward: ")
                .append("10")
                .setForegroundColor(getResources().getColor(R.color.color_yellow))
                .setBold()
                .setFontSize(20, true)
                .append(" coins each 5 minutes, annually about 1 millions new coins added as always.").append("\n")
                .append("\n").append("The Scarcity of London PMC")
                .setForegroundColor(getResources().getColor(R.color.color_black))
                .setFontSize(20, true).append("\n")
                .append("1. Only one London city in the world.").append("\n")
                .append("2. Limited initial coins volume.").append("\n")
                .append("3. Only 10 new coins added to the world each 5 minutes.").append("\n")
                .append("4. Only one coin for London GPS region embedded natively in the PMC app.").append("\n")
                .append("5. Phones in London will auto join the London PMC network through TAU app.")
                .create();
        binding.tvLondonPmcDesc.setText(spannableStringBuilder);
    }
}