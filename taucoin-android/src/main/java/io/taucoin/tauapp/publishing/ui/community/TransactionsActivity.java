package io.taucoin.tauapp.publishing.ui.community;

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import cn.bingoogolapple.refreshlayout.BGARefreshLayout;
import cn.bingoogolapple.refreshlayout.BGAStickinessRefreshViewHolder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.model.data.IncomeAndExpenditure;
import io.taucoin.tauapp.publishing.core.model.data.UserAndFriend;
import io.taucoin.tauapp.publishing.core.model.data.UserAndTx;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.tauapp.publishing.core.utils.DateUtil;
import io.taucoin.tauapp.publishing.core.utils.ObservableUtil;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.core.utils.ToastUtils;
import io.taucoin.tauapp.publishing.databinding.ActivityCommunityChooseBinding;
import io.taucoin.tauapp.publishing.databinding.ActivityWalletTransactionsBinding;
import io.taucoin.tauapp.publishing.databinding.ViewContentDialogBinding;
import io.taucoin.tauapp.publishing.ui.BaseActivity;
import io.taucoin.tauapp.publishing.ui.constant.IntentExtra;
import io.taucoin.tauapp.publishing.ui.constant.Page;
import io.taucoin.tauapp.publishing.ui.customviews.ConfirmDialog;
import io.taucoin.tauapp.publishing.ui.friends.FriendsListAdapter;
import io.taucoin.tauapp.publishing.ui.transaction.TxUtils;
import io.taucoin.tauapp.publishing.ui.transaction.TxViewModel;

/**
 * 钱包页面
 */
public class TransactionsActivity extends BaseActivity implements TransactionListAdapter.ClickListener,
        BGARefreshLayout.BGARefreshLayoutDelegate {

    private ActivityWalletTransactionsBinding binding;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private Disposable itemDetailDisposable;
    private TxViewModel viewModel;
    private CommunityViewModel communityViewModel;
    private TransactionListAdapter adapter;
    private String chainID;
    private ConfirmDialog txDialog;
    private boolean dataChanged = false;
    private boolean isLoadMore = false;
    private int currentPos = 0;
    private long timeRefresh = DateUtil.getTime();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(TxViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_wallet_transactions);
        initParameter();
        initLayout();
        initRefreshLayout();
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
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.menu_transactions);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new TransactionListAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerList.setLayoutManager(layoutManager);
        binding.recyclerList.setAdapter(adapter);
    }

    private void initRefreshLayout() {
        binding.refreshLayout.setDelegate(this);
        BGAStickinessRefreshViewHolder refreshViewHolder = new BGAStickinessRefreshViewHolder(this, true);
        refreshViewHolder.setRotateImage(R.mipmap.ic_launcher_foreground);
        refreshViewHolder.setStickinessColor(R.color.color_yellow);

        refreshViewHolder.setLoadingMoreText(getString(R.string.common_loading));
        binding.refreshLayout.setPullDownRefreshEnable(false);

        binding.refreshLayout.setRefreshViewHolder(refreshViewHolder);
    }

    @Override
    public void onItemClicked(IncomeAndExpenditure entry) {
        if (itemDetailDisposable != null && !itemDetailDisposable.isDisposed()) {
            itemDetailDisposable.dispose();
        }
        if (entry.txType == -1) {
            itemDetailDisposable = communityViewModel.observeBlockByHash(chainID, entry.hash)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(block -> {
                        if (block != null) {
                            showTransactionDetail(TxUtils.createBlockSpan(block));
                        }
                    }, it -> {});
        } else {
            itemDetailDisposable = viewModel.observeTxByTxID(entry.hash)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(tx -> {
                if (tx != null) {
                    showTransactionDetail(TxUtils.createBlockTxSpan(tx));
                }
            }, it -> {});
        }
    }

    private void showTransactionDetail(SpannableStringBuilder stringBuilder) {
        if (txDialog != null && txDialog.isShowing()) {
            txDialog.closeDialog();
            if (itemDetailDisposable != null && !itemDetailDisposable.isDisposed()) {
                itemDetailDisposable.dispose();
            }
        }
        ViewContentDialogBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.view_content_dialog, null, false);
        dialogBinding.tvMsg.setTextColor(getResources().getColor(R.color.color_black));
        dialogBinding.tvMsg.setText(stringBuilder);
        dialogBinding.tvMsg.setGravity(Gravity.START);
        dialogBinding.ivClose.setOnClickListener(v -> {
            txDialog.closeDialog();
            if (itemDetailDisposable != null && !itemDetailDisposable.isDisposed()) {
                itemDetailDisposable.dispose();
            }
        });
        txDialog = new ConfirmDialog.Builder(this)
                .setContentView(dialogBinding.getRoot())
                .setWarpView(dialogBinding.tvMsg)
                .setCanceledOnTouchOutside(true)
                .create();
        txDialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        communityViewModel.clearCommunityAccountTips(chainID);
        loadData(0);
        viewModel.getWalletTxs().observe(this, list -> {
            int size;
            if (currentPos == 0) {
                adapter.submitList(list);
                size = list.size();
                isLoadMore = size != 0 && size % Page.PAGE_SIZE == 0;
            } else {
                List<IncomeAndExpenditure> currentList = new ArrayList<>();
                currentList.addAll(adapter.getCurrentList());
                currentList.addAll(list);
                adapter.submitList(currentList);
                isLoadMore = list.size() != 0 && list.size() % Page.PAGE_SIZE == 0;
            }
            binding.refreshLayout.endLoadingMore();
        });

        disposables.add(viewModel.observeWalletChanged()
                .subscribeOn(Schedulers.io())
                .subscribe(o -> dataChanged = true));

        disposables.add(ObservableUtil.interval(500)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(o -> {
                    forceListRefresh();
                    if (dataChanged) {
                        loadData(0);
                        dataChanged = false;
                    }
                }));
    }

    /**
     * 数据一分钟强制刷新一次（更新Confirm Rate）
     */
    private void forceListRefresh() {
        long currentTime = DateUtil.getTime();
        if (currentTime - timeRefresh > 60) {
            adapter.notifyDataSetChanged();
            timeRefresh = currentTime;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (txDialog != null && txDialog.isShowing()) {
            txDialog.closeDialog();
        }
        if (itemDetailDisposable != null && !itemDetailDisposable.isDisposed()) {
            itemDetailDisposable.dispose();
        }
    }

    private int getItemCount() {
        int count = 0;
        if (adapter != null) {
            count = adapter.getItemCount();
        }
        return count;
    }

    protected void loadData(int pos) {
        this.currentPos = pos;
        viewModel.queryWalletIncomeAndExpenditure(chainID, pos, getItemCount());
    }

    @Override
    public void onBGARefreshLayoutBeginRefreshing(BGARefreshLayout refreshLayout) {
        refreshLayout.endRefreshing();
        refreshLayout.setPullDownRefreshEnable(false);
    }

    @Override
    public boolean onBGARefreshLayoutBeginLoadingMore(BGARefreshLayout refreshLayout) {
        logger.debug("LoadingMore isLoadMore::{}", isLoadMore);
        if (isLoadMore) {
            loadData(getItemCount());
            return true;
        } else {
            refreshLayout.endLoadingMore();
            return false;
        }
    }
}