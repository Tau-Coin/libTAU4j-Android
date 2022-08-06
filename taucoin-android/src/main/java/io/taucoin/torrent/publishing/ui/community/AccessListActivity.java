package io.taucoin.torrent.publishing.ui.community;

import android.content.Context;
import android.os.Bundle;

import java.util.List;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.TauDaemonAlertHandler;
import io.taucoin.torrent.publishing.databinding.ActivityListBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;

/**
 * 访问列表页面
 */
public class AccessListActivity extends BaseActivity implements AccessListAdapter.ClickListener{

    protected static final int ACCESS_LIST_TYPE = 0x01;
    private CompositeDisposable disposables = new CompositeDisposable();
    private ActivityListBinding binding;
    private AccessListAdapter adapter;
    private CommunityViewModel communityViewModel;
    private String chainID;
    private int currentType;

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
            currentType = getIntent().getIntExtra(IntentExtra.TYPE, ACCESS_LIST_TYPE);
        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.chain_access_list);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new AccessListAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(adapter);

        TauDaemonAlertHandler tauDaemonHandler = TauDaemon.getInstance(getApplicationContext()).getTauDaemonHandler();
        adapter.submitList(tauDaemonHandler.getOnlinePeersList(chainID));
        tauDaemonHandler.getOnlinePeerMap()
                .observe(this, set -> {
                    adapter.submitList(tauDaemonHandler.getOnlinePeersList(chainID));
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    public void onClicked(String key) {

    }
}