package io.taucoin.torrent.publishing.ui.community;

import android.os.Bundle;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.databinding.ActivityListBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;

/**
 * 同步状态页面
 */
public class SyncStatusActivity extends BaseActivity {

    private CompositeDisposable disposables = new CompositeDisposable();
    private ActivityListBinding binding;
    private SyncListAdapter adapter;
    private CommunityViewModel communityViewModel;
    private String chainID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
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
        binding.toolbarInclude.toolbar.setTitle(R.string.community_sync_status);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new SyncListAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        disposables.add(communityViewModel.observeCommunitySyncStatus(chainID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> adapter.submitList(list)));
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }
}