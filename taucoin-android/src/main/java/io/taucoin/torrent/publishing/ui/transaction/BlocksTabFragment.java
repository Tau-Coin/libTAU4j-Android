package io.taucoin.torrent.publishing.ui.transaction;

import com.noober.menu.FloatMenu;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.BlockAndTx;
import io.taucoin.torrent.publishing.core.model.data.OperationMenuItem;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.ui.community.BlockListAdapter;
import io.taucoin.torrent.publishing.ui.constant.Page;

/**
 * Blocks Tab页
 */
public class BlocksTabFragment extends CommunityTabFragment implements ChainListAdapter.ClickListener,
    BlockListAdapter.ClickListener {

    private BlockListAdapter adapter;
    private FloatMenu operationsMenu;

    /**
     * 初始化视图
     */
    @Override
    public void initView() {
        super.initView();
        currentTab = TAB_CHAIN;
        adapter = new BlockListAdapter(this);
        binding.txList.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        communityViewModel.observerChainBlocks().observe(this, blocks -> {
            List<BlockAndTx> currentList = new ArrayList<>(blocks);
            if (currentPos == 0) {
                initScrollToBottom();
                adapter.submitList(currentList, handleUpdateAdapter);
            } else {
                currentList.addAll(adapter.getCurrentList());
                adapter.submitList(currentList, handlePullAdapter);
            }
            binding.refreshLayout.setRefreshing(false);
            binding.refreshLayout.setEnabled(blocks.size() != 0 && blocks.size() % Page.PAGE_SIZE == 0);

            closeProgressDialog();
        });
        showProgressDialog(false);
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
    public void onDestroy() {
        super.onDestroy();
        if (operationsMenu != null) {
            operationsMenu.setOnItemClickListener(null);
            operationsMenu.dismiss();
        }
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
        communityViewModel.loadBlocksData(chainID, currentPos, getItemCount());
    }

    @Override
    public void onLongClick(BlockAndTx block) {
        List<OperationMenuItem> menuList = new ArrayList<>();
        if (block.tx != null) {
            menuList.add(new OperationMenuItem(R.string.tx_operation_copy_transaction_hash));
        }
        menuList.add(new OperationMenuItem(R.string.tx_operation_copy_miner));
        menuList.add(new OperationMenuItem(R.string.tx_operation_copy_hash));
        if (StringUtil.isNotEmpty(block.previousBlockHash)) {
            menuList.add(new OperationMenuItem(R.string.tx_operation_copy_previous_hash));
        }
        operationsMenu = new FloatMenu(activity);
        operationsMenu.items(menuList);
        operationsMenu.setOnItemClickListener((v, position) -> {
            OperationMenuItem item = menuList.get(position);
            int resId = item.getResId();
            switch (resId) {
                case R.string.tx_operation_copy_miner:
                    CopyManager.copyText(block.miner);
                    ToastUtils.showShortToast(R.string.copy_successfully);
                    break;
                case R.string.tx_operation_copy_hash:
                    CopyManager.copyText(block.blockHash);
                    ToastUtils.showShortToast(R.string.copy_successfully);
                    break;
                case R.string.tx_operation_copy_previous_hash:
                    CopyManager.copyText(block.previousBlockHash);
                    ToastUtils.showShortToast(R.string.copy_successfully);
                    break;
                case R.string.tx_operation_copy_transaction_hash:
                    CopyManager.copyText(block.tx.txID);
                    ToastUtils.showShortToast(R.string.copy_successfully);
                    break;
            }
        });
        operationsMenu.show(activity.getPoint());
    }
}