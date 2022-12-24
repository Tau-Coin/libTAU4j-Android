package io.taucoin.news.publishing.ui.friends;

import android.os.Bundle;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.databinding.ActivityListBinding;
import io.taucoin.news.publishing.ui.BaseActivity;
import io.taucoin.news.publishing.ui.community.CommunityViewModel;
import io.taucoin.news.publishing.ui.constant.IntentExtra;

/**
 * Airdrop详情页
 */
public class AirdropHistoryActivity extends BaseActivity {

    private ActivityListBinding binding;
    private CommunityViewModel communityViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private AirdropHistoryAdapter adapter;
    private String chainID;
    private String publicKey;
    private long airdropTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_list);
        initLayout();
    }

    private void initLayout() {
        chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
        publicKey = getIntent().getStringExtra(IntentExtra.PUBLIC_KEY);
        airdropTime = getIntent().getLongExtra(IntentExtra.TIMESTAMP, 0);

        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.bot_airdrop_history);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new AirdropHistoryAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(adapter);
    }


    @Override
    protected void onStart() {
        super.onStart();
        Disposable airdropDisposable = communityViewModel.observeAirdropHistoryOnChain(chainID,
                publicKey, airdropTime)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    adapter.submitList(list);
                });
        disposables.add(airdropDisposable);

    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }
}
