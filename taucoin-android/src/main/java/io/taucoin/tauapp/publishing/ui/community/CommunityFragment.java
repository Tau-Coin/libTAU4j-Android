package io.taucoin.tauapp.publishing.ui.community;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.tabs.TabLayout;

import org.slf4j.LoggerFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.model.TauDaemon;
import io.taucoin.tauapp.publishing.core.model.TauDaemonAlertHandler;
import io.taucoin.tauapp.publishing.core.model.data.Statistics;
import io.taucoin.tauapp.publishing.core.storage.RepositoryHelper;
import io.taucoin.tauapp.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.tauapp.publishing.core.utils.ActivityUtil;
import io.taucoin.tauapp.publishing.core.utils.ChainIDUtil;
import io.taucoin.tauapp.publishing.core.utils.FmtMicrometer;
import io.taucoin.tauapp.publishing.core.utils.KeyboardUtils;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.databinding.ExternalAirdropLinkDialogBinding;
import io.taucoin.tauapp.publishing.databinding.FragmentCommunityBinding;
import io.taucoin.tauapp.publishing.ui.BaseFragment;
import io.taucoin.tauapp.publishing.ui.customviews.CommonDialog;
import io.taucoin.tauapp.publishing.ui.customviews.ConfirmDialog;
import io.taucoin.tauapp.publishing.ui.customviews.FragmentStatePagerAdapter;
import io.taucoin.tauapp.publishing.ui.transaction.CommunityTabFragment;
import io.taucoin.tauapp.publishing.ui.constant.IntentExtra;
import io.taucoin.tauapp.publishing.ui.main.MainActivity;
import io.taucoin.tauapp.publishing.ui.transaction.MarketTabFragment;
import io.taucoin.tauapp.publishing.ui.transaction.NotesTabFragment;
import io.taucoin.tauapp.publishing.ui.transaction.TransactionsTabFragment;

/**
 * 单个群组页面
 */
public class CommunityFragment extends BaseFragment implements View.OnClickListener {

    public static final int MEMBERS_REQUEST_CODE = 0x100;
    private MainActivity activity;
    private FragmentCommunityBinding binding;
    private CommunityViewModel communityViewModel;
    private SettingsRepository settingsRepo;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final CommunityTabFragment[] fragments = new CommunityTabFragment[3];
    private String chainID;
    private boolean nearExpired = false;
    private boolean chainStopped = false;
    private boolean isJoined = false;
    private boolean isNoBalance = true;
    private Statistics memberStatistics;
    private int onlinePeers;
    private long nodes = 0;
    private long miningTime = -1;
    private boolean isConnectChain = true;
    private boolean isEnterSentTransactions = false;
    private ConfirmDialog chainStoppedDialog;

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
            isEnterSentTransactions = getArguments().getBoolean(IntentExtra.IS_ENTER_SENT_TRANSACTIONS, false);
        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        Context context = activity.getApplicationContext();
        TauDaemonAlertHandler tauDaemonHandler = TauDaemon.getInstance(context).getTauDaemonHandler();
        this.onlinePeers = tauDaemonHandler.getOnlinePeersCount(chainID);
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

//        int[] tabs = new int[]{R.string.community_chain_note, R.string.community_chain_market, R.string.community_on_chain};
//        for (int i = 0; i < tabs.length; i++) {
//            TabViewBinding tabBinding = DataBindingUtil.inflate(LayoutInflater.from(context),
//                    R.layout.tab_view, null, false);
//            tabBinding.tvTabTitle.setText(tabs[0]);
//            tabBinding.ivTabRed.setVisibility(View.VISIBLE);
//            TabLayout.Tab tab = binding.tabLayout.newTab();
//            tab.setCustomView(tabBinding.getRoot());
//            binding.tabLayout.addTab(tab);
//        }

        for (int i = 0; i < binding.tabLayout.getTabCount(); i++) {
            updateTabBadgeDrawable(i, true, false);
        }
        // 自定义的Adapter继承自FragmentPagerAdapter
        StateAdapter stateAdapter = new StateAdapter(this.getChildFragmentManager(),
                binding.tabLayout.getTabCount());
        // ViewPager设置Adapter
        binding.viewPager.setAdapter(stateAdapter);
        binding.viewPager.setOffscreenPageLimit(3);
        binding.tabLayout.setupWithViewPager(binding.viewPager);

        binding.tabLayout.addOnTabSelectedListener(onTabSelectedListener);

        // 检测区块链是否因为获取数据失败而停止
        tauDaemonHandler.getChainStoppedData()
                .observe(this.getViewLifecycleOwner(), set -> {
                    boolean chainStopped = set != null && set.contains(chainID);
                    if (this.chainStopped != chainStopped) {
                        this.chainStopped = chainStopped;
                        showWarningView();
                    }
                });

        tauDaemonHandler.getOnlinePeerData()
                .observe(this.getViewLifecycleOwner(), set -> {
                    int peers = tauDaemonHandler.getOnlinePeersCount(chainID);
                    if (this.onlinePeers != peers) {
                        this.onlinePeers = peers;
//                        showCommunitySubtitle();
                    }
                });

        if (isEnterSentTransactions) {
            binding.viewPager.setCurrentItem(2);
        }
    }

    private void updateTabBadgeDrawable(int index, boolean init, boolean visible) {
        TabLayout.Tab tab = binding.tabLayout.getTabAt(index);
        if (tab != null) {
            BadgeDrawable badgeDrawable = tab.getOrCreateBadge();
            if (init) {
                int badgeOffset = getResources().getDimensionPixelSize(R.dimen.widget_size_5);
                badgeDrawable.setHorizontalOffset(-badgeOffset);
                badgeDrawable.setVerticalOffset(badgeOffset);
                badgeDrawable.setBackgroundColor(getResources().getColor(R.color.color_red));
            }
            // 红点显示并且不在当前tab页
            badgeDrawable.setVisible(visible && binding.tabLayout.getSelectedTabPosition() != index);
        }
    }

    private final TabLayout.OnTabSelectedListener onTabSelectedListener = new TabLayout.OnTabSelectedListener() {

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
//            showCommunitySubtitle();
        }
    }

    /**
     * 显示顶部警告视图
     */
    private void showWarningView() {
        binding.llWarning.setVisibility(chainStopped || nearExpired ? View.VISIBLE : View.GONE);
        if (chainStopped) {
            binding.tvWarning.setText(R.string.community_stopped_running_tips);
        } else if (nearExpired) {
            binding.tvWarning.setText(R.string.community_near_expiry_tips);
        }
    }

    private void showCommunityTitle() {
        long total = 0;
        if (memberStatistics != null) {
            total = memberStatistics.getTotal();
        }
        if (StringUtil.isNotEmpty(chainID)) {
            String communityName = ChainIDUtil.getName(chainID);
            String communityCode = ChainIDUtil.getCode(chainID);
            String communityTitle = getString(R.string.main_community_name_total,
                    communityName, communityCode, total);
            binding.toolbarInclude.tvTitle.setText(communityTitle);
        }
    }

    private void showCommunitySubtitle() {
        StringBuilder subtitle = new StringBuilder();
        if (isJoined) {
            // 已加入社区
            if (nodes > 0) {
                if (miningTime >= 0) {
                    int maxMiningTime = 1800;
                    if (miningTime > maxMiningTime) {
                        subtitle.append(getString(R.string.community_users_mining_overflow));
                    } else {
                        double rate = (maxMiningTime - miningTime) * 1f / maxMiningTime;
                        rate = Math.sqrt(rate) * 100;
                        if (rate < 10) {
                            subtitle.append(getString(R.string.community_users_mining_missed));
                        } else {
                            subtitle.append(getString(R.string.community_users_mining_block));
                            subtitle.append(" ").append(FmtMicrometer.fmtFixedDecimal(rate)).append("%");
                            if (rate == 100) {
                                subtitle.append(getString(R.string.chain_mining_syncing));
                            }
                        }
                    }
                } else if (miningTime == -100) {
                    subtitle.append(getString(R.string.community_users_doze));
                }
            } else {
                subtitle.append(getString(R.string.community_users_discovering));
            }
            if (onlinePeers > 0 && subtitle.length() > 0) {
                subtitle.append(getString(R.string.community_users_stats_c, onlinePeers));
            }
        }
        binding.toolbarInclude.tvSubtitle.setText(subtitle);
        binding.toolbarInclude.tvSubtitle.setVisibility(subtitle.length() > 0 ? View.VISIBLE : View.GONE);
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding.tabLayout.removeOnTabSelectedListener(onTabSelectedListener);
        if (chainStoppedDialog != null && chainStoppedDialog.isShowing()) {
            chainStoppedDialog.closeDialog();
        }
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

        disposables.add(communityViewModel.observerCommunityMiningTime(chainID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(time -> {
                    this.miningTime = time;
                    showCommunitySubtitle();
                }, it->{}));

//        // 60s更新检查一次
//        disposables.add(ObservableUtil.intervalSeconds(60)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(l -> showCommunitySubtitle()));

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
                }));

        disposables.add(communityViewModel.observerCurrentMember(chainID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(member -> {
                    if (isJoined != member.isJoined()) {
                        isJoined = member.isJoined();
                        // 只有社区joined后触发
                        connectChain(chainID);
//                        showCommunitySubtitle();
                    }
                    isConnectChain = false;
                    if (nearExpired != member.nearExpired()) {
                        nearExpired = member.nearExpired();
                        showWarningView();
                    }
                    isNoBalance = member.noBalance();
                    for (CommunityTabFragment fragment: fragments) {
                        if (fragment != null) {
                            fragment.handleMember(member);
                        }
                    }
                    LoggerFactory.getLogger("updateTabBadgeDrawable")
                            .debug("msgUnread::{}, newsUnread::{}", member.msgUnread, member.newsUnread);
                    binding.flJoin.setVisibility(member.isJoined() ? View.GONE : View.VISIBLE);
                    updateTabBadgeDrawable(0, false, member.msgUnread == 1);
                    updateTabBadgeDrawable(1, false, member.newsUnread == 1);
                }, it -> {}));
    }

    /**
     * 连接链（只触发一次）
     * @param chainID 链ID
     */
    private void connectChain(String chainID) {
        if (isConnectChain && isJoined) {
            communityViewModel.connectChain(chainID);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_join:
                communityViewModel.joinCommunity(chainID);
                break;
            case R.id.ll_warning:
                showWarningDialog();
                break;
        }
    }

    /**
     * 显示警告的对话框
     */
    private void showWarningDialog() {
        if (chainStoppedDialog != null && chainStoppedDialog.isShowing()) {
            return;
        }
        Context context = activity.getApplicationContext();
        ExternalAirdropLinkDialogBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(context),
                R.layout.external_airdrop_link_dialog, null, false);
        if (chainStopped) {
            dialogBinding.tvPeer.setText(R.string.community_stopped_running);
            dialogBinding.tvJoin.setText(R.string.common_retry);
        } else {
            dialogBinding.tvPeer.setText(R.string.community_near_expiry);
            dialogBinding.tvJoin.setVisibility(View.GONE);
        }

        dialogBinding.tvPeer.setTextColor(context.getResources().getColor(R.color.color_black));
        dialogBinding.ivSkip.setOnClickListener(view -> {
            if (chainStoppedDialog != null) {
                chainStoppedDialog.closeDialog();
            }
        });
        dialogBinding.tvJoin.setOnClickListener(view -> {
            if (chainStoppedDialog != null) {
                chainStoppedDialog.closeDialog();
            }
            TauDaemon.getInstance(activity.getApplicationContext()).restartFailedChain(chainID);
        });
        chainStoppedDialog = new ConfirmDialog.Builder(activity)
                .setContentView(dialogBinding.getRoot())
                .setCanceledOnTouchOutside(false)
                .create();
        chainStoppedDialog.show();
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
        bundle.putBoolean(IntentExtra.IS_ENTER_SENT_TRANSACTIONS, isEnterSentTransactions);
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
