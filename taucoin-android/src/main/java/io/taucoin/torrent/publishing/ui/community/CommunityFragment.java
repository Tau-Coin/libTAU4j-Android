package io.taucoin.torrent.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.leinardi.android.speeddial.SpeedDialActionItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
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
import io.taucoin.torrent.publishing.ui.transaction.AirdropCreateActivity;
import io.taucoin.torrent.publishing.ui.transaction.BlocksTabFragment;
import io.taucoin.torrent.publishing.ui.transaction.CommunityTabFragment;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.main.MainActivity;
import io.taucoin.torrent.publishing.ui.transaction.ChainTabFragment;
import io.taucoin.torrent.publishing.ui.transaction.AnnouncementCreateActivity;
import io.taucoin.torrent.publishing.ui.transaction.MarketTabFragment;
import io.taucoin.torrent.publishing.ui.transaction.NotesTabFragment;
import io.taucoin.torrent.publishing.ui.transaction.QueueTabFragment;
import io.taucoin.torrent.publishing.ui.transaction.SellCreateActivity;
import io.taucoin.torrent.publishing.ui.transaction.SpinnerAdapter;
import io.taucoin.torrent.publishing.ui.transaction.TransactionCreateActivity;

import static io.taucoin.torrent.publishing.ui.transaction.CommunityTabFragment.TX_REQUEST_CODE;

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
    private CommunityTabFragment currentTabFragment = null;
    private RelativeLayout selectedView = null;
    private int currentTab = -1;
    private int[] spinnerItems;
    private int spinnerSelected = 0;
    private String chainID;
    private boolean isJoined = false;
    private boolean isOnChain = false;
    private boolean isNoBalance = true;
    private boolean isFirstLoad = true;
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
        initFabSpeedDial();
        onClick(binding.rlNotes);
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

        int verticalOffset = -getResources().getDimensionPixelSize(R.dimen.widget_size_5);
        binding.viewSpinner.setDropDownVerticalOffset(verticalOffset);
        binding.viewSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isFirstLoad) {
                    if (currentTab == CommunityTabFragment.TAB_CHAIN) {
                        spinnerSelected = position;
                        loadTabView(selectedView);
                    } else {
                        if (currentTabFragment != null) {
                            currentTabFragment.switchView(spinnerItems[position]);
                        }
                    }
                }
                isFirstLoad = false;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

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

    /**
     * 初始化右下角悬浮按钮组件
     */
    private void initFabSpeedDial() {
        FloatingActionButton mainFab = binding.fabButton.getMainFab();
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mainFab.getLayoutParams();
        layoutParams.gravity = Gravity.END | Gravity.BOTTOM;
        mainFab.setLayoutParams(layoutParams);
        mainFab.setCustomSize(getResources().getDimensionPixelSize(R.dimen.widget_size_50));
    }

    /**
     * 更新右下角悬浮按钮组件
     */
    private void updateFabSpeedDial(int currentTab) {
        if (currentTab == CommunityTabFragment.TAB_NOTES) {
            return;
        }
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.CHAIN_ID, chainID);
        intent.putExtra(IntentExtra.ON_CHAIN, isOnChain);
        intent.putExtra(IntentExtra.NO_BALANCE, !isOnChain || isNoBalance);

        if (currentTab == CommunityTabFragment.TAB_MARKET) {
            binding.fabButton.setMainFabAnimationRotateAngle(45);
            SpeedDialActionItem invitationItem = new SpeedDialActionItem.Builder(R.id.community_create_invitation,
                    R.drawable.ic_add_36dp)
                    .setFabSize(getResources().getDimensionPixelSize(R.dimen.widget_size_20))
                    .setLabel(getString(R.string.community_leader_invitation))
                    .setLabelColor(getResources().getColor(R.color.color_yellow))
                    .create();
            binding.fabButton.addActionItem(invitationItem);

            SpeedDialActionItem airdropItem = new SpeedDialActionItem.Builder(R.id.community_create_airdrop,
                    R.drawable.ic_add_36dp)
                    .setFabSize(getResources().getDimensionPixelSize(R.dimen.widget_size_14))
                    .setLabel(getString(R.string.community_airdrop))
                    .setLabelColor(getResources().getColor(R.color.color_yellow))
                    .create();
            binding.fabButton.addActionItem(airdropItem);

            SpeedDialActionItem sellItem = new SpeedDialActionItem.Builder(R.id.community_create_sell,
                    R.drawable.ic_add_36dp)
                    .setFabSize(getResources().getDimensionPixelSize(R.dimen.widget_size_30))
                    .setLabel(getString(R.string.community_sell_coins))
                    .setLabelColor(getResources().getColor(R.color.color_yellow))
                    .create();
            binding.fabButton.addActionItem(sellItem);

            binding.fabButton.getMainFab().setOnClickListener(v -> {
                if (binding.fabButton.isOpen()) {
                    binding.fabButton.close();
                } else {
                    binding.fabButton.open();
                }
            });
            binding.fabButton.setOnActionSelectedListener(actionItem -> {
                if (!isJoined) {
                    return false;
                }
                switch (actionItem.getId()) {
                    case R.id.community_create_sell:
                        ActivityUtil.startActivityForResult(intent, activity, SellCreateActivity.class,
                                TX_REQUEST_CODE);
                        break;
                    case R.id.community_create_airdrop:
                        ActivityUtil.startActivityForResult(intent, activity, AirdropCreateActivity.class,
                                TX_REQUEST_CODE);
                        break;
                    case R.id.community_create_invitation:
                        ActivityUtil.startActivityForResult(intent, activity, AnnouncementCreateActivity.class,
                                TX_REQUEST_CODE);
                        break;
                }
                return false;
            });
        } else {
            binding.fabButton.clearActionItems();
            binding.fabButton.setMainFabAnimationRotateAngle(0);
            // 自定义点击事件
            binding.fabButton.getMainFab().setOnClickListener(v -> {
                if (!isJoined) {
                    return;
                }
                // chain
                ActivityUtil.startActivityForResult(intent, activity, TransactionCreateActivity.class,
                        TX_REQUEST_CODE);
            });
        }
    }

    private void refreshSpinnerView() {
        if (null == currentTabFragment || null == spinnerItems) {
            return;
        }
        SpinnerAdapter adapter = new SpinnerAdapter(activity, spinnerItems);
        isFirstLoad = true;
        binding.viewSpinner.setSelection(0);
        binding.viewSpinner.setAdapter(adapter);
    }

    /**
     * 加载Tab视图
     */
    private void loadTabView(View view) {
        KeyboardUtils.hideSoftInput(activity);

        Bundle bundle = new Bundle();
        bundle.putString(IntentExtra.CHAIN_ID, chainID);
        bundle.putBoolean(IntentExtra.IS_JOINED, isJoined);

        if (currentTabFragment != null) {
            currentTabFragment.hideView();
        }

        switch (view.getId()) {
            case R.id.rl_notes:
                // note
                spinnerItems = new int[] {};
                currentTabFragment = new NotesTabFragment();
                currentTab = CommunityTabFragment.TAB_NOTES;
                break;
            case R.id.rl_market:
                // market
                spinnerItems = new int[] {R.string.community_view_all,
                        R.string.community_view_sell,
                        R.string.community_view_airdrop,
                        R.string.community_view_announcement};
                currentTabFragment = new MarketTabFragment();
                currentTab = CommunityTabFragment.TAB_MARKET;
                break;
            case R.id.rl_chain:
                // chain
                spinnerItems = new int[] {R.string.community_view_blocks,
                        R.string.community_view_own_txs,
                        R.string.community_view_mining_pool};
                if (spinnerItems[spinnerSelected] == R.string.community_view_own_txs) {
                    currentTabFragment = new QueueTabFragment();
                } else if (spinnerItems[spinnerSelected] == R.string.community_view_blocks) {
                    currentTabFragment = new BlocksTabFragment();
                } else {
                    currentTabFragment = new ChainTabFragment();
                }
                currentTab = CommunityTabFragment.TAB_CHAIN;
                break;
        }
        if (currentTabFragment != null) {
            currentTabFragment.setArguments(bundle);
        }
        replaceOrRemoveFragment(false);
    }

    private void replaceOrRemoveFragment(boolean isRemove) {
        if (currentTabFragment != null && activity != null) {
            FragmentManager fm = activity.getSupportFragmentManager();
            if (fm.isDestroyed()) {
                return;
            }
            FragmentTransaction transaction = fm.beginTransaction();
            if (!isRemove) {
                // Replace whatever is in the fragment container view with this fragment,
                // and add the transaction to the back stack
                transaction.replace(R.id.tab_fragment, currentTabFragment);
                // 执行此方法后，fragment的onDestroy()方法和ViewModel的onCleared()方法都不执行
                // transaction.addToBackStack(null);
                transaction.commitAllowingStateLoss();
            } else {
                transaction.remove(currentTabFragment);
                transaction.commitAllowingStateLoss();
                currentTabFragment = null;
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        subscribeCommunityViewModel();
        communityViewModel.setVisitChain(chainID);
    }

    @Override
    public void onStop() {
        super.onStop();
        disposables.clear();
        communityViewModel.unsetVisitChain(chainID);
        if (helpDialog != null && helpDialog.isShowing()) {
            helpDialog.closeDialog();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        replaceOrRemoveFragment(true);
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
                    if (currentTabFragment != null) {
                        currentTabFragment.handleMember(member);
                        int color = !isJoined ? R.color.gray_light : R.color.primary;
                        binding.fabButton.setMainFabClosedBackgroundColor(getResources().getColor(color));
                    }
                    binding.flJoin.setVisibility(member.isJoined() ? View.GONE : View.VISIBLE);
//                    binding.msgUnread.setVisibility(member.msgUnread == 1  ? View.VISIBLE : View.GONE);
                }, it -> {}));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rl_notes:
                onTabClick(v, CommunityTabFragment.TAB_NOTES);
                break;
            case R.id.rl_market:
                onTabClick(v, CommunityTabFragment.TAB_MARKET);
                break;
            case R.id.rl_chain:
                onTabClick(v, CommunityTabFragment.TAB_CHAIN);
                break;
            case R.id.tv_join:
                communityViewModel.joinCommunity(chainID);
                break;
            case R.id.iv_help:
                showHelpDialog();
                break;
        }
    }

    private void onTabClick(View v, int currentTab) {
        // 避免同一页面多次刷新
        if (this.selectedView != null && selectedView.getId() == v.getId()) {
            return;
        }
        boolean isShow = currentTab != CommunityTabFragment.TAB_NOTES;
        binding.fabButton.setVisibility(isShow ? View.VISIBLE : View.GONE);
        binding.rlBottom.setVisibility(isShow ? View.VISIBLE : View.GONE);

        updateFabSpeedDial(currentTab);

        if (this.selectedView != null) {
            this.selectedView.setBackgroundResource(R.drawable.white_rect_round_bg_no_border);
            TextView textView = (TextView) selectedView.getChildAt(0);
            textView.setTextColor(getResources().getColor(R.color.gray_dark));
        }
        RelativeLayout selectedView = (RelativeLayout) v;
        selectedView.setBackgroundResource(R.drawable.yellow_rect_round_border_small_radius);
        TextView textView = (TextView) selectedView.getChildAt(0);
        textView.setTextColor(getResources().getColor(R.color.color_yellow));
        this.selectedView = selectedView;
        this.spinnerSelected = 0;
        loadTabView(selectedView);
        refreshSpinnerView();
    }

    @Override
    public void onFragmentResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onFragmentResult(requestCode, resultCode, data);
        if (currentTabFragment != null) {
            currentTabFragment.onFragmentResult(requestCode, resultCode, data);
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
}
