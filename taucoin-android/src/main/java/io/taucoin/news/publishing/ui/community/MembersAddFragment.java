package io.taucoin.news.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import cn.bingoogolapple.refreshlayout.BGARefreshLayout;
import cn.bingoogolapple.refreshlayout.BGAStickinessRefreshViewHolder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.Constants;
import io.taucoin.news.publishing.core.model.data.message.TxType;
import io.taucoin.news.publishing.core.storage.sqlite.entity.User;
import io.taucoin.news.publishing.core.utils.ActivityUtil;
import io.taucoin.news.publishing.core.utils.ChainIDUtil;
import io.taucoin.news.publishing.core.utils.FmtMicrometer;
import io.taucoin.news.publishing.core.utils.ObservableUtil;
import io.taucoin.news.publishing.core.utils.StringUtil;
import io.taucoin.news.publishing.core.utils.ToastUtils;
import io.taucoin.news.publishing.core.utils.bus.Members;
import io.taucoin.news.publishing.core.utils.bus.RxBus2;
import io.taucoin.news.publishing.databinding.FragmentMembersAddBinding;
import io.taucoin.news.publishing.databinding.ViewConfirmDialogBinding;
import io.taucoin.news.publishing.ui.BaseFragment;
import io.taucoin.news.publishing.ui.constant.IntentExtra;
import io.taucoin.news.publishing.ui.constant.Page;
import io.taucoin.news.publishing.ui.customviews.ConfirmDialog;
import io.taucoin.news.publishing.ui.main.MainActivity;
import io.taucoin.news.publishing.ui.transaction.TxViewModel;
import io.taucoin.news.publishing.ui.user.UserViewModel;

/**
 * 社区成员添加页面
 */
public class MembersAddFragment extends BaseFragment implements BGARefreshLayout.BGARefreshLayoutDelegate {

    public static final int PAGE_COMMUNITY_CREATION = 0x01;
    public static final int PAGE_ADD_MEMBERS = 0x02;
    private FragmentActivity activity;
    private FragmentMembersAddBinding binding;
    private TxViewModel viewModel;
    private UserViewModel userViewModel;
    private CommunityViewModel communityViewModel;
    private ConfirmDialog confirmDialog;
    private MembersAddAdapter adapter;
    private String chainID;
    private String medianFee;
    private long airdropCoin;
    private int page;
    private final List<User> friends = new ArrayList<>();
    private final CompositeDisposable disposables = new CompositeDisposable();
    private boolean dataChanged = false;
    private int currentPos = 0;
    private boolean isLoadMore = false;
    private long paymentBalance = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_members_add, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activity = getActivity();
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(TxViewModel.class);
        userViewModel = provider.get(UserViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        initParameter();
        initLayout();
        observeAirdropState();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if (getArguments() != null) {
            chainID = getArguments().getString(IntentExtra.CHAIN_ID);
            airdropCoin = getArguments().getLong(IntentExtra.AIRDROP_COIN,
                    Constants.AIRDROP_COIN.longValue());
            page = getArguments().getInt(IntentExtra.TYPE, PAGE_ADD_MEMBERS);
        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        adapter = new MembersAddAdapter(airdropCoin);
        adapter.setListener(new MembersAddAdapter.ClickListener() {
            @Override
            public void onSelectClicked() {
                calculateTotalCoins();
            }

            @Override
            public void onTextChanged() {
                calculateTotalCoins();
            }
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        binding.recyclerList.setLayoutManager(layoutManager);
        binding.recyclerList.setItemAnimator(null);
        binding.recyclerList.setAdapter(adapter);

        if (page == PAGE_COMMUNITY_CREATION) {
            userViewModel.getUserList().observe(getViewLifecycleOwner(), friends -> {
                if (friends != null) {
                    if (currentPos == 0) {
                        this.friends.clear();
                    }
                    this.friends.addAll(friends);
                    adapter.submitFriendList(this.friends, false);
                    int size = friends.size();
                    isLoadMore = size != 0 && size % Page.PAGE_SIZE == 0;
                    binding.refreshLayout.endLoadingMore();
                    calculateTotalCoins();
                }
            });
        }
        initRefreshLayout();
    }

    private void initRefreshLayout() {
        binding.refreshLayout.setDelegate(this);
        BGAStickinessRefreshViewHolder refreshViewHolder = new BGAStickinessRefreshViewHolder(activity, true);
        refreshViewHolder.setRotateImage(R.mipmap.ic_launcher_foreground);
        refreshViewHolder.setStickinessColor(R.color.color_yellow);

        refreshViewHolder.setLoadingMoreText(getString(R.string.common_loading));
        binding.refreshLayout.setPullDownRefreshEnable(false);

        binding.refreshLayout.setRefreshViewHolder(refreshViewHolder);
    }

    private void calculateTotalCoins() {
        if (adapter != null) {
            Map<String, String> map = getSelectedMap();
            int selectedFriends = map.size();
            binding.tvSelectedFriends.setText(getString(R.string.community_selected_friends, selectedFriends));

            double totalCoins = 0d;
            Collection<String> values = map.values();
            for (String value : values) {
                totalCoins += StringUtil.getDoubleString(value);
            }
            binding.tvAirdropCoins.setText(getString(R.string.community_total_coins,
                    FmtMicrometer.formatTwoDecimal(totalCoins)));
        }
    }

    Map<String, String> getSelectedMap() {
        return adapter.getSelectedMap();
    }

    /**
     * 观察添加社区的状态
     */
    private void observeAirdropState() {
        viewModel.getAirdropState().observe(getViewLifecycleOwner(), state -> {
            if (state.isSuccess()) {
                closeProgressDialog();
                if (confirmDialog != null) {
                    confirmDialog.closeDialog();
                }
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(IntentExtra.TYPE, 0);
                ActivityUtil.startActivity(intent, this, MainActivity.class);
                ToastUtils.showShortToast(R.string.contacts_add_successfully);
            } else {
                closeProgressDialog();
                ToastUtils.showShortToast(state.getMsg());
            }
        });
    }

    /**
     * 显示添加新社区成功后的对话框
     */
    void showConfirmDialog() {
        if (getSelectedMap().size() == 0) {
            ToastUtils.showShortToast(R.string.community_added_members_empty);
            return;
        }
        Collection<String> values = getSelectedMap().values();
        for (String value : values) {
            long coin = FmtMicrometer.fmtTxLongValue(value);
            if (coin == 0) {
                ToastUtils.showLongToast(R.string.error_airdrop_coins_empty);
                return;
            }
        }
        ViewConfirmDialogBinding binding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.view_confirm_dialog, null, false);
        binding.tvAvailableBalance.setText(getString(R.string.tx_payment_balance,
                FmtMicrometer.fmtLong(paymentBalance),
                ChainIDUtil.getCoinName(chainID)));
        MembersConfirmAdapter adapter = new MembersConfirmAdapter(getSelectedMap());
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        binding.recyclerList.setLayoutManager(layoutManager);
        binding.recyclerList.setItemAnimator(null);
        binding.recyclerList.setAdapter(adapter);
        adapter.submitList(this.adapter.getSelectedList());

        calculateConfirmCoins(binding);

        binding.tvConfirm.setOnClickListener(view -> {
            showProgressDialog();
            viewModel.addMembers(chainID, getSelectedMap(), medianFee);
        });

        confirmDialog = new ConfirmDialog.Builder(activity)
                .setContentView(binding.getRoot())
                .setWarpView(binding.recyclerList)
                .create();
        confirmDialog.show();
    }

    private void calculateConfirmCoins(ViewConfirmDialogBinding binding) {
        if (adapter != null) {
            Map<String, String> map = getSelectedMap();
            int selectedFriends = map.size();
            binding.tvAirdropPeers.setText(getString(R.string.community_airdrop_peers, selectedFriends));
            double totalCoins = 0d;
            Collection<String> values = map.values();
            for (String value : values) {
                totalCoins += StringUtil.getDoubleString(value);
            }
            binding.tvAirdropCoins.setText(getString(R.string.community_airdrop_coins,
                    FmtMicrometer.formatTwoDecimal(totalCoins)));
            double totalFree = selectedFriends * Double.parseDouble(medianFee);
            binding.tvAirdropFree.setText(getString(R.string.community_airdrop_fee,
                    FmtMicrometer.formatTwoDecimal(totalFree)));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeProgressDialog();
        if (confirmDialog != null) {
            confirmDialog.closeDialog();
        }
        RxBus2.getInstance().removeStickyEvent(Members.class);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (page == PAGE_COMMUNITY_CREATION) {
            loadData(0);
            disposables.add(userViewModel.observeUsersChanged()
                    .subscribeOn(Schedulers.io())
                    .subscribe(o -> dataChanged = true));

            disposables.add(ObservableUtil.interval(500)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(o -> {
                        if (dataChanged) {
                            loadData(0);
                            dataChanged = false;
                        }
                    }));
        } else {
            disposables.add(RxBus2.getInstance().registerStickyEvent(Members.class, members -> {
                this.friends.addAll(members.getList());
                adapter.submitFriendList(friends, true);
                calculateTotalCoins();
            }));
        }

        disposables.add(viewModel.observeAverageTxFee(chainID, TxType.WIRING_TX)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fee -> {
                    this.medianFee = FmtMicrometer.fmtFeeValue(fee);
                }));

        disposables.add(communityViewModel.observerCurrentMember(chainID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(member -> {
                    if (member != null) {
                        paymentBalance = member.getPaymentBalance() >= 0 ? member.getPaymentBalance() : 0;
                    }
                }, it -> {}));
    }

    @Override
    public void onStop() {
        super.onStop();
        disposables.clear();
    }

    private int getItemCount() {
        int count = 0;
        if (adapter != null) {
            count = adapter.getItemCount();
        }
        return count;
    }

    protected void loadData(int pos) {
        this.currentPos = pos;
        userViewModel.loadUsersList(pos, getItemCount());
    }

    @Override
    public void onBGARefreshLayoutBeginRefreshing(BGARefreshLayout refreshLayout) {
    }

    @Override
    public boolean onBGARefreshLayoutBeginLoadingMore(BGARefreshLayout refreshLayout) {
        if (isLoadMore) {
            loadData(getItemCount());
            return true;
        } else {
            refreshLayout.endLoadingMore();
            return false;
        }
    }
}
