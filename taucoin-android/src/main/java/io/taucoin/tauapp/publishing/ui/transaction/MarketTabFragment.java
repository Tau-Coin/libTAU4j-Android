package io.taucoin.tauapp.publishing.ui.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.leinardi.android.speeddial.SpeedDialActionItem;

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
import io.taucoin.tauapp.publishing.core.model.data.CommunityAndMember;
import io.taucoin.tauapp.publishing.core.model.data.UserAndTx;
import io.taucoin.tauapp.publishing.core.utils.ActivityUtil;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.databinding.FragmentTxsMarketTabBinding;
import io.taucoin.tauapp.publishing.ui.TauNotifier;
import io.taucoin.tauapp.publishing.ui.constant.IntentExtra;
import io.taucoin.tauapp.publishing.ui.constant.Page;

/**
 * Market Tab页
 */
public class MarketTabFragment extends CommunityTabFragment implements MarketListAdapter.ClickListener {

    private MarketListAdapter adapter;
    private int filterItem;
    private FragmentTxsMarketTabBinding binding;
    private boolean isVisibleToUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_txs_market_tab, container, false);
        binding.setListener(this);
        return binding.getRoot();
    }

    /**
     * 初始化视图
     */
    @Override
    public void initView() {
        super.initView();
        filterItem = R.string.community_view_all;
        currentTab = TAB_MARKET;
        adapter = new MarketListAdapter(this, chainID);
        binding.txList.setAdapter(adapter);

        initSpinner();
        initFabSpeedDial();
    }

    @Override
    public RecyclerView getRecyclerView() {
        return binding.txList;
    }

    @Override
    public SwipeRefreshLayout getRefreshLayout() {
        return binding.refreshLayout;
    }

    private void initSpinner() {
        int verticalOffset = -getResources().getDimensionPixelSize(R.dimen.widget_size_5);
        binding.viewSpinner.setDropDownVerticalOffset(verticalOffset);

        int[] spinnerItems = new int[] {R.string.community_view_all,
                R.string.community_view_sell,
                R.string.community_view_airdrop,
                R.string.community_view_announcement};
        SpinnerAdapter adapter = new SpinnerAdapter(activity, spinnerItems);
        binding.viewSpinner.setSelection(0);
        binding.viewSpinner.setAdapter(adapter);

        binding.viewSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switchView(spinnerItems[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    /**
     * 初始化右下角悬浮按钮组件
     */
    private void initFabSpeedDial() {
        FloatingActionButton mainFab = binding.fabButton.getMainFab();
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mainFab.getLayoutParams();
        layoutParams.gravity = Gravity.END | Gravity.BOTTOM;
        mainFab.setLayoutParams(layoutParams);
        mainFab.setCustomSize(getResources().getDimensionPixelSize(R.dimen.widget_size_44));

        SpeedDialActionItem invitationItem = new SpeedDialActionItem.Builder(R.id.community_create_invitation,
                R.drawable.ic_add_36dp)
                .setFabSize(getResources().getDimensionPixelSize(R.dimen.widget_size_20))
                .setLabel(getString(R.string.community_leader_invitation))
                .setLabelColor(getResources().getColor(R.color.color_yellow))
                .create();
        binding.fabButton.addActionItem(invitationItem);

        SpeedDialActionItem airdropItem = new SpeedDialActionItem.Builder(R.id.community_create_airdrop,
                R.drawable.ic_add_36dp)
                .setFabSize(getResources().getDimensionPixelSize(R.dimen.widget_size_14))
                .setLabel(getString(R.string.community_airdrop))
                .setLabelColor(getResources().getColor(R.color.color_yellow))
                .create();
        binding.fabButton.addActionItem(airdropItem);

        SpeedDialActionItem sellItem = new SpeedDialActionItem.Builder(R.id.community_create_sell,
                R.drawable.ic_add_36dp)
                .setFabSize(getResources().getDimensionPixelSize(R.dimen.widget_size_30))
                .setLabel(getString(R.string.community_sell_coins))
                .setLabelColor(getResources().getColor(R.color.color_yellow))
                .create();
        binding.fabButton.addActionItem(sellItem);

        binding.fabButton.getMainFab().setOnClickListener(v -> {
            if (binding.fabButton.isOpen()) {
                binding.fabButton.close();
            } else {
                binding.fabButton.open();
            }
        });

        Intent intent = new Intent();
        intent.putExtra(IntentExtra.CHAIN_ID, chainID);
        binding.fabButton.setOnActionSelectedListener(actionItem -> {
            if (!isJoined) {
                return false;
            }
            switch (actionItem.getId()) {
                case R.id.community_create_sell:
                    ActivityUtil.startActivityForResult(intent, activity, SellCreateActivity.class,
                            TX_REQUEST_CODE);
                    break;
                case R.id.community_create_airdrop:
                    ActivityUtil.startActivityForResult(intent, activity, AirdropCreateActivity.class,
                            TX_REQUEST_CODE);
                    break;
                case R.id.community_create_invitation:
                    ActivityUtil.startActivityForResult(intent, activity, AnnouncementCreateActivity.class,
                            TX_REQUEST_CODE);
                    break;
            }
            return false;
        });
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        this.isVisibleToUser = isVisibleToUser;
        logger.debug("setUserVisibleHint1::{}", isVisibleToUser);
        if (communityViewModel != null && isVisibleToUser) {
            communityViewModel.clearNewsUnread(chainID);
        }
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
            if (isVisibleToUser) {
                communityViewModel.clearNewsUnread(chainID);
            }
            logger.debug("txs.size::{}", txs.size());
            closeProgressDialog();
            TauNotifier.getInstance().cancelNotify(chainID);
        });
        loadData(0);

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

        disposables.add(txViewModel.observeLatestPinnedMsg(currentTab, chainID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    boolean isHavePinnedMsg = list != null && list.size() > 0;
                    binding.llPinnedMessage.setVisibility(isHavePinnedMsg ? View.VISIBLE : View.GONE);
                    if (isHavePinnedMsg) {
                        binding.tvPinnedContent.setText(TxUtils.createTxSpan(list.get(0)));
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
    public void switchView(int filterItem) {
        super.switchView(filterItem);
        this.filterItem = filterItem;
        loadData(0);
    }

    @Override
    public void handleMember(CommunityAndMember member) {
        super.handleMember(member);
        int color = !isJoined ? R.color.gray_light : R.color.primary;
        binding.fabButton.setMainFabClosedBackgroundColor(getResources().getColor(color));
    }

    @Override
    public void loadData(int pos) {
        super.loadData(pos);
        txViewModel.loadMarketData(filterItem, chainID, pos, getItemCount());
    }
}
