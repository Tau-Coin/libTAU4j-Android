package io.taucoin.torrent.publishing.ui.community;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.ChainStatus;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndMember;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.databinding.ActivityCommunitiesBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.main.MainActivity;
import io.taucoin.torrent.publishing.ui.transaction.TransactionCreateActivity;

/**
 * 社区信息页面
 */
public class CommunitiesActivity extends BaseActivity implements View.OnClickListener {
    private ActivityCommunitiesBinding binding;
    private CommunityViewModel viewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
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
        loadMemberData(null);
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
            disposables.add(viewModel.observerCurrentMember(chainID)
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
        binding.itemMiningIncome.setRightText(FmtMicrometer.fmtBalance(status.totalRewards));
        binding.itemLastBlock.setRightText(FmtMicrometer.fmtLong(status.headBlock));
        binding.itemDifficulty.setRightText(FmtMicrometer.fmtLong(status.difficulty));
    }

    /**
     * 加载社区当前登陆用户数据
     */
    private void loadMemberData(CommunityAndMember member) {
        long power = 0;
        long balance = 0;
        boolean onChain = false;
        if (member != null) {
            balance = member.balance;
//            long currentTime = DateUtil.getTime();
//            long expiryBlocks = member.headBlock - member.blockNumber + 1;
//            expiryBlocks = Constants.BLOCKS_NOT_PERISHABLE - expiryBlocks;
//            long expiryDate = expiryBlocks * Constants.BLOCK_IN_AVG + currentTime;
//            binding.itemExpiryDate.setRightText(DateUtil.formatTime(expiryDate, DateUtil.pattern4));
//
//            long renewalDate = (expiryBlocks - Constants.AUTO_RENEWAL_MAX_BLOCKS)
//                    * Constants.BLOCK_IN_AVG + currentTime;
//            binding.itemRenewalDate.setRightText(DateUtil.formatTime(renewalDate, DateUtil.pattern4));
        }
        binding.itemBalance.setRightText(FmtMicrometer.fmtBalance(balance));
        binding.itemMiningPower.setRightText(FmtMicrometer.fmtLong(power));
        binding.itemExpiryDate.setVisibility(onChain ? View.VISIBLE : View.GONE);
        binding.itemRenewalDate.setVisibility(onChain ? View.VISIBLE : View.GONE);
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
    }


    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }
}