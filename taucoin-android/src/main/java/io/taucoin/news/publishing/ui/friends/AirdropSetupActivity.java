package io.taucoin.news.publishing.ui.friends;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;


import java.math.BigInteger;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.utils.ActivityUtil;
import io.taucoin.news.publishing.core.utils.ChainIDUtil;
import io.taucoin.news.publishing.core.utils.FmtMicrometer;
import io.taucoin.news.publishing.core.utils.KeyboardUtils;
import io.taucoin.news.publishing.core.utils.ToastUtils;
import io.taucoin.news.publishing.core.utils.ViewUtils;
import io.taucoin.news.publishing.databinding.ActivityAirdropSetupBinding;
import io.taucoin.news.publishing.ui.BaseActivity;
import io.taucoin.news.publishing.ui.community.CommunityViewModel;
import io.taucoin.news.publishing.ui.constant.IntentExtra;

/**
 * Airdrop详情页
 */
public class AirdropSetupActivity extends BaseActivity {

    private ActivityAirdropSetupBinding binding;
    private CommunityViewModel communityViewModel;
    private String chainID;
    private long balance;

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
        balance = getIntent().getLongExtra(IntentExtra.BALANCE, 0);

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
            BigInteger airdropTotalCoins = ViewUtils.getBigIntegerTag(binding.tvTotal);
            BigInteger balanceStr = BigInteger.valueOf(balance);
            if (airdropTotalCoins.compareTo(balanceStr) > 0) {
                ToastUtils.showShortToast(getString(R.string.bot_airdrop_total_coins_error,
                        FmtMicrometer.fmtBigInteger(balanceStr)));
                return;
            }
            communityViewModel.setupAirdropBot(chainID, members, coins, airdropTotalCoins);
        });

        communityViewModel.getAirdropResult().observe(this, result -> {
            if (result.isSuccess()) {
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                ActivityUtil.startActivity(intent, this, AirdropDetailActivity.class);
                onBackPressed();
            } else {
                ToastUtils.showShortToast(result.getMsg());
            }
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
        int coins = (int) ViewUtils.getFloatText(binding.etCoins);
        int referralBonus = (int) (coins / 2);
        if (coins > 0) {
            referralBonus = Math.max(referralBonus, 1);
        }
        BigInteger totalReferralBonus = BigInteger.valueOf(referralBonus).multiply(BigInteger.TEN).multiply(BigInteger.valueOf(members));
        binding.tvReferralBonus.setText(getString(R.string.bot_airdrop_referral_bonus_total,
                FmtMicrometer.fmtLong(referralBonus), FmtMicrometer.fmtBigInteger(totalReferralBonus)));
        BigInteger total = BigInteger.valueOf(members).multiply(BigInteger.valueOf(coins)).add(totalReferralBonus) ;
        String totalCoins = FmtMicrometer.fmtBigInteger(total);
        binding.tvTotal.setText(getString(R.string.bot_airdrop_coins_total_value, totalCoins));
        binding.tvTotal.setTag(total);

        BigInteger balanceStr = BigInteger.valueOf(balance);
        boolean isShowError = total.compareTo(balanceStr) > 0;
        binding.tvTotalError.setVisibility(isShowError ? View.VISIBLE : View.INVISIBLE);
        if (isShowError) {
            binding.tvTotalError.setText(getString(R.string.bot_airdrop_total_coins_error,
                    FmtMicrometer.fmtBigInteger(balanceStr)));
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
