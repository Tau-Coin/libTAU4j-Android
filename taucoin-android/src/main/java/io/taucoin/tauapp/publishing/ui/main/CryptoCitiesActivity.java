package io.taucoin.tauapp.publishing.ui.main;

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import androidx.databinding.DataBindingUtil;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.utils.SpanUtils;
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
                .append("Crypto Cities are public place for chats and news.  These groups have city logos. The coins arrangement is as following:")
                .append("\n\n")
                .append("- Initial Volume: ").append("\n")
                .append("1,000,000")
                .setForegroundColor(getResources().getColor(R.color.color_yellow))
                .setBold()
                .setFontSize(20, true)
                .append(" coins").append("\n")
                .append("- Airdrop: ")
                .append("60%")
                .setForegroundColor(getResources().getColor(R.color.color_yellow))
                .setBold()
                .setFontSize(20, true)
                .append(" for early adopters.").append("\n")
                .append("- TAU company reserves: ")
                .append("40%").append("\n")
                .append("- Mining success reward: ")
                .append("10")
                .setForegroundColor(getResources().getColor(R.color.color_yellow))
                .setBold()
                .setFontSize(20, true)
                .append(" coins each 5 minutes, annually about 1 millions new coins added as always.").append("\n")
                .append("\n")
                .append("The current crypto cities are San Francisco and London.")
//                .append("The Scarcity of London PMC")
//                .setForegroundColor(getResources().getColor(R.color.color_black))
//                .setFontSize(20, true).append("\n")
//                .append("- Only one London city in the world.").append("\n")
//                .append("- Limited initial coins volume.").append("\n")
//                .append("- Only 10 new coins added to the world each 5 minutes.").append("\n")
//                .append("- Only \"London PMC\" for London GPS region embedded natively in the PMC app.").append("\n")
//                .append("- Phones in London will auto join the London PMC network through TAU app.")
                .create();
        binding.tvLondonPmcDesc.setText(spannableStringBuilder);
    }
}