package io.taucoin.news.publishing.ui.community;

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.Gravity;
import android.view.LayoutInflater;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.model.data.IncomeAndExpenditure;
import io.taucoin.news.publishing.databinding.ActivityMiningIncomeBinding;
import io.taucoin.news.publishing.databinding.ViewContentDialogBinding;
import io.taucoin.news.publishing.ui.BaseActivity;
import io.taucoin.news.publishing.ui.constant.IntentExtra;
import io.taucoin.news.publishing.ui.customviews.ConfirmDialog;
import io.taucoin.news.publishing.ui.transaction.TxUtils;
import io.taucoin.news.publishing.ui.transaction.TxViewModel;

/**
 * 挖矿收入页面
 */
public class MiningIncomeActivity extends BaseActivity implements MiningIncomeListAdapter.ClickListener {

    private ActivityMiningIncomeBinding binding;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private Disposable itemDetailDisposable;
    private TxViewModel viewModel;
    private CommunityViewModel communityViewModel;
    private MiningIncomeListAdapter adapter;
    private String chainID;
    private ConfirmDialog txDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(TxViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_mining_income);
        initParameter();
        initLayout();
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
        binding.toolbarInclude.toolbar.setTitle(R.string.community_mining_income_pending);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new MiningIncomeListAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerList.setLayoutManager(layoutManager);
        binding.recyclerList.setAdapter(adapter);
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
        disposables.add(viewModel.observeMiningIncome(chainID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    adapter.submitList(list);
                }));
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
}