package io.taucoin.tauapp.publishing.ui.community;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.model.data.ChainStatus;
import io.taucoin.tauapp.publishing.core.model.data.MemberAndAmount;
import io.taucoin.tauapp.publishing.core.model.data.MemberTips;
import io.taucoin.tauapp.publishing.core.utils.ActivityUtil;
import io.taucoin.tauapp.publishing.core.utils.ChainIDUtil;
import io.taucoin.tauapp.publishing.core.utils.FmtMicrometer;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.core.utils.ToastUtils;
import io.taucoin.tauapp.publishing.databinding.ActivityCommunitiesBinding;
import io.taucoin.tauapp.publishing.ui.BaseActivity;
import io.taucoin.tauapp.publishing.ui.constant.IntentExtra;
import io.taucoin.tauapp.publishing.ui.main.MainActivity;
import io.taucoin.tauapp.publishing.ui.transaction.TransactionCreateActivity;

/**
 * 社区信息页面
 */
public class CommunitiesActivity extends BaseActivity implements View.OnClickListener {
    private ActivityCommunitiesBinding binding;
    private CommunityViewModel viewModel;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private String chainID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_communities);
        binding.setListener(this);
        initParameter();
        initLayout();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if (getIntent() != null) {
            chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
        }
    }

    /**
     * 初始化布局
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(ChainIDUtil.getName(chainID));
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        viewModel.getJoinedResult().observe(this, result -> {
            if (result.isSuccess()) {
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(IntentExtra.TYPE, 0);
                ActivityUtil.startActivity(intent, this, MainActivity.class);
            } else {
                ToastUtils.showShortToast(R.string.community_join_failed);
            }
        });
    }

    private void loadMiningInfo() {
        if (StringUtil.isNotEmpty(chainID)) {
            loadChainStatusData(new ChainStatus());
            disposables.add(viewModel.observerChainStatus(chainID)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::loadChainStatusData));

            loadMemberData(null);
            disposables.add(viewModel.observerMemberAndAmount(chainID)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::loadMemberData));
        }
    }

    /**
     * 加载链状态数据
     */
    private void loadChainStatusData(ChainStatus status) {
        if (null == status) {
            return;
        }
//        binding.itemMiningIncome.setRightText(FmtMicrometer.fmtBalance(status.totalRewards));
        binding.itemLastBlock.setRightText(FmtMicrometer.fmtLong(status.headBlock));
        binding.itemDifficulty.setRightText(FmtMicrometer.fmtLong(status.difficulty));
    }

    /**
     * 加载社区当前登陆用户数据
     */
    private void loadMemberData(MemberAndAmount member) {
        long balance = 0;
        long pendingBalance = 0;
        long power = 0;
//        long miningIncome = 0;
        long mIncomePending = 0;
        if (member != null) {
            power = member.power;
            long onChainBalance = member.mIncomePending + member.txIncome - member.txExpenditure;
            pendingBalance = onChainBalance + member.txIncomePending - member.txExpenditurePending;
            // 余额根据libTAU balance减去计算上链为100%的金额
            balance = member.balance - onChainBalance;
            balance = Math.max(0, balance);

            logger.debug("loadMemberData balance::{}, showBalance::{}, onChainBalance::{}, pendingBalance::{}," +
                            " mTotalIncome::{}, mIncomePending::{}",
                    member.balance, balance, onChainBalance, pendingBalance,
                    member.mTotalIncome, member.mIncomePending);

            mIncomePending = member.mIncomePending;
//            miningIncome = member.mTotalIncome - mIncomePending;
//            miningIncome = Math.max(0, miningIncome);
        }
        binding.itemBalance.setRightText(FmtMicrometer.fmtLong(balance) + "/" +FmtMicrometer.fmtLong(pendingBalance));
//        binding.itemMiningIncome.setRightText(FmtMicrometer.fmtLong(miningIncome));
        binding.itemMiningIncome.setVisibility(View.GONE);
        binding.itemMiningIncomePending.setRightText(FmtMicrometer.fmtLong(mIncomePending));
        double showPower = Math.sqrt(1 + power);
        String powerStr = "sqrt(1+%s)=%s";
        powerStr = String.format(powerStr, FmtMicrometer.fmtLong(power), FmtMicrometer.formatThreeDecimal(showPower));
        binding.itemMiningPower.setRightText(powerStr);
        //sqrt(1+ power)//
    }

//    /**
//     *  创建右上角Menu
//     */
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_transactions, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onPrepareOptionsMenu(Menu menu) {
//        MenuItem item = menu.findItem(R.id.menu_transactions);
//        CharSequence title = item.getTitle();
//        item.setActionView(R.layout.menu_textview);
//        TextView tv = (TextView) item.getActionView();
//        tv.setText(title);
//        tv.setOnClickListener(this);
//        return super.onPrepareOptionsMenu(menu);
//    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_join:
                viewModel.joinCommunity(chainID);
                break;
            case R.id.rl_transactions:
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                ActivityUtil.startActivity(intent, this, TransactionsActivity.class);
                break;
            case R.id.rl_pay_people:
                intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                ActivityUtil.startActivity(intent, this, TransactionCreateActivity.class);
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadMiningInfo();

        disposables.add(viewModel.observeMemberTips()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleMemberTips));
    }

    private void handleMemberTips(MemberTips tips) {
        boolean isShowTips = tips.pendingTime > 0;
        binding.viewTips.setVisibility(isShowTips ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }
}