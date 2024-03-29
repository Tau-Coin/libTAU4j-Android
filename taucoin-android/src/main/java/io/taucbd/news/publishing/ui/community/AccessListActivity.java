package io.taucbd.news.publishing.ui.community;

import android.os.Bundle;

import java.util.List;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.model.TauDaemon;
import io.taucbd.news.publishing.core.utils.ObservableUtil;
import io.taucbd.news.publishing.databinding.ActivityListBinding;
import io.taucbd.news.publishing.ui.BaseActivity;
import io.taucbd.news.publishing.ui.constant.IntentExtra;

/**
 * 访问列表页面
 */
public class AccessListActivity extends BaseActivity implements AccessListAdapter.ClickListener{

    protected static final int ACCESS_LIST_TYPE = 0x01;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private ActivityListBinding binding;
    private AccessListAdapter adapter;
    private String chainID;
    private TauDaemon tauDaemon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_list);
        tauDaemon = TauDaemon.getInstance(getApplicationContext());
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
        binding.toolbarInclude.toolbar.setTitle(R.string.chain_access_list);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new AccessListAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        disposables.add(ObservableUtil.intervalSeconds(2, true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(l -> {
                    List<String> activeList = tauDaemon.getActiveList(chainID);
                    if (activeList != null && activeList.size() > 0) {
                        adapter.submitList(activeList);
                    }
                }));
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