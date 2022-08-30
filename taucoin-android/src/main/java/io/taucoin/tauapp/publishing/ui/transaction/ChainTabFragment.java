package io.taucoin.tauapp.publishing.ui.transaction;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.model.data.UserAndTx;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.databinding.FragmentTxsTabBinding;
import io.taucoin.tauapp.publishing.ui.constant.Page;

/**
 * Chain Tab页
 */
public class ChainTabFragment extends CommunityTabFragment implements ChainListAdapter.ClickListener {

    private ChainListAdapter adapter;
    private FragmentTxsTabBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_txs_tab, container, false);
        binding.setListener(this);
        return binding.getRoot();
    }

    @Override
    public RecyclerView getRecyclerView() {
        return binding.txList;
    }

    @Override
    public SwipeRefreshLayout getRefreshLayout() {
        return binding.refreshLayout;
    }

    /**
     * 初始化视图
     */
    @Override
    public void initView() {
        super.initView();
        currentTab = TAB_CHAIN;
        adapter = new ChainListAdapter(this, chainID);
        binding.txList.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        txViewModel.observerChainTxs().observe(this, txs -> {
            List<UserAndTx> currentList = new ArrayList<>(txs);
            if (currentPos == 0) {
                initScrollToBottom();
                adapter.submitList(currentList, handleUpdateAdapter);
            } else {
                currentList.addAll(adapter.getCurrentList());
                adapter.submitList(currentList, handlePullAdapter);
            }
            binding.refreshLayout.setRefreshing(false);
            binding.refreshLayout.setEnabled(txs.size() != 0 && txs.size() % Page.PAGE_SIZE == 0);

            logger.debug("txs.size::{}", txs.size());
            closeProgressDialog();
        });

        showProgressDialog();
        loadData(0);

//        disposables.add(txViewModel.observeLatestPinnedMsg(currentTab, chainID)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(list -> {
//                    boolean isHavePinnedMsg = list != null && list.size() > 0;
//                    binding.llPinnedMessage.setVisibility(isHavePinnedMsg ? View.VISIBLE : View.GONE);
//                    if (isHavePinnedMsg) {
//                        binding.tvPinnedContent.setText(TxUtils.createTxSpan(list.get(0)));
//                    }
//                }));

        disposables.add(txViewModel.observeDataSetChanged()
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
        txViewModel.loadChainTxsData(chainID, pos, getItemCount());
    }
}