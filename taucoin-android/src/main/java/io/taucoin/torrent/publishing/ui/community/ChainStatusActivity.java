package io.taucoin.torrent.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;

import org.libTAU4j.Block;

import java.util.Locale;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.ChainStatus;
import io.taucoin.torrent.publishing.core.model.data.ForkPoint;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.ActivityChainStatusBinding;
import io.taucoin.torrent.publishing.databinding.ReloadChainDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.transaction.TxUtils;

/**
 * 群组成员页面
 */
public class ChainStatusActivity extends BaseActivity {

    private ActivityChainStatusBinding binding;
    private CommunityViewModel communityViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private CompositeDisposable blockDisposables = new CompositeDisposable();
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
        loadCommunityData(null);
        loadChainStatusData(new ChainStatus());
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
    private void loadChainStatusData(ChainStatus status) {
        if (null == status) {
            return;
        }
        blockDisposables.clear();

        binding.tvExternalHeadBlock.setText(FmtMicrometer.fmtLong(status.syncingHeadBlock));

        binding.tvHeadBlock.setText(FmtMicrometer.fmtLong(status.headBlock));
        blockDisposables.add(getBlockByNumber(chainID, status.headBlock)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(block -> loadBlockDetailData(binding.headBlock, block)));

        binding.tvTailBlock.setText(FmtMicrometer.fmtLong(status.tailBlock));
        blockDisposables.add(getBlockByNumber(chainID, status.tailBlock)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(block -> loadBlockDetailData(binding.tailBlock, block)));

        binding.tvConsensusBlock.setText(FmtMicrometer.fmtLong(status.consensusBlock));
        blockDisposables.add(getBlockByNumber(chainID, status.consensusBlock)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(block -> loadBlockDetailData(binding.consensusBlock, block)));

        binding.itemDifficulty.setRightText(FmtMicrometer.fmtLong(status.difficulty));
        binding.itemTotalPeers.setRightText(FmtMicrometer.fmtLong(status.totalPeers));
        binding.itemPeersBlocks.setRightText(FmtMicrometer.fmtLong(status.peerBlocks));
        binding.itemTotalCoins.setRightText(FmtMicrometer.fmtBalance(status.totalCoin));
        binding.itemBalance.setRightText(FmtMicrometer.fmtBalance(status.balance));
    }

    /**
     * 加载社区数据
     */
    private void loadCommunityData(Community community) {
        ForkPoint point = null;
        if (community != null) {
            point = new Gson().fromJson(community.forkPoint, ForkPoint.class);
            if (point != null) {
                binding.itemForkBlockHash.setRightText(point.getHash());
                binding.itemForkBlockNum.setRightText(String.valueOf(point.getNumber()));
            }
        }
        binding.llForkPoint.setVisibility(point != null ? View.VISIBLE : View.GONE);
    }

    /**
     * 左侧抽屉布局点击事件
     */
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.item_top_peers:
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.TYPE, ChainTopActivity.TOP_PEERS);
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                ActivityUtil.startActivity(intent, this, ChainTopActivity.class);
                break;
            case R.id.item_chain_votes:
                intent = new Intent();
                intent.putExtra(IntentExtra.TYPE, ChainTopActivity.TOP_VOTES);
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                ActivityUtil.startActivity(intent, this, ChainTopActivity.class);
                break;
            case R.id.item_head_block:
                showBlockDetail(binding.headBlock, binding.ivHeadDetail);
                break;
            case R.id.item_tail_block:
                showBlockDetail(binding.tailBlock, binding.ivTailDetail);
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
            case R.id.item_gossip_list:
                intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                intent.putExtra(IntentExtra.TYPE, AccessListActivity.GOSSIP_LIST_TYPE);
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
     * @param blockNumber 区块号
     * @return Block
     */
    Observable<Block> getBlockByNumber(String chainID, long blockNumber) {
        return Observable.create(emitter -> {
            try {
                Block block = tauDaemon.getBlockByNumber(chainID, blockNumber);
                if (block != null) {
                    emitter.onNext(block);
                }
            } catch (Exception e) {
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

            disposables.add(communityViewModel.observerCommunityByChainID(chainID)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::loadCommunityData));

            disposables.add(communityViewModel.observerCommunityMiningTime(chainID)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::loadMiningTime));
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
                binding.itemMiningTime.setRightText(getString(R.string.chain_mining_time_seconds, seconds));
            }
        }
        binding.llMiningTime.setVisibility(time >= 0 ? View.VISIBLE : View.GONE);
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