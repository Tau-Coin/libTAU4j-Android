package io.taucoin.torrent.publishing.ui.transaction;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.BlockInfo;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.ui.community.BlockListAdapter;
import io.taucoin.torrent.publishing.ui.constant.Page;

/**
 * Blocks Tab页
 */
public class BlocksTabFragment extends CommunityTabFragment implements ChainListAdapter.ClickListener {

    private BlockListAdapter adapter;

    /**
     * 初始化视图
     */
    @Override
    public void initView() {
        super.initView();
        currentTab = TAB_CHAIN;
        adapter = new BlockListAdapter();
        binding.txList.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        communityViewModel.observerChainBlocks().observe(this, txs -> {
            List<BlockInfo> currentList = new ArrayList<>(txs);
            if (currentPos == 0) {
                adapter.submitList(currentList, handleUpdateAdapter);
            } else {
                currentList.addAll(adapter.getCurrentList());
                adapter.submitList(currentList, handlePullAdapter);
            }
            binding.refreshLayout.setRefreshing(false);
            binding.refreshLayout.setEnabled(txs.size() != 0 && txs.size() % Page.PAGE_SIZE == 0);

            logger.debug("txs.size::{}", txs.size());
        });

        loadData(0);

        disposables.add(communityViewModel.observeBlocksSetChanged()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    // 跟当前用户有关系的才触发刷新
                    if (result != null && StringUtil.isNotEmpty(result.getMsg())) {
                        binding.refreshLayout.setRefreshing(false);
                        binding.refreshLayout.setEnabled(false);
                        // 立即执行刷新
                        loadData(0);
                    }
                }));
    }

    @Override
    int getItemCount() {
        if (adapter != null) {
            return adapter.getItemCount();
        }
        return super.getItemCount();
    }

    @Override
    public void loadData(int pos) {
        super.loadData(pos);
        communityViewModel.loadBlocksData(chainID, currentPos);
    }
}