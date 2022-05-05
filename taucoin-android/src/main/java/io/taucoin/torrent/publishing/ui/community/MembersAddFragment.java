package io.taucoin.torrent.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.math.BigInteger;
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
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.data.FriendAndUser;
import io.taucoin.torrent.publishing.core.model.data.UserAndFriend;
import io.taucoin.torrent.publishing.core.model.data.message.TxType;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.databinding.FragmentMembersAddBinding;
import io.taucoin.torrent.publishing.databinding.ViewConfirmDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseFragment;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.main.MainActivity;
import io.taucoin.torrent.publishing.ui.transaction.TxViewModel;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 社区成员添加页面
 */
public class MembersAddFragment extends BaseFragment {

    public static final int PAGE_COMMUNITY_CREATION = 0x01;
    public static final int PAGE_ADD_MEMBERS = 0x02;
    private FragmentActivity activity;
    private FragmentMembersAddBinding binding;
    private TxViewModel viewModel;
    private UserViewModel userViewModel;
    private CommonDialog confirmDialog;
    private MembersAddAdapter adapter;
    private String chainID;
    private String medianFee;
    private long airdropCoin;
    private int page;
    private List<User> friends;

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
            friends = getArguments().getParcelableArrayList(IntentExtra.BEAN);
            airdropCoin = getArguments().getLong(IntentExtra.AIRDROP_COIN,
                    Constants.AIRDROP_COIN.longValue());
            page = getArguments().getInt(IntentExtra.TYPE, PAGE_ADD_MEMBERS);
            if (null == friends) {
                friends = new ArrayList<>();
            }

        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        long txFree = 0L;
        if (StringUtil.isNotEmpty(chainID)) {
            txFree = viewModel.getTxFee(chainID, TxType.WIRING_TX);
        }
        medianFee = FmtMicrometer.fmtFeeValue(txFree);
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
            userViewModel.loadUsersList(0, true, null);
            userViewModel.getUserList().observe(getViewLifecycleOwner(), friends -> {
                if (friends != null) {
                    this.friends.addAll(friends);
                    adapter.submitFriendList(this.friends, false);
                    calculateTotalCoins();
                }
            });
        } else {
            adapter.submitFriendList(friends, true);
            calculateTotalCoins();
        }
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
        MembersConfirmAdapter adapter = new MembersConfirmAdapter(getSelectedMap());
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        binding.recyclerList.setLayoutManager(layoutManager);
        binding.recyclerList.setItemAnimator(null);
        binding.recyclerList.setAdapter(adapter);
        adapter.submitList(this.adapter.getSelectedList());

        calculateConfirmCoins(binding);

        confirmDialog = new CommonDialog.Builder(activity)
                .setContentView(binding.getRoot())
                .setButtonWidth(R.dimen.widget_size_240)
                .setPositiveButton(R.string.common_confirm, (dialog, which) -> {
                    showProgressDialog();
                    viewModel.addMembers(chainID, getSelectedMap(), medianFee);
                }).create();
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
    }
}
