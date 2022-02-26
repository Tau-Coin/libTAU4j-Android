package io.taucoin.torrent.publishing.ui.friends;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.KeyboardUtils;
import io.taucoin.torrent.publishing.core.utils.MoneyValueFilter;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivityAirdropSetupBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;

/**
 * Airdrop详情页
 */
public class AirdropSetupActivity extends BaseActivity {

    private ActivityAirdropSetupBinding binding;
    private CommunityViewModel communityViewModel;
    private ReferralBonusAdapter adapter;
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

        binding.etCoins.setFilters(new InputFilter[]{new MoneyValueFilter()});
        int[] referralBonuses = getResources().getIntArray(R.array.referral_bonus);
        adapter = new ReferralBonusAdapter(referralBonuses);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        binding.rvReferralBonus.setLayoutManager(layoutManager);
        binding.rvReferralBonus.setAdapter(adapter);

        binding.etMembers.addTextChangedListener(textWatcher);
        binding.etCoins.addTextChangedListener(textWatcher);

        updateAirdropCoinsTotal(0F);

        binding.tvSubmit.setOnClickListener(v -> {
            int members = ViewUtils.getIntText(binding.etMembers);
            float coins = ViewUtils.getFloatText(binding.etCoins);
//            int referralBonus  = adapter.getSelected();
            if (members <= 0) {
                ToastUtils.showShortToast(R.string.bot_airdrop_members_error);
                return;
            }
            if (coins <= 0) {
                ToastUtils.showShortToast(R.string.bot_airdrop_coins_error);
                return;
            }
            communityViewModel.setupAirdropBot(chainID, members, coins);
        });

        communityViewModel.getAirdropResult().observe(this, result -> {
            onBackPressed();
        });
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            int members = ViewUtils.getIntText(binding.etMembers);
            float coins = ViewUtils.getFloatText(binding.etCoins);
            updateAirdropCoinsTotal(members * coins);
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    private void updateAirdropCoinsTotal(float total) {
        String totalCoins = FmtMicrometer.fmtDecimal(total);
        binding.tvTotal.setText(getString(R.string.bot_airdrop_coins_total_value, totalCoins));
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
