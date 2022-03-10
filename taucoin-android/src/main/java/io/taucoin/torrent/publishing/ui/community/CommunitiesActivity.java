package io.taucoin.torrent.publishing.ui.community;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.data.ChainStatus;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndMember;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.ActivityCommunitiesBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;

/**
 * 社区信息页面
 */
public class CommunitiesActivity extends BaseActivity implements View.OnClickListener {
    private static final int CHOOSE_REQUEST_CODE = 0x01;
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
        initLayout();
        loadMemberData(null);
        observeJoinedList();
    }

    /**
     * 观察加入的社区列表
     */
    private void observeJoinedList() {
        viewModel.getJoinedCommunityList();
        viewModel.getJoinedList().observe(this, communities -> {
            if (communities != null && communities.size() > 0) {
                chainID = communities.get(0).chainID;
                loadMiningInfo();
            }
        });
    }

    /**
     * 初始化布局
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.drawer_communities);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadMiningInfo() {
        String communityName = ChainIDUtil.getName(chainID);
        binding.tvCommunityName.setText(communityName);

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
        boolean isReadOnly = true;
        if (member != null) {
            isReadOnly = member.isReadOnly();
            power = member.power;
            long currentTime = DateUtil.getTime();
            long expiryBlocks = member.headBlock - member.blockNumber + 1;
            expiryBlocks = Constants.BLOCKS_NOT_PERISHABLE - expiryBlocks;
            long expiryDate = expiryBlocks * Constants.BLOCK_IN_AVG + currentTime;
            binding.itemExpiryDate.setRightText(DateUtil.formatTime(expiryDate, DateUtil.pattern4));

            long renewalDate = (expiryBlocks - Constants.AUTO_RENEWAL_MAX_BLOCKS)
                    * Constants.BLOCK_IN_AVG + currentTime;
            binding.itemRenewalDate.setRightText(DateUtil.formatTime(renewalDate, DateUtil.pattern4));
        }
        binding.itemMiningPower.setRightText(FmtMicrometer.fmtLong(power));
        binding.itemExpiryDate.setVisibility(!isReadOnly ? View.VISIBLE : View.GONE);
        binding.itemRenewalDate.setVisibility(!isReadOnly ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_community_name:
            case R.id.iv_arrow:
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                ActivityUtil.startActivityForResult(intent, this, CommunityChooseActivity.class,
                        CHOOSE_REQUEST_CODE);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == CHOOSE_REQUEST_CODE) {
            if (data != null) {
                chainID = data.getStringExtra(IntentExtra.CHAIN_ID);
                loadMiningInfo();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }


    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }
}