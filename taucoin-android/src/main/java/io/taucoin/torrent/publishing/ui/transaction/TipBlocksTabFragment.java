package io.taucoin.torrent.publishing.ui.transaction;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.libTAU4j.Block;
import org.libTAU4j.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.Formatter;
import io.taucoin.torrent.publishing.core.utils.rlp.ByteUtil;
import io.taucoin.torrent.publishing.databinding.FragmentTipBlocksTabBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.CommunityTabFragment;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * Tip区块页面
 */
public class TipBlocksTabFragment extends CommunityTabFragment {

    private BaseActivity activity;
    private FragmentTipBlocksTabBinding binding;
    private CommunityViewModel communityViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();

    private String chainID;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_tip_blocks_tab, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (BaseActivity) getActivity();
        assert activity != null;
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        initParameter();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if(getArguments() != null){
            chainID = getArguments().getString(IntentExtra.CHAIN_ID);
        }
    }

    /**
     * 加载Tip区块信息
     */
    private void loadData() {
        List<Block> tipBlocks = communityViewModel.getTopTipBlock(chainID, 1);
        if (tipBlocks != null && tipBlocks.size() == 1) {
            Block block = tipBlocks.get(0);
            binding.tvTipBlock.setText(String.valueOf(block.getBlockNumber()));
            Transaction tx = block.getTx();
            byte[] payload = tx.getPayload();

            boolean isHaveTx = payload != null && payload.length > 0;
            binding.tvTxs.setText(isHaveTx ? "1" : "0");
            binding.tvTimestamp.setText(DateUtil.formatTime(block.getTimestamp(), DateUtil.pattern6));
            binding.tvMiner.setText(ByteUtil.toHexString(block.getMiner()));
            String blockReward = FmtMicrometer.fmtFeeValue(tx.getFee());
            blockReward += " " + ChainIDUtil.getCoinName(chainID);
            binding.tvReward.setText(blockReward);
            String difficulty = FmtMicrometer.fmtDecimal(block.getCumulativeDifficulty().longValue());
            binding.tvDifficulty.setText(difficulty);
            binding.tvSize.setText(Formatter.formatFileSize(activity, block.Size()));
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof BaseActivity)
            activity = (BaseActivity)context;
    }

    @Override
    public void onStart() {
        super.onStart();
        disposables.add(communityViewModel.observerCommunityByChainID(chainID)
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(community -> {
                loadData();
            }));
    }

    @Override
    public void onStop() {
        super.onStop();
        disposables.clear();
    }
}
