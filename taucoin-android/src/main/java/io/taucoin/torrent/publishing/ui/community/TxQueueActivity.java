package io.taucoin.torrent.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.TxQueueAndStatus;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.databinding.ActivityListBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.transaction.TransactionCreateActivity;
import io.taucoin.torrent.publishing.ui.transaction.TxViewModel;

/**
 * 交易队列页面
 */
public class TxQueueActivity extends BaseActivity implements QueueListAdapter.ClickListener {

    private ActivityListBinding binding;
    private TxViewModel txViewModel;
    private QueueListAdapter adapter;
    private String chainID;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        txViewModel = provider.get(TxViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_list);
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
        binding.toolbarInclude.toolbar.setTitle(R.string.community_transaction_queue);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new QueueListAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        observeTxQueue();
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    /**
     * 观察加入的社区列表
     */
    private void observeTxQueue() {
        Disposable subscribe = txViewModel.observeCommunityTxQueue(chainID)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(list -> {
                    if (adapter != null) {
                        adapter.submitList(list);
                    }
                });
        disposables.add(subscribe);
    }

    @Override
    public void onEditClicked(TxQueueAndStatus tx) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.BEAN, tx);
        ActivityUtil.startActivity(intent, this, TransactionCreateActivity.class);
    }
}