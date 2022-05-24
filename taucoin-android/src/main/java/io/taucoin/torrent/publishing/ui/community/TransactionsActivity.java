package io.taucoin.torrent.publishing.ui.community;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.databinding.ActivityCommunityChooseBinding;
import io.taucoin.torrent.publishing.databinding.ViewDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.transaction.TxUtils;
import io.taucoin.torrent.publishing.ui.transaction.TxViewModel;

/**
 * 钱包页面
 */
public class TransactionsActivity extends BaseActivity {

    private ActivityCommunityChooseBinding binding;
    private CompositeDisposable disposables = new CompositeDisposable();
    private TxViewModel viewModel;
    private TransactionListAdapter adapter;
    private String chainID;
    private CommonDialog txDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(TxViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_community_choose);
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
        binding.toolbarInclude.toolbar.setTitle(R.string.menu_transactions);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new TransactionListAdapter();
        binding.joinedList.setOnItemClickListener((view, adapterPosition) -> {
            UserAndTx tx = adapter.getCurrentList().get(adapterPosition);
            showTransactionDetail(tx);
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.joinedList.setLayoutManager(layoutManager);
        binding.joinedList.setAdapter(adapter);
    }

    private void showTransactionDetail(UserAndTx tx) {
        if (txDialog != null && txDialog.isShowing()) {
            txDialog.closeDialog();
        }
        ViewDialogBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.view_dialog, null, false);
        dialogBinding.tvMsg.setTextColor(getResources().getColor(R.color.color_black));
        dialogBinding.tvMsg.setText(TxUtils.createBlockTxSpan(tx));
        dialogBinding.tvMsg.setGravity(Gravity.START);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) dialogBinding.tvMsg.getLayoutParams();
        layoutParams.leftMargin = getResources().getDimensionPixelSize(R.dimen.widget_size_20);
        layoutParams.rightMargin = getResources().getDimensionPixelSize(R.dimen.widget_size_20);
        dialogBinding.tvMsg.setLayoutParams(layoutParams);
        dialogBinding.ivClose.setVisibility(View.VISIBLE);
        dialogBinding.ivClose.setOnClickListener(v -> {
            txDialog.closeDialog();
        });
        txDialog = new CommonDialog.Builder(this)
                .setContentView(dialogBinding.getRoot())
                .setCanceledOnTouchOutside(true)
                .create();
        txDialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Disposable disposable = viewModel.observeWalletTransactions(chainID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    if (list != null) {
                        adapter.submitList(list);
                    }
                });
        disposables.add(disposable);
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
    }
}