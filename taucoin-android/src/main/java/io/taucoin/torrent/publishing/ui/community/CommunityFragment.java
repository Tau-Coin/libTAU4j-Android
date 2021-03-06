package io.taucoin.torrent.publishing.ui.community;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
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
import io.taucoin.torrent.publishing.core.model.data.Statistics;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.KeyboardUtils;
import io.taucoin.torrent.publishing.core.utils.ObservableUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.databinding.EditFeeDialogBinding;
import io.taucoin.torrent.publishing.databinding.ExternalAirdropLinkDialogBinding;
import io.taucoin.torrent.publishing.databinding.FragmentCommunityBinding;
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
 * ??????????????????
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
    private boolean isJoined = false;
    private boolean isOnChain = false;
    private boolean isNoBalance = true;
    private Statistics memberStatistics;
    private AccessList accessList;
    private long nodes = 0;
    private CommonDialog chainStoppedDialog;

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
     * ???????????????
     */
    private void initParameter() {
        if (getArguments() != null) {
            chainID = getArguments().getString(IntentExtra.ID);
        }
    }

    /**
     * ???????????????
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

        // ????????????Adapter?????????FragmentPagerAdapter
        StateAdapter stateAdapter = new StateAdapter(this.getChildFragmentManager(),
                binding.tabLayout.getTabCount());
        // ViewPager??????Adapter
        binding.viewPager.setAdapter(stateAdapter);
        binding.viewPager.setOffscreenPageLimit(3);
        binding.tabLayout.setupWithViewPager(binding.viewPager);
        binding.tabLayout.addOnTabSelectedListener(onTabSelectedListener);

        // ??????????????????????????????????????????????????????
        TauDaemon.getInstance(activity.getApplicationContext()).getChainStoppedSet()
                .observe(this.getViewLifecycleOwner(), set -> {
                    if (set != null && set.contains(chainID)) {
                        binding.ivHelp.setVisibility(View.VISIBLE);
                    } else {
                        binding.ivHelp.setVisibility(View.INVISIBLE);
                    }
        });
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
            long connected = accessList.getConnectedSize();
            if (connected > 0) {
                subtitle.append(getString(R.string.community_users_stats_c, connected));
            }
        }
        if (isJoined) {
            // ???????????????
            subtitle.append(nodes > 0 ? getString(R.string.community_users_mining) : getString(R.string.community_users_discovering));
        } else {
            // ???????????????
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
     * ?????????????????????????????????
     */
    private void subscribeCommunityViewModel() {
        communityViewModel.getSetBlacklistState().observe(this, state -> {
            if(state){
                activity.goBack();
            }
        });

        // 60s??????????????????
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
                showChainStoppedDialog();
                break;
        }
    }

    /**
     * ???????????????????????????
     */
    private void showChainStoppedDialog() {
        if (chainStoppedDialog != null && chainStoppedDialog.isShowing()) {
            return;
        }
        Context context = activity.getApplicationContext();
        ExternalAirdropLinkDialogBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(context),
                R.layout.external_airdrop_link_dialog, null, false);
        dialogBinding.tvPeer.setText(R.string.community_stopped_running);
        dialogBinding.tvPeer.setTextColor(context.getResources().getColor(R.color.color_black));
        dialogBinding.tvJoin.setText(R.string.common_restart);
        dialogBinding.tvSkip.setOnClickListener(view -> {
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
        chainStoppedDialog = new CommonDialog.Builder(activity)
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
