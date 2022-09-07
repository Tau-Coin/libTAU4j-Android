package io.taucoin.tauapp.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.tauapp.publishing.core.utils.ActivityUtil;
import io.taucoin.tauapp.publishing.databinding.ActivityCommunityChooseBinding;
import io.taucoin.tauapp.publishing.databinding.ItemCommunityChooseBinding;
import io.taucoin.tauapp.publishing.ui.BaseActivity;
import io.taucoin.tauapp.publishing.ui.constant.IntentExtra;

/**
 * 钱包页面
 */
public class WalletActivity extends BaseActivity {

    private ActivityCommunityChooseBinding binding;
    private CommunityViewModel communityViewModel;
    private ChooseListAdapter adapter;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_community_choose);
        initLayout();
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.drawer_wallet);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new ChooseListAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.joinedList.setLayoutManager(layoutManager);
        binding.joinedList.setOnItemClickListener((view, adapterPosition) -> {
            // 选择社区退出返回数据
            Member member = adapter.getCurrentList().get(adapterPosition);
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.CHAIN_ID, member.chainID);
            ActivityUtil.startActivity(intent, this, CommunitiesActivity.class);
        });
        binding.joinedList.setAdapter(adapter);

        LayoutInflater inflater = LayoutInflater.from(this);
        ItemCommunityChooseBinding headerBinding = DataBindingUtil.inflate(inflater,
                R.layout.item_community_choose, null, false);
        headerBinding.tvName.setText(R.string.community_paste_join);
        headerBinding.viewTips.setVisibility(View.INVISIBLE);
        View headerView = headerBinding.getRoot();
        headerView.setOnClickListener(l -> {
            ActivityUtil.startActivity(this, CommunityJoinActivity.class);
        });
        binding.joinedList.addHeaderView(headerView);
    }

    @Override
    protected void onStart() {
        super.onStart();
        disposables.add(communityViewModel.observerJoinedCommunityList()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(members -> {
            if (adapter != null) {
                adapter.submitList(members);
            }
        }));
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }
}