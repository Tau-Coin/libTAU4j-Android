package io.taucbd.news.publishing.ui.community;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.model.TauDaemon;
import io.taucbd.news.publishing.core.model.TauDaemonAlertHandler;
import io.taucbd.news.publishing.core.model.data.Statistics;
import io.taucbd.news.publishing.core.storage.RepositoryHelper;
import io.taucbd.news.publishing.core.storage.sp.SettingsRepository;
import io.taucbd.news.publishing.core.utils.ActivityUtil;
import io.taucbd.news.publishing.core.utils.ChainIDUtil;
import io.taucbd.news.publishing.core.utils.FmtMicrometer;
import io.taucbd.news.publishing.core.utils.KeyboardUtils;
import io.taucbd.news.publishing.core.utils.ObservableUtil;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.databinding.ExternalAirdropLinkDialogBinding;
import io.taucbd.news.publishing.databinding.FragmentCommunityBinding;
import io.taucbd.news.publishing.ui.BaseFragment;
import io.taucbd.news.publishing.ui.customviews.ConfirmDialog;
import io.taucbd.news.publishing.ui.constant.IntentExtra;
import io.taucbd.news.publishing.ui.main.MainActivity;
import io.taucbd.news.publishing.ui.transaction.MarketTabFragment;

/**
 * 单个群组页面
 */
public class CommunityFragment extends BaseFragment implements View.OnClickListener {
    private static final Logger logger = LoggerFactory.getLogger("CommunityFragment");
    public static final int MEMBERS_REQUEST_CODE = 0x100;
    private MainActivity activity;
    private FragmentCommunityBinding binding;
    private CommunityViewModel communityViewModel;
    private SettingsRepository settingsRepo;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private MarketTabFragment currentFragment = null;
    private String chainID;
    private boolean nearExpired = false;
    private boolean chainStopped = false;
    private boolean isJoined = false;
    private boolean isNoBalance = true;
    private Statistics memberStatistics;
    private int onlinePeers = -1;
    private long nodes = 0;
    private long miningTime = -1;
    private boolean isConnectChain = true;
    private ConfirmDialog chainStoppedDialog;
    private boolean isVisibleToUser;

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
        loadNewsFragment();
        communityViewModel.touchChain(chainID);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        logger.debug("onHiddenChanged...hidden::{}", hidden);
        if (!hidden) {
            if (getArguments() != null) {
                String chainID = getArguments().getString(IntentExtra.ID);
                if (StringUtil.isNotEquals(this.chainID, chainID)) {
                    isConnectChain = true;
                    TauDaemonAlertHandler tauDaemonHandler = TauDaemon.getInstance(activity.getApplicationContext())
                            .getTauDaemonHandler();
                    tauDaemonHandler.getChainStoppedData().removeObserver(chainStoppedObserver);
                    tauDaemonHandler.getChainStoppedData().observe(this.getViewLifecycleOwner(), chainStoppedObserver);
                }
                this.chainID = chainID;
                subscribeCommunityViewModel();
            }
        } else {
            disposables.clear();
            closeAllDialog();
            this.chainID = null;
        }
        if (currentFragment != null) {
            currentFragment.onHiddenChanged(hidden, chainID);
        }
    }

    private final Observer<CopyOnWriteArraySet<String>> chainStoppedObserver = new Observer<>() {
        @Override
        public void onChanged(CopyOnWriteArraySet<String> set) {
            boolean chainStopped = set != null && StringUtil.isNotEmpty(chainID) && set.contains(chainID);
            if (CommunityFragment.this.chainStopped != chainStopped) {
                CommunityFragment.this.chainStopped = chainStopped;
                showWarningView();
            }
        }
    };

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
        Context context = activity.getApplicationContext();
        TauDaemonAlertHandler tauDaemonHandler = TauDaemon.getInstance(context).getTauDaemonHandler();
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

        // 检测区块链是否因为获取数据失败而停止
        tauDaemonHandler.getChainStoppedData()
                .observe(this.getViewLifecycleOwner(), set -> {
                    boolean chainStopped = set != null && StringUtil.isNotEmpty(chainID) && set.contains(chainID);
                    if (this.chainStopped != chainStopped) {
                        this.chainStopped = chainStopped;
                        showWarningView();
                    }
                });
    }
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
                            subtitle.append(getString(R.string.community_users_mining_preparation));
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
//        if (isHidden()) {
//            return;
//        }
//        logger.debug("onStart...");
//        subscribeCommunityViewModel();
    }

    @Override
    public void onStop() {
        super.onStop();
        disposables.clear();
    }

    private void closeAllDialog() {
        if (chainStoppedDialog != null && chainStoppedDialog.isShowing()) {
            chainStoppedDialog.closeDialog();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeAllDialog();
    }

    /**
     * 订阅社区相关的被观察者
     */
    private void subscribeCommunityViewModel() {
        if (StringUtil.isEmpty(chainID)) {
            return;
        }
        TauDaemon tauDaemon = TauDaemon.getInstance(activity.getApplicationContext());
        disposables.add(ObservableUtil.intervalSeconds(2, true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(l -> {
                    List<String> activeList = tauDaemon.getActiveList(chainID);
                    int activeListSize = activeList != null ? activeList.size() : 0;
                    if (this.onlinePeers != activeListSize) {
                        this.onlinePeers = activeListSize;
                    }
                }));

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
                    if (currentFragment != null) {
                        currentFragment.handleMember(member);
                    }
                    binding.flJoin.setVisibility(member.isJoined() ? View.GONE : View.VISIBLE);
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
        if (currentFragment != null) {
            currentFragment.onFragmentResult(requestCode, resultCode, data);
        }
    }

    private void loadNewsFragment() {
        currentFragment = new MarketTabFragment();
        Bundle bundle = new Bundle();
        bundle.putString(IntentExtra.CHAIN_ID, chainID);
        bundle.putBoolean(IntentExtra.IS_JOINED, isJoined);
        currentFragment.setArguments(bundle);

        FragmentManager fm = this.getChildFragmentManager();
        if (fm.isDestroyed()) {
            return;
        }
        FragmentTransaction transaction = fm.beginTransaction();
        // Replace whatever is in the fragment container view with this fragment,
        // and add the transaction to the back stack
        transaction.replace(R.id.news_fragment, currentFragment);
        // 执行此方法后，fragment的onDestroy()方法和ViewModel的onCleared()方法都不执行
        // transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
    }
}
