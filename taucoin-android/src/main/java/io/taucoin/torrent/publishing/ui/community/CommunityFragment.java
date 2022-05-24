package io.taucoin.torrent.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.AccessList;
import io.taucoin.torrent.publishing.core.model.data.BlockStatistics;
import io.taucoin.torrent.publishing.core.model.data.Statistics;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.KeyboardUtils;
import io.taucoin.torrent.publishing.core.utils.ObservableUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.FragmentCommunityBinding;
import io.taucoin.torrent.publishing.databinding.ViewDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseFragment;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.customviews.FragmentStatePagerAdapter;
import io.taucoin.torrent.publishing.ui.transaction.CommunityTabFragment;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.main.MainActivity;
import io.taucoin.torrent.publishing.ui.transaction.MarketTabFragment;
import io.taucoin.torrent.publishing.ui.transaction.NotesTabFragment;
import io.taucoin.torrent.publishing.ui.transaction.TransactionsTabFragment;

/**
 * 单个群组页面
 */
public class CommunityFragment extends BaseFragment implements View.OnClickListener {

    public static final int MEMBERS_REQUEST_CODE = 0x100;
    private MainActivity activity;
    private FragmentCommunityBinding binding;
    private CommunityViewModel communityViewModel;
    private SettingsRepository settingsRepo;
    private CompositeDisposable disposables = new CompositeDisposable();
    private CommunityTabFragment[] fragments = new CommunityTabFragment[3];
    private String chainID;
    private boolean isJoined = false;
    private boolean isOnChain = false;
    private boolean isNoBalance = true;
    private BlockStatistics blockStatistics;
    private Statistics memberStatistics;
    private AccessList accessList;
    private long nodes = 0;
    private CommonDialog helpDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_community, container, false);
        binding.setListener(this);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activity = (MainActivity) getActivity();
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        settingsRepo = RepositoryHelper.getSettingsRepository(activity.getApplicationContext());
        binding.setListener(this);
        binding.toolbarInclude.setListener(this);
        initParameter();
        initLayout();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if (getArguments() != null) {
            chainID = getArguments().getString(IntentExtra.ID);
        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.tvBlocksStatistics.setText(getString(R.string.community_blocks_stats, 0, 0));
        showCommunityTitle();
        showCommunitySubtitle();
        binding.toolbarInclude.ivBack.setOnClickListener(v -> {
            KeyboardUtils.hideSoftInput(activity);
            activity.goBack();
        });
        binding.toolbarInclude.tvSubtitle.setVisibility(View.VISIBLE);
        binding.toolbarInclude.ivAction.setVisibility(View.VISIBLE);
        binding.toolbarInclude.ivAction.setImageResource(R.mipmap.icon_community_detail);
        binding.toolbarInclude.ivAction.setOnClickListener(v -> {
            if (StringUtil.isEmpty(chainID)) {
                return;
            }
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.CHAIN_ID, chainID);
            intent.putExtra(IntentExtra.IS_JOINED, isJoined);
            intent.putExtra(IntentExtra.NO_BALANCE, isNoBalance);
            ActivityUtil.startActivityForResult(intent, activity, CommunityDetailActivity.class, MEMBERS_REQUEST_CODE);
        });

        // 自定义的Adapter继承自FragmentPagerAdapter
        StateAdapter stateAdapter = new StateAdapter(this.getChildFragmentManager(),
                binding.tabLayout.getTabCount());
        // ViewPager设置Adapter
        binding.viewPager.setAdapter(stateAdapter);
        binding.viewPager.setOffscreenPageLimit(3);
        binding.tabLayout.setupWithViewPager(binding.viewPager);
        binding.tabLayout.addOnTabSelectedListener(onTabSelectedListener);
    }

    private TabLayout.OnTabSelectedListener onTabSelectedListener = new TabLayout.OnTabSelectedListener() {

        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            int currentTab = tab.getPosition();
            int currentItem = binding.viewPager.getCurrentItem();
            if (currentTab != currentItem) {
                binding.viewPager.setCurrentItem(currentTab);
            }
            KeyboardUtils.hideSoftInput(activity);
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };

    private void handleSettingsChanged(String key) {
        if (StringUtil.isEquals(key, getString(R.string.pref_key_dht_nodes))) {
            nodes = settingsRepo.getLongValue(key, 0);
            showCommunitySubtitle();
        }
    }

    private void showCommunityTitle() {
        long total = 0;
        if (memberStatistics != null) {
            total = memberStatistics.getTotal();
        }
        if (StringUtil.isNotEmpty(chainID)) {
            String communityName = ChainIDUtil.getName(chainID);
            String communityTitle = getString(R.string.community_title, communityName, total);
            binding.toolbarInclude.tvTitle.setText(communityTitle);
        }
    }

    private void showCommunitySubtitle() {
        StringBuilder subtitle = new StringBuilder();
        if (memberStatistics != null) {
            long members = memberStatistics.getOnChain();
            if (members > 0) {
                subtitle.append(getString(R.string.community_users_stats_m, members));
            }
        }
        if (accessList != null) {
            long gossip = accessList.getGossipSize();
            if (gossip > 0) {
                subtitle.append(getString(R.string.community_users_stats_g, gossip));
            }
            long connected = accessList.getConnectedSize();
            if (connected > 0) {
                subtitle.append(getString(R.string.community_users_stats_c, connected));
            }
        }
        if (isJoined) {
            // 已加入社区
            if (nodes > 0) {
                // 如果社区成员就自己一个, 或者区块总数为0
                boolean isMiningAlone = (memberStatistics != null && memberStatistics.getTotal() == 1) ||
                        (blockStatistics != null && blockStatistics.getTotal() == 0);
                // 显示help, 区块总数为0，社区只有自己一个成员的话不显示
                boolean isShowHelp = blockStatistics != null && blockStatistics.getTotal() == 0;
                if (!isMiningAlone) {
                    long latestTime = TauDaemon.daemonStartTime;
                    if (blockStatistics != null && blockStatistics.getMaxCreateTime() > latestTime) {
                        latestTime = blockStatistics.getMaxCreateTime();
                    }
                    // 30分钟内无Total Blocks无变化为mining alone
                    float minutes = DateUtil.timeDiffMinutes(latestTime, DateUtil.getMillisTime());
                    isMiningAlone = minutes > 30;
                    if (!isShowHelp) {
                        isShowHelp = isMiningAlone;
                    }
                }
                subtitle.append(isMiningAlone ? getString(R.string.community_users_mining_alone) :
                        getString(R.string.community_users_mining));
                binding.ivHelp.setVisibility(isShowHelp ? View.VISIBLE : View.GONE);
            } else {
                subtitle.append(getString(R.string.community_users_discovering));
            }
        } else {
            // 未加入社区
            int length = subtitle.length();
            if (length > 0) {
                subtitle.delete(length - 2, length - 1);
            }
        }
        binding.toolbarInclude.tvSubtitle.setText(subtitle);
    }

    @Override
    public void onStart() {
        super.onStart();
        subscribeCommunityViewModel();
    }

    @Override
    public void onStop() {
        super.onStop();
        disposables.clear();
        if (helpDialog != null && helpDialog.isShowing()) {
            helpDialog.closeDialog();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding.tabLayout.removeOnTabSelectedListener(onTabSelectedListener);
    }

    /**
     * 订阅社区相关的被观察者
     */
    private void subscribeCommunityViewModel() {
        communityViewModel.getSetBlacklistState().observe(this, state -> {
            if(state){
                activity.goBack();
            }
        });

        // 60s更新检查一次
        disposables.add(ObservableUtil.intervalSeconds(60)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(l -> showCommunitySubtitle()));

        nodes = settingsRepo.getLongValue(getString(R.string.pref_key_dht_nodes), 0);
        disposables.add(settingsRepo.observeSettingsChanged()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleSettingsChanged));

        disposables.add(communityViewModel.getMembersStatistics(chainID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(statistics -> {
                    this.memberStatistics = statistics;
                    showCommunityTitle();
                    showCommunitySubtitle();
                }));

        disposables.add(communityViewModel.getBlocksStatistics(chainID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(statistics -> {
                    this.blockStatistics = statistics;
                    showCommunitySubtitle();
                    binding.tvBlocksStatistics.setText(getString(R.string.community_blocks_stats,
                            statistics.getTotal(), statistics.getOnChain()));
                })
        );

        disposables.add(communityViewModel.observeAccessList(chainID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    this.accessList = list;
                    showCommunitySubtitle();
                }));

        disposables.add(communityViewModel.observerCurrentMember(chainID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(member -> {
                    isJoined = member.isJoined();
                    if (isOnChain != member.onChain()) {
                        showCommunitySubtitle();
                    }
                    isOnChain = member.onChain();
                    isNoBalance = member.noBalance();
                    for (CommunityTabFragment fragment: fragments) {
                        if (fragment != null) {
                            fragment.handleMember(member);
                        }
                    }
                    binding.flJoin.setVisibility(member.isJoined() ? View.GONE : View.VISIBLE);
                }, it -> {}));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_join:
                communityViewModel.joinCommunity(chainID);
                break;
            case R.id.iv_help:
                showHelpDialog();
                break;
        }
    }

    @Override
    public void onFragmentResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onFragmentResult(requestCode, resultCode, data);
        for (CommunityTabFragment fragment : fragments) {
            if (fragment != null) {
                fragment.onFragmentResult(requestCode, resultCode, data);
            }
        }
    }

    private void showHelpDialog() {
        if (helpDialog != null && helpDialog.isShowing()) {
            helpDialog.closeDialog();
        }
        ViewDialogBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.view_dialog, null, false);
        dialogBinding.tvMsg.setTextColor(getResources().getColor(R.color.color_black));
        dialogBinding.tvMsg.setText(R.string.community_peers_help);
        dialogBinding.ivClose.setVisibility(View.GONE);
        helpDialog = new CommonDialog.Builder(activity)
                .setContentView(dialogBinding.getRoot())
                .setCanceledOnTouchOutside(true)
                .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                .create();
        helpDialog.show();
    }

    private CommunityTabFragment createFragmentView(int position) {
        int pos = position < binding.tabLayout.getTabCount() ? position : 0;
        CommunityTabFragment tab;
        if (pos == 0) {
            tab = new NotesTabFragment();
        } else if (pos == 1) {
            tab = new MarketTabFragment();
        } else {
            tab = new TransactionsTabFragment();
        }
        fragments[pos] = tab;

        Bundle bundle = new Bundle();
        bundle.putString(IntentExtra.CHAIN_ID, chainID);
        bundle.putBoolean(IntentExtra.IS_JOINED, isJoined);
        tab.setArguments(bundle);
        return tab;
    }

    public class StateAdapter extends FragmentStatePagerAdapter {

        StateAdapter(@NonNull FragmentManager fm, int count) {
            super(fm, count);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return createFragmentView(position);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 1) {
                return getString(R.string.community_chain_market);
            } else if (position == 2) {
                return getString(R.string.community_on_chain);
            } else {
                return getString(R.string.community_chain_note);
            }
        }
    }
}
