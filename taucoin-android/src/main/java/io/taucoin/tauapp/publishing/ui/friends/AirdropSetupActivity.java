package io.taucoin.tauapp.publishing.ui.friends;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;


import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.Constants;
import io.taucoin.tauapp.publishing.core.utils.ActivityUtil;
import io.taucoin.tauapp.publishing.core.utils.ChainIDUtil;
import io.taucoin.tauapp.publishing.core.utils.FmtMicrometer;
import io.taucoin.tauapp.publishing.core.utils.KeyboardUtils;
import io.taucoin.tauapp.publishing.core.utils.ToastUtils;
import io.taucoin.tauapp.publishing.core.utils.ViewUtils;
import io.taucoin.tauapp.publishing.databinding.ActivityAirdropSetupBinding;
import io.taucoin.tauapp.publishing.ui.BaseActivity;
import io.taucoin.tauapp.publishing.ui.community.CommunityViewModel;
import io.taucoin.tauapp.publishing.ui.constant.IntentExtra;

/**
 * Airdrop详情页
 */
public class AirdropSetupActivity extends BaseActivity {

    private ActivityAirdropSetupBinding binding;
    private CommunityViewModel communityViewModel;
    private String chainID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_airdrop_setup);
        initLayout();
    }

    private void initLayout() {
        chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);

        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(ChainIDUtil.getName(chainID));
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

//        binding.etCoins.setFilters(new InputFilter[]{new MoneyValueFilter()});
        binding.etCoins.setInputType(InputType.TYPE_CLASS_NUMBER);

        binding.etMembers.addTextChangedListener(textWatcher);
        binding.etCoins.addTextChangedListener(textWatcher);

        updateAirdropCoinsTotal();

        binding.tvSubmit.setOnClickListener(v -> {
            int members = ViewUtils.getIntText(binding.etMembers);
            float coins = ViewUtils.getFloatText(binding.etCoins);
            if (members <= 0) {
                ToastUtils.showShortToast(R.string.bot_airdrop_members_error);
                return;
            }
            if (coins <= 0) {
                ToastUtils.showShortToast(R.string.bot_airdrop_coins_error);
                return;
            }
            float airdropTotalCoins = ViewUtils.getFloatTag(binding.tvTotal);
            long maxTotalCoins = Constants.TOTAL_COIN.divide(Constants.COIN).longValue();
            if (airdropTotalCoins > maxTotalCoins) {
                ToastUtils.showShortToast(getString(R.string.bot_airdrop_total_coins_error,
                        FmtMicrometer.fmtLong(maxTotalCoins)));
                return;
            }
            communityViewModel.setupAirdropBot(chainID, members, coins);
        });

        communityViewModel.getAirdropResult().observe(this, result -> {
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.CHAIN_ID, chainID);
            ActivityUtil.startActivity(intent, this, AirdropDetailActivity.class);
            onBackPressed();
        });
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            updateAirdropCoinsTotal();
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    private void updateAirdropCoinsTotal() {
        int members = ViewUtils.getIntText(binding.etMembers);
        float coins = ViewUtils.getFloatText(binding.etCoins);
        int referralBonus = (int) (coins / 2);
        if (coins > 0) {
            referralBonus = Math.max(referralBonus, 1);
        }
        binding.tvReferralBnous.setText(getString(R.string.bot_airdrop_referral_bonus_total,
                referralBonus));
        float total = members * (coins + 10 * referralBonus);
        String totalCoins = FmtMicrometer.fmtDecimal(total);
        binding.tvTotal.setText(getString(R.string.bot_airdrop_coins_total_value, totalCoins));
        binding.tvTotal.setTag(total);

        long maxTotalCoins = Constants.TOTAL_COIN.divide(Constants.COIN).longValue();
        boolean isShowError = total > maxTotalCoins;
        binding.tvTotalError.setVisibility(isShowError ? View.VISIBLE : View.INVISIBLE);
        if (isShowError) {
            binding.tvTotalError.setText(getString(R.string.bot_airdrop_total_coins_error,
                    FmtMicrometer.fmtLong(maxTotalCoins)));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        KeyboardUtils.hideSoftInput(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.etMembers.removeTextChangedListener(textWatcher);
        binding.etCoins.removeTextChangedListener(textWatcher);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
