package io.taucoin.torrent.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.databinding.ActivityCommunityChooseBinding;
import io.taucoin.torrent.publishing.databinding.ItemCommunityChooseBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;

/**
 * 群组选择页面
 */
public class CommunityChooseActivity extends BaseActivity {

    private ActivityCommunityChooseBinding binding;
    private CommunityViewModel communityViewModel;
    private ChooseListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_community_choose);
        initLayout();
        observeJoinedList();
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.drawer_communities);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new ChooseListAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.joinedList.setLayoutManager(layoutManager);
        binding.joinedList.setOnItemClickListener((view, adapterPosition) -> {
            // 选择社区退出返回数据
            Community community = adapter.getCurrentList().get(adapterPosition);
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.CHAIN_ID, community.chainID);
            ActivityUtil.startActivity(intent, this, CommunitiesActivity.class);
        });
        binding.joinedList.setAdapter(adapter);

        LayoutInflater inflater = LayoutInflater.from(this);
        ItemCommunityChooseBinding headerBinding = DataBindingUtil.inflate(inflater,
                R.layout.item_community_choose, null, false);
        headerBinding.tvName.setText(R.string.community_paste_join);
        View headerView = headerBinding.getRoot();
        headerView.setOnClickListener(l -> {
            ActivityUtil.startActivity(this, CommunityJoinActivity.class);
        });
        binding.joinedList.addHeaderView(headerView);
    }

    @Override
    protected void onStart() {
        super.onStart();
        communityViewModel.getJoinedCommunityList();
    }

    /**
     * 观察加入的社区列表
     */
    private void observeJoinedList() {
        communityViewModel.getJoinedList().observe(this, communities -> {
            if(adapter != null){
                adapter.submitList(communities);
            }
        });
    }
}