package io.taucoin.torrent.publishing.ui.transaction;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.core.model.data.message.TxType;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.SpannableUrl;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.databinding.DialogTrustBinding;
import io.taucoin.torrent.publishing.ui.CommunityTabFragment;
import io.taucoin.torrent.publishing.ui.community.CommunityTabs;
import io.taucoin.torrent.publishing.ui.constant.Page;
import io.taucoin.torrent.publishing.ui.setting.FavoriteViewModel;
import io.taucoin.torrent.publishing.ui.user.UserDetailActivity;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.FragmentTxsTabBinding;
import io.taucoin.torrent.publishing.databinding.ItemOperationsBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 交易Tab页
 */
public class TxsTabFragment extends CommunityTabFragment implements TxListAdapter.ClickListener,
        View.OnClickListener {

    private static final Logger logger = LoggerFactory.getLogger("TxsTabFragment");
    public static final int TX_REQUEST_CODE = 0x1002;
    private BaseActivity activity;
    private FragmentTxsTabBinding binding;
    private TxViewModel txViewModel;
    private UserViewModel userViewModel;
    private FavoriteViewModel favoriteViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private TxListAdapter adapter;
    private CommonDialog operationsDialog;
    private CommonDialog trustDialog;

    private String chainID;
    private int currentTab;
    private boolean isReadOnly;
    private int currentPos = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_txs_tab, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activity = (BaseActivity) getActivity();
        assert activity != null;
        ViewModelProvider provider = new ViewModelProvider(this);
        txViewModel = provider.get(TxViewModel.class);
        userViewModel = provider.get(UserViewModel.class);
        favoriteViewModel = provider.get(FavoriteViewModel.class);
        initParameter();
        initView();
        initFabSpeedDial();
        handleReadOnly(isReadOnly);
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if(getArguments() != null){
            chainID = getArguments().getString(IntentExtra.CHAIN_ID);
            currentTab = getArguments().getInt(IntentExtra.TYPE, -1);
            if(currentTab == -1){
                binding.fabButton.setVisibility(View.GONE);
            }
            isReadOnly = getArguments().getBoolean(IntentExtra.READ_ONLY, false);
        }
    }

    /**
     * 初始化视图
     */
    private void initView() {
        binding.refreshLayout.setOnRefreshListener(this);

        adapter = new TxListAdapter(this, chainID);

        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
//        layoutManager.setStackFromEnd(true);
        binding.txList.setLayoutManager(layoutManager);
        binding.txList.setItemAnimator(null);
        binding.txList.setAdapter(adapter);

        loadData(0);
    }

    private final Runnable handleUpdateAdapter = () -> {
        LinearLayoutManager layoutManager = (LinearLayoutManager) binding.txList.getLayoutManager();
        if (layoutManager != null) {
            int bottomPosition = adapter.getItemCount() - 1;
            // 滚动到底部
            logger.debug("handleUpdateAdapter scrollToPosition::{}", bottomPosition);
            layoutManager.scrollToPositionWithOffset(bottomPosition, Integer.MIN_VALUE);
        }
    };

    private final Runnable handlePullAdapter = () -> {
        LinearLayoutManager layoutManager = (LinearLayoutManager) binding.txList.getLayoutManager();
        if (layoutManager != null) {
            int bottomPosition = adapter.getItemCount() - 1;
            int position = bottomPosition - currentPos;
            layoutManager.scrollToPositionWithOffset(position, 0);
        }
    };

    /**
     * 初始化右下角悬浮按钮组件
     */
    private void initFabSpeedDial() {
        // 自定义点击事件
        binding.fabButton.getMainFab().setOnClickListener(v ->{
            if (isReadOnly) {
                return;
            }
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.CHAIN_ID, chainID);
            if (currentTab == CommunityTabs.NOTE.getIndex()) {
                ActivityUtil.startActivityForResult(intent, activity, NoteCreateActivity.class,
                        TX_REQUEST_CODE);
            } else if (currentTab == CommunityTabs.MARKET.getIndex()) {
                ActivityUtil.startActivityForResult(intent, activity, SellCreateActivity.class,
                        TX_REQUEST_CODE);
            } else {
                ActivityUtil.startActivityForResult(intent, activity, TransactionCreateActivity.class,
                        TX_REQUEST_CODE);
            }
        });
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
        txViewModel.observerChainTxs().observe(this, txs -> {
            List<UserAndTx> currentList = new ArrayList<>(txs);
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

        userViewModel.getEditBlacklistResult().observe(this, result -> {
            if (result.isSuccess()) {
                ToastUtils.showShortToast(R.string.ban_successfully);
            } else {
                ToastUtils.showShortToast(R.string.ban_failed);
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    public void onItemLongClicked(UserAndTx tx, String msg) {
        showItemOperationDialog(tx, msg);
    }

    @Override
    public void onUserClicked(String senderPk) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.PUBLIC_KEY, senderPk);
        ActivityUtil.startActivity(intent, this, UserDetailActivity.class);
    }

    @Override
    public void onEditNameClicked(String senderPk){
        userViewModel.showRemarkDialog(activity, senderPk);
    }

    @Override
    public void onBanClicked(UserAndTx tx){
        String showName = UsersUtil.getShowName(tx.sender, tx.senderPk);
        userViewModel.showBanDialog(activity, tx.senderPk, showName);
    }

    @Override
    public void onTrustClicked(User user) {
        showTrustDialog(user);
    }

    @Override
    public void onItemClicked(UserAndTx tx) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.ID, tx.txID);
        intent.putExtra(IntentExtra.CHAIN_ID, tx.chainID);
        intent.putExtra(IntentExtra.PUBLIC_KEY, tx.senderPk);
        ActivityUtil.startActivity(intent, activity, SellDetailActivity.class);
    }

    /**
     * 显示信任的对话框
     */
    private void showTrustDialog(User user) {
        DialogTrustBinding binding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.dialog_trust, null, false);
        String showName = UsersUtil.getShowName(user);

        Spanned trustTip = Html.fromHtml(getString(R.string.tx_give_trust_tip, showName));
        binding.tvTrustTip.setText(trustTip);
        long txFee = txViewModel.getTxFee(chainID);
        String txFeeStr = FmtMicrometer.fmtFeeValue(txFee);

        String medianFree = getString(R.string.tx_median_fee, txFeeStr,
                ChainIDUtil.getCoinName(chainID));
        binding.tvTrustFee.setText(Html.fromHtml(medianFree));
        binding.tvTrustFee.setTag(txFeeStr);

        binding.ivClose.setOnClickListener(v -> trustDialog.closeDialog());
        binding.tvSubmit.setOnClickListener(v -> {
            int txType = TxType.TRUST_TX.getType();
            String fee = ViewUtils.getStringTag(binding.tvTrustFee);
            Tx tx = new Tx(chainID, user.publicKey, FmtMicrometer.fmtTxLongValue(fee), txType);
            txViewModel.addTransaction(tx);
            trustDialog.closeDialog();
        });
        trustDialog = new CommonDialog.Builder(activity)
                .setContentView(binding.getRoot())
                .setCanceledOnTouchOutside(false)
                .enableWarpWidth(true)
                .create();
        trustDialog.show();
    }

    /**
     * 显示每个item长按操作选项对话框
     */
    private void showItemOperationDialog(UserAndTx tx, String msg) {
        ItemOperationsBinding binding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.item_operations, null, false);
        binding.setListener(this);
        binding.replay.setVisibility(View.GONE);
        // 用户不能拉黑自己
        if(StringUtil.isEquals(tx.senderPk,
                MainApplication.getInstance().getPublicKey())){
            binding.blacklist.setVisibility(View.GONE);
        }
        binding.replay.setTag(tx);
        binding.copy.setTag(msg);
        String link = SpannableUrl.parseUrlFormStr(msg);
        if(StringUtil.isNotEmpty(link)){
            binding.copyLink.setTag(link);
        }else{
            binding.copyLink.setVisibility(View.GONE);
        }
        binding.blacklist.setTag(tx.senderPk);
        binding.favourite.setTag(tx.txID);
        binding.msgHash.setTag(tx.txID);
        operationsDialog = new CommonDialog.Builder(activity)
                .setContentView(binding.getRoot())
                .enableWarpWidth(true)
                .create();
        operationsDialog.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeAllDialog();
    }

    private void closeAllDialog() {
        if (operationsDialog != null) {
            operationsDialog.closeDialog();
        }
        if (trustDialog != null) {
            trustDialog.closeDialog();
        }
    }

    @Override
    public void onClick(View v) {
        closeAllDialog();
        switch (v.getId()){
            case R.id.copy:
                String msg = ViewUtils.getStringTag(v);
                CopyManager.copyText(msg);
                ToastUtils.showShortToast(R.string.copy_successfully);
                break;
            case R.id.copy_link:
                String link = ViewUtils.getStringTag(v);
                CopyManager.copyText(link);
                ToastUtils.showShortToast(R.string.copy_link_successfully);
                break;
            case R.id.blacklist:
                String publicKey = ViewUtils.getStringTag(v);
                userViewModel.setUserBlacklist(publicKey, true);
                ToastUtils.showShortToast(R.string.blacklist_successfully);
                break;
            case R.id.favourite:
                String txID = ViewUtils.getStringTag(v);
                favoriteViewModel.addTxFavorite(txID);
                ToastUtils.showShortToast(R.string.favourite_successfully);
                break;
            case R.id.msg_hash:
                String msgHash = ViewUtils.getStringTag(v);
                CopyManager.copyText(msgHash);
                ToastUtils.showShortToast(R.string.copy_message_hash);
                break;
        }
    }

    @Override
    public void handleReadOnly(boolean isReadOnly) {
        if (null == binding) {
            return;
        }
        this.isReadOnly = isReadOnly;
        int color = isReadOnly ? R.color.gray_light : R.color.primary;
        binding.fabButton.setMainFabClosedBackgroundColor(getResources().getColor(color));
    }

    @Override
    public void onRefresh() {
        loadData(adapter.getItemCount());
    }

    private void loadData(int pos) {
        currentPos = pos;
        txViewModel.loadTxsData(currentTab, chainID, pos);
    }

    @Override
    public void onFragmentResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onFragmentResult(requestCode, resultCode, data);
        if (requestCode == TX_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            loadData(0);
        }
    }
}
