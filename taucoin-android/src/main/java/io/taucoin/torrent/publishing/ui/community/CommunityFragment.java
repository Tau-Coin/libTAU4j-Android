package io.taucoin.torrent.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
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
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.KeyboardUtils;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.FragmentCommunityBinding;
import io.taucoin.torrent.publishing.ui.BaseFragment;
import io.taucoin.torrent.publishing.ui.transaction.AirdropCreateActivity;
import io.taucoin.torrent.publishing.ui.transaction.BlocksTabFragment;
import io.taucoin.torrent.publishing.ui.transaction.CommunityTabFragment;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.main.MainActivity;
import io.taucoin.torrent.publishing.ui.transaction.ChainTabFragment;
import io.taucoin.torrent.publishing.ui.transaction.LeaderInvitationCreateActivity;
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
    private CompositeDisposable disposables = new CompositeDisposable();
    private CommunityTabFragment currentTabFragment = null;
    private TextView selectedView = null;
    private int currentTab = -1;
    private int[] spinnerItems;
    private int spinnerSelected = 0;
    private String chainID;
    private boolean isJoined = false;
    private boolean isOnChain = false;
    private boolean isNoBalance = true;
    private boolean isFirstLoad = true;

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
        binding.setListener(this);
        binding.toolbarInclude.setListener(this);
        initParameter();
        initLayout();
        initFabSpeedDial();
        onClick(binding.tvNotes);
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

        if (StringUtil.isNotEmpty(chainID)) {
            String communityName = ChainIDUtil.getName(chainID);
            binding.toolbarInclude.tvTitle.setText(Html.fromHtml(communityName));
            binding.toolbarInclude.tvSubtitle.setText(getString(R.string.community_users_stats, 0));
        }
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
                        ActivityUtil.startActivityForResult(intent, activity, LeaderInvitationCreateActivity.class,
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
        bundle.putBoolean(IntentExtra.NO_BALANCE, isNoBalance);
        bundle.putBoolean(IntentExtra.ON_CHAIN, isOnChain);
        bundle.putBoolean(IntentExtra.IS_JOINED, isJoined);

        if (currentTabFragment != null) {
            currentTabFragment.hideView();
        }

        switch (view.getId()) {
            case R.id.tv_notes:
                // note
                spinnerItems = new int[] {};
                currentTabFragment = new NotesTabFragment();
                currentTab = CommunityTabFragment.TAB_NOTES;
                break;
            case R.id.tv_market:
                // market
                spinnerItems = new int[] {R.string.community_view_all,
                        R.string.community_view_sell,
                        R.string.community_view_airdrop,
                        R.string.community_view_announcement};
                currentTabFragment = new MarketTabFragment();
                currentTab = CommunityTabFragment.TAB_MARKET;
                break;
            case R.id.tv_chain:
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
        disposables.add(communityViewModel.getMembersStatistics(chainID)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(statistics ->
                    binding.toolbarInclude.tvSubtitle.setText(getString(R.string.community_users_stats,
                            statistics.getMembers())))
        );

        disposables.add(communityViewModel.observerCurrentMember(chainID)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(member -> {
                    isJoined = member.isJoined();
                    isOnChain = member.onChain();
                    isNoBalance = member.noBalance();
                    if (currentTabFragment != null) {
                        currentTabFragment.handleMember(member);
                        int color = !isJoined ? R.color.gray_light : R.color.primary;
                        binding.fabButton.setMainFabClosedBackgroundColor(getResources().getColor(color));
                    }
                    binding.flJoin.setVisibility(member.isJoined() ? View.GONE : View.VISIBLE);
                }, it -> {}));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_notes:
                onTabClick(v, CommunityTabFragment.TAB_NOTES);
                break;
            case R.id.tv_market:
                onTabClick(v, CommunityTabFragment.TAB_MARKET);
                break;
            case R.id.tv_chain:
                onTabClick(v, CommunityTabFragment.TAB_CHAIN);
                break;
            case R.id.tv_join:
                communityViewModel.joinCommunity(chainID);
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
            this.selectedView.setTextColor(getResources().getColor(R.color.gray_dark));
        }
        TextView selectedView = (TextView) v;
        selectedView.setBackgroundResource(R.drawable.yellow_rect_round_border_small_radius);
        selectedView.setTextColor(getResources().getColor(R.color.color_yellow));
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
}
