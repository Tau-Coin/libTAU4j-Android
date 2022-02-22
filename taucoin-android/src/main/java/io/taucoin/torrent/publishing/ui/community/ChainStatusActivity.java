package io.taucoin.torrent.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.google.gson.Gson;

import org.libTAU4j.Block;
import org.libTAU4j.Transaction;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.ChainStatus;
import io.taucoin.torrent.publishing.core.model.data.ForkPoint;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.Formatter;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.rlp.ByteUtil;
import io.taucoin.torrent.publishing.databinding.ActivityChainStatusBinding;
import io.taucoin.torrent.publishing.databinding.ItemBlockLayoutBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;

/**
 * 群组成员页面
 */
public class ChainStatusActivity extends BaseActivity {

    private ActivityChainStatusBinding binding;
    private CommunityViewModel communityViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private String chainID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
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

        binding.tvSyncingHeadBlock.setText(FmtMicrometer.fmtLong(status.syncingHeadBlock));
        Block syncingHeadBlock = communityViewModel.getBlockByNumber(chainID, status.syncingHeadBlock);
        loadBlockDetailData(binding.syncingHeadBlock, syncingHeadBlock);

        binding.tvHeadBlock.setText(FmtMicrometer.fmtLong(status.headBlock));
        Block headBlock = communityViewModel.getBlockByNumber(chainID, status.headBlock);
        loadBlockDetailData(binding.headBlock, headBlock);

        binding.tvTailBlock.setText(FmtMicrometer.fmtLong(status.tailBlock));
        Block tailBlock = communityViewModel.getBlockByNumber(chainID, status.tailBlock);
        loadBlockDetailData(binding.tailBlock, tailBlock);

        binding.tvConsensusBlock.setText(FmtMicrometer.fmtLong(status.consensusBlock));
        Block consensusBlock = communityViewModel.getBlockByNumber(chainID, status.consensusBlock);
        loadBlockDetailData(binding.consensusBlock, consensusBlock);

        binding.itemDifficulty.setRightText(FmtMicrometer.fmtLong(status.difficulty));
        binding.itemTotalPeers.setRightText(FmtMicrometer.fmtLong(status.totalPeers));
        binding.itemPeersBlocks.setRightText(FmtMicrometer.fmtLong(status.peerBlocks));
        binding.itemTotalCoins.setRightText(FmtMicrometer.fmtBalance(status.totalCoin));
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
        }
    }

    private void showBlockDetail(ItemBlockLayoutBinding binding, ImageView ivHeadDetail) {
        View blockView = binding.getRoot();
        boolean isVisible = blockView.getVisibility() == View.VISIBLE;
        blockView.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        ivHeadDetail.setRotation(isVisible ? 90 : -90);
    }

    /**
     * 加载区块详细信息
     */
    private void loadBlockDetailData(ItemBlockLayoutBinding binding, Block block) {
        if (null == block) {
            return;
        }
        Transaction tx = block.getTx();
        byte[] payload = tx.getPayload();

        binding.tvHash.setText(block.Hash());
        boolean isHaveTx = payload != null && payload.length > 0;
        binding.tvTxs.setText(isHaveTx ? "1" : "0");
        binding.tvTimestamp.setText(DateUtil.formatTime(block.getTimestamp(), DateUtil.pattern6));
        binding.tvMiner.setText(ByteUtil.toHexString(block.getMiner()));
        String blockReward = FmtMicrometer.fmtBalance(tx.getFee());
        if (block.getBlockNumber() <= 0) {
            blockReward = FmtMicrometer.fmtBalance(block.getMinerBalance());
        }
        blockReward += " " + ChainIDUtil.getCoinName(chainID);
        binding.tvReward.setText(blockReward);
        String difficulty = FmtMicrometer.fmtDecimal(block.getCumulativeDifficulty().longValue());
        binding.tvDifficulty.setText(difficulty);
        binding.tvSize.setText(Formatter.formatFileSize(this, block.Size()));
    }

    @Override
    public void onStart() {
        super.onStart();
        if (StringUtil.isNotEmpty(chainID)) {
            disposables.add(communityViewModel.observerChainStatus(chainID)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::loadChainStatusData));

            disposables.add(communityViewModel.observerCommunityByChainID(chainID)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::loadCommunityData));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }
}