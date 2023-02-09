package io.taucbd.news.publishing.ui.main;

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import java.util.ArrayList;
import java.util.List;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucbd.news.publishing.BuildConfig;
import io.taucbd.news.publishing.MainApplication;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Member;
import io.taucbd.news.publishing.core.utils.ChainIDUtil;
import io.taucbd.news.publishing.core.utils.LinkUtil;
import io.taucbd.news.publishing.core.utils.SpanUtils;
import io.taucbd.news.publishing.databinding.ActivityCrytoCitiesBinding;
import io.taucbd.news.publishing.databinding.ItemGroupComunityBinding;
import io.taucbd.news.publishing.ui.BaseActivity;
import io.taucbd.news.publishing.ui.community.CommunityViewModel;

public class CryptoCitiesActivity extends BaseActivity {
    private ActivityCrytoCitiesBinding binding;
    private CommunityViewModel communityViewModel;
    private MyExpandableListAdapter adapter;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_cryto_cities);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.drawer_tau_communities);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        SpannableStringBuilder spannableStringBuilder = new SpanUtils()
                .append("TAU Communities are public places for chats and news. These communities have logos.  The coins economics is as following:")
                .append("\n\n")
                .append("- Initial Volume: ")
                .append("10,000,000")
                .setForegroundColor(getResources().getColor(R.color.color_yellow))
                .setBold()
                .setFontSize(20, true)
                .append(" coins").append("\n")
                .append("- Mining Rewards: ")
                .append("10")
                .setForegroundColor(getResources().getColor(R.color.color_yellow))
                .setBold()
                .setFontSize(20, true)
                .append(" coins each 5 minutes for winner.")
                .create();
        binding.tvLondonPmcDesc.setText(spannableStringBuilder);

        List<GroupItem> groupItems = new ArrayList<>();
        combinedCommunityData(groupItems, BuildConfig.CHAIN_LIST1);
        combinedCommunityData(groupItems, BuildConfig.CHAIN_LIST2);

        adapter = new MyExpandableListAdapter(groupItems, communityViewModel);
        binding.listView.setAdapter(adapter);

        View headerView = binding.llHeader;
        binding.llRoot.removeView(headerView);
        binding.listView.addHeaderView(headerView);
        headerView.setVisibility(View.VISIBLE);
    }

    private void combinedCommunityData(List<GroupItem> groupItems, ArrayList<String> chainList) {
        if (chainList != null && chainList.size() > 0) {
            GroupItem group = new GroupItem();
            groupItems.add(group);

            String[] chainData = chainList.get(0).split(",");
            group.data = chainData[0];

            for (int i = 1; i < chainList.size(); i++) {
                group.children.add(chainList.get(i).split(",")[0]);
            }
        }
    }

    static class GroupItem {
        private String data;
        private final List<String> children = new ArrayList<>();
    }

    private static class MyExpandableListAdapter extends BaseExpandableListAdapter {

        private final LayoutInflater inflater;
        private final List<GroupItem> groupItems;
        private final List<String> joinedChains = new ArrayList<>();
        private CommunityViewModel communityViewModel;
        MyExpandableListAdapter(List<GroupItem> groupItems, CommunityViewModel communityViewModel) {
            this.groupItems = groupItems;
            this.inflater = LayoutInflater.from(MainApplication.getInstance());
            this.communityViewModel = communityViewModel;
        }

        public void setJoinedChains(List<Member> joinedChains) {
            if (joinedChains != null) {
                this.joinedChains.clear();
                for (Member member : joinedChains) {
                    this.joinedChains.add(member.chainID);
                }
                notifyDataSetChanged();
            }
        }
        @Override
        public int getGroupCount() {
            return groupItems.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return getGroup(groupPosition).children.size();
        }

        @Override
        public GroupItem getGroup(int groupPosition) {
            return groupItems.get(groupPosition);
        }

        @Override
        public String getChild(int groupPosition, int childPosition) {
            return getGroup(groupPosition).children.get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return 0;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            ItemGroupComunityBinding binding;
            if (null == convertView) {
                binding = DataBindingUtil.inflate(inflater, R.layout.item_group_comunity, parent, false);
                convertView = binding.getRoot();
                convertView.setTag(binding);
            } else {
                binding = (ItemGroupComunityBinding) convertView.getTag();
            }
            GroupItem group = getGroup(groupPosition);
            binding.tvName.setText(ChainIDUtil.getName(group.data));
            boolean isJoined = this.joinedChains.contains(group.data);
            binding.tvJoined.setVisibility(isJoined ? View.VISIBLE : View.GONE);
            binding.tvJoin.setVisibility(!isJoined? View.VISIBLE : View.GONE);
            binding.tvJoin.setOnClickListener(v -> {
                String chainLink = LinkUtil.encodeChain(BuildConfig.CHAIN_PEER, group.data, BuildConfig.CHAIN_PEER);
                communityViewModel.addCommunity(group.data, LinkUtil.decode(chainLink));
            });
            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            ItemGroupComunityBinding binding;
            if (null == convertView) {
                binding = DataBindingUtil.inflate(inflater, R.layout.item_group_comunity, parent, false);
                convertView = binding.getRoot();
                convertView.setTag(binding);
            } else {
                binding = (ItemGroupComunityBinding) convertView.getTag();
            }
            String child = getGroup(groupPosition).children.get(childPosition);
            String childName = "    " + ChainIDUtil.getName(child);
            binding.tvName.setText(childName);
            boolean isJoined = this.joinedChains.contains(child);
            binding.tvJoined.setVisibility(isJoined ? View.VISIBLE : View.GONE);
            binding.tvJoin.setVisibility(!isJoined? View.VISIBLE : View.GONE);
            binding.tvJoin.setOnClickListener(v -> {
                String chainLink = LinkUtil.encodeChain(BuildConfig.CHAIN_PEER, child, BuildConfig.CHAIN_PEER);
                communityViewModel.addCommunity(child, LinkUtil.decode(chainLink));
            });
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return getGroup(groupPosition).children.size() > 0;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        disposables.add(communityViewModel.observerJoinedCommunityList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(members -> {
                    if (adapter != null) {
                        adapter.setJoinedChains(members);
                    }
                }));
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }
}