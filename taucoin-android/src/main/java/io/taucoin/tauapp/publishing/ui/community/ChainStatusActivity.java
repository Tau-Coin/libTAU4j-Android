package io.taucoin.tauapp.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;

import org.libTAU4j.Block;
import java.util.HashMap;
import java.util.Map;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.model.TauDaemon;
import io.taucoin.tauapp.publishing.core.model.data.BlockStatistics;
import io.taucoin.tauapp.publishing.core.model.data.ChainStatus;
import io.taucoin.tauapp.publishing.core.model.data.ForkPoint;
import io.taucoin.tauapp.publishing.core.utils.ActivityUtil;
import io.taucoin.tauapp.publishing.core.utils.FmtMicrometer;
import io.taucoin.tauapp.publishing.core.utils.HashUtil;
import io.taucoin.tauapp.publishing.core.utils.Logarithm;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.databinding.ActivityChainStatusBinding;
import io.taucoin.tauapp.publishing.databinding.ReloadChainDialogBinding;
import io.taucoin.tauapp.publishing.ui.BaseActivity;
import io.taucoin.tauapp.publishing.ui.constant.IntentExtra;
import io.taucoin.tauapp.publishing.ui.customviews.CommonDialog;
import io.taucoin.tauapp.publishing.ui.transaction.TxUtils;

/**
 * 群组成员页面
 */
public class ChainStatusActivity extends BaseActivity {
    private ActivityChainStatusBinding binding;
    private CommunityViewModel communityViewModel;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final CompositeDisposable blockDisposables = new CompositeDisposable();
    private String chainID;
    private TauDaemon tauDaemon;
    private Disposable reloadChainDisposable;
    private CommonDialog reloadChainDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        tauDaemon = TauDaemon.getInstance(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chain_status);
        initParameter();
        initLayout();
        loadChainStatusData(new ChainStatus());
        loadBlockStatistics(new BlockStatistics());
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
    private void initLayout() {
        binding.toolbarInclude.toolbar.setTitle(R.string.community_chain_status);
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    /**
     * 加载链状态数据
     */
    private ChainStatus mStatus;
    private String headBlockHash;
    private String consensusBlockHash;
    private void loadChainStatusData(ChainStatus status) {
        if (null == status) {
            return;
        }
        blockDisposables.clear();

        if (null == mStatus || mStatus.syncingHeadBlock != status.syncingHeadBlock) {
            binding.tvExternalHeadBlock.setText(FmtMicrometer.fmtLong(status.syncingHeadBlock));
        }

        if (null == mStatus || mStatus.headBlock != status.headBlock) {
            binding.tvHeadBlock.setText(FmtMicrometer.fmtLong(status.headBlock));
        }
        if (null == mStatus || mStatus.consensusBlock != status.consensusBlock) {
            binding.tvConsensusBlock.setText(FmtMicrometer.fmtLong(status.consensusBlock));
        }
        blockDisposables.add(getBlockByNumber(chainID, status.headBlock, status.consensusBlock)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(blocksMap -> {
                    Block block = blocksMap.get(status.headBlock);
                    if (block != null && StringUtil.isNotEquals(headBlockHash, block.Hash())) {
                        headBlockHash = block.Hash();
                        loadBlockDetailData(binding.headBlock, block);
                    }
                    block = blocksMap.get(status.consensusBlock);
                    if (block != null && StringUtil.isNotEquals(consensusBlockHash, block.Hash())) {
                        consensusBlockHash = block.Hash();
                        loadBlockDetailData(binding.consensusBlock, block);
                    }
                }));

        if (null == mStatus || mStatus.difficulty != status.difficulty) {
            binding.itemDifficulty.setRightText(FmtMicrometer.fmtLong(status.difficulty));
        }
//        if (null == mStatus || mStatus.totalPeers != status.totalPeers) {
//            binding.itemTotalPeers.setRightText(FmtMicrometer.fmtLong(status.totalPeers));
//        }

        if (null == mStatus || mStatus.peerBlocks != status.peerBlocks) {
            binding.itemPeersBlocks.setRightText(FmtMicrometer.fmtLong(status.peerBlocks));
        }

        if (null == mStatus || mStatus.totalCoin != status.totalCoin) {
            binding.itemTotalCoins.setRightText(FmtMicrometer.fmtBalance(status.totalCoin));
        }

        if (null == mStatus || mStatus.balUpdateTime != status.balUpdateTime) {
            String balance = FmtMicrometer.fmtBalance(status.balance);
//            String time = DateUtil.formatTime(status.balUpdateTime, DateUtil.pattern14);
//            String balanceAndTime = getResources().getString(R.string.drawer_balance_time_no_title,
//                    balance, time);
            binding.itemBalance.setRightText(balance);
        }

        if (null == mStatus || mStatus.power != status.power) {
            double showPower = Math.sqrt(1 + status.power);
            binding.itemPower.setRightText(FmtMicrometer.formatThreeDecimal(showPower));
        }
        if (null == mStatus || StringUtil.isNotEquals(mStatus.forkPoint, status.forkPoint)) {
            if (StringUtil.isNotEmpty(status.forkPoint)) {
                ForkPoint point = new Gson().fromJson(status.forkPoint, ForkPoint.class);
                if (point != null) {
                    binding.itemForkBlockHash.setRightText(HashUtil.hashMiddleHide(point.getHash()));
                    binding.itemForkBlockNum.setRightText(String.valueOf(point.getNumber()));
                    binding.llForkPoint.setVisibility(View.VISIBLE);
                } else {
                    binding.llForkPoint.setVisibility(View.GONE);
                }
            } else {
                binding.llForkPoint.setVisibility(View.GONE);
            }
        }
        mStatus = status;
    }

    private BlockStatistics mStatistics;
    private void loadBlockStatistics(BlockStatistics statistics) {
        if (null == mStatistics || mStatistics.getOnChain() != statistics.getOnChain()) {
            binding.itemOnChainBlocks.setRightText(FmtMicrometer.fmtLong(statistics.getOnChain()));
        }
        if (null == mStatistics || mStatistics.getTotal() != statistics.getTotal()) {
            binding.itemTotalBlocks.setRightText(FmtMicrometer.fmtLong(statistics.getTotal()));
        }
        this.mStatistics = statistics;
    }

    /**
     * 左侧抽屉布局点击事件
     */
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.item_all_peers:
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                ActivityUtil.startActivity(intent, this, ChainTopActivity.class);
                break;
            case R.id.item_head_block:
                showBlockDetail(binding.headBlock, binding.ivHeadDetail);
                break;
            case R.id.item_consensus_block:
                showBlockDetail(binding.consensusBlock, binding.ivConsensusDetail);
                break;
//            case R.id.item_syncing_head_block:
//                showBlockDetail(binding.syncingHeadBlock, binding.ivSyncingHeadBlock);
//                break;
            case R.id.item_sync_status:
                intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                ActivityUtil.startActivity(intent, this, SyncStatusActivity.class);
                break;
            case R.id.item_reload_chain:
                reloadChain();
                break;
            case R.id.item_access_list:
                intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                intent.putExtra(IntentExtra.TYPE, AccessListActivity.ACCESS_LIST_TYPE);
                ActivityUtil.startActivity(intent, this, AccessListActivity.class);
                break;
        }
    }

    private void reloadChain() {
        if (reloadChainDialog != null && reloadChainDialog.isShowing()) {
            return;
        }
        ReloadChainDialogBinding reloadBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.reload_chain_dialog, null, false);
        reloadChainDialog = new CommonDialog.Builder(this)
                .setContentView(reloadBinding.getRoot())
                .setCanceledOnTouchOutside(false)
                .create();
        reloadChainDialog.show();

        reloadBinding.tvCancel.setOnClickListener(v -> {
            reloadChainDialog.closeDialog();
            if (reloadChainDisposable != null && !reloadChainDisposable.isDisposed()) {
                reloadChainDisposable.dispose();
            }
        });

        if (reloadChainDisposable != null && !reloadChainDisposable.isDisposed()) {
            return;
        }
        reloadChainDisposable = communityViewModel.reloadChain(chainID).
                subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(progress -> {
                    int loadingProgress = progress.intValue();
                    if (loadingProgress == 100) {
                        reloadBinding.tvStatus.setText(R.string.common_done);
                        reloadBinding.tvCancel.setText(R.string.ok);
                        reloadBinding.tvCancel.setTextColor(getResources().getColor(R.color.color_yellow));
                    }
                    reloadBinding.cvProgress.setProgress(loadingProgress);
                });
    }

    /**
     * 获取区块
     * @param chainID 社区ID
     * @param blockNumbers 区块号
     * @return Block
     */
    Observable<Map<Long, Block>> getBlockByNumber(String chainID, long... blockNumbers) {
        return Observable.create(emitter -> {
            try {
                if (blockNumbers != null && blockNumbers.length > 0) {
                    Map<Long, Block> map = new HashMap<>();
                    for (long blockNum : blockNumbers) {
                        Block block = tauDaemon.getBlockByNumber(chainID, blockNum);
                        map.put(blockNum, block);
                    }
                    emitter.onNext(map);
                }
            } catch (Exception ignore) {
            }
            emitter.onComplete();
        });
    }

    private void showBlockDetail(TextView blockView, ImageView ivHeadDetail) {
        boolean isVisible = blockView.getVisibility() == View.VISIBLE;
        blockView.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        ivHeadDetail.setRotation(isVisible ? 90 : -90);
    }

    /**
     * 加载区块详细信息
     */
    private void loadBlockDetailData(TextView textView, Block block) {
        if (null == block) {
            return;
        }
        textView.setText(TxUtils.createBlockSpan(block));
    }

    @Override
    public void onStart() {
        super.onStart();
        if (StringUtil.isNotEmpty(chainID)) {
            disposables.add(communityViewModel.observerChainStatus(chainID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::loadChainStatusData));

            disposables.add(communityViewModel.observerCommunityMiningTime(chainID)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::loadMiningTime, it->{}));

            disposables.add(communityViewModel.getBlocksStatistics(chainID)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::loadBlockStatistics)
            );
        }
    }

    private void loadMiningTime(long time) {
        if (time >= 0) {
            long minutes = time / 60;
            long seconds = time % 60;
            if (minutes > 0) {
                binding.itemMiningTime.setRightText(getString(R.string.chain_mining_time_min_seconds,
                        minutes, seconds));
            } else {
                String secondsStr = getString(R.string.chain_mining_time_seconds, seconds);
                if (seconds == 0) {
                    secondsStr += getString(R.string.chain_mining_syncing);
                }
                binding.itemMiningTime.setRightText(secondsStr);
            }
        } else if (time == -100) {
            binding.itemMiningTime.setRightText(getString(R.string.chain_mining_time_doze));
        }
        binding.llMiningTime.setVisibility(time >= 0 || time == -100 ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
        blockDisposables.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reloadChainDisposable != null && !reloadChainDisposable.isDisposed()) {
            reloadChainDisposable.dispose();
        }
        if (reloadChainDialog != null && reloadChainDialog.isShowing()) {
            reloadChainDialog.closeDialog();
        }
    }
}