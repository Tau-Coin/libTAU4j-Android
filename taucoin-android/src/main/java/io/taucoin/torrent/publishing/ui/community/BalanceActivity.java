package io.taucoin.torrent.publishing.ui.community;

import android.os.Bundle;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.databinding.ActivityCommunityBalanceBinding;
import io.taucoin.torrent.publishing.databinding.ActivityCommunityChooseBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;

/**
 * 社区余额列表页面
 */
@Deprecated
public class BalanceActivity extends BaseActivity {

    private ActivityCommunityBalanceBinding binding;
    private BalanceAdapter adapter;
    private CommunityViewModel communityViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_community_balance);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        initLayout();
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.drawer_balance);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new BalanceAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.joinedList.setLayoutManager(layoutManager);
        binding.joinedList.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Disposable subscribe = communityViewModel.observeCommunities()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(communities -> {
                    adapter.submitList(communities);
                });
        disposables.add(subscribe);
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }
}