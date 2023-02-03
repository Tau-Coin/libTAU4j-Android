package io.taucbd.news.publishing.ui.transaction;

import android.os.Bundle;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.databinding.ActivityTrustMoreBinding;
import io.taucbd.news.publishing.ui.BaseActivity;
import io.taucbd.news.publishing.ui.constant.IntentExtra;

/**
 * Trust more
 */
@Deprecated
public class TrustMoreActivity extends BaseActivity {
    private ActivityTrustMoreBinding binding;
    private TxViewModel txViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private TrustListAdapter adapter;
    private String chainID;
    private String trustPk;
    private int currentPos = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        txViewModel = provider.get(TxViewModel.class);
        txViewModel.observeNeedStartDaemon();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_trust_more);
        initParam();
        initView();
    }

    /**
     * 初始化参数
     */
    private void initParam() {
        if (getIntent() != null) {
            chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
            trustPk = getIntent().getStringExtra(IntentExtra.PUBLIC_KEY);
        }
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.tx_sell_detail_trust_hash);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        binding.refreshLayout.setOnRefreshListener(this);

        adapter = new TrustListAdapter();

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
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

    @Override
    protected void onStart() {
        super.onStart();
//        txViewModel.observerTrustTxs().observe(this, txs -> {
//            List<Tx> currentList = new ArrayList<>(txs);
//            if (currentPos == 0) {
//                adapter.submitList(currentList, handleUpdateAdapter);
//            } else {
//                currentList.addAll(adapter.getCurrentList());
//                adapter.submitList(currentList, handlePullAdapter);
//            }
//            binding.refreshLayout.setRefreshing(false);
//            binding.refreshLayout.setEnabled(txs.size() != 0 && txs.size() % Page.PAGE_SIZE == 0);
//        });

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
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    public void onRefresh() {
        loadData(adapter.getItemCount());
    }

    private void loadData(int pos) {
        currentPos = pos;
//        txViewModel.loadTrustTxsData(chainID, trustPk, pos);
    }
}