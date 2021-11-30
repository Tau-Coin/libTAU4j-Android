package io.taucoin.torrent.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.FragmentMembersAddBinding;
import io.taucoin.torrent.publishing.databinding.ViewConfirmDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseFragment;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.main.MainActivity;
import io.taucoin.torrent.publishing.ui.transaction.TxViewModel;

/**
 * 社区成员添加页面
 */
public class MembersAddFragment extends BaseFragment implements CompoundButton.OnCheckedChangeListener {

    private FragmentActivity activity;
    private FragmentMembersAddBinding binding;
    private TxViewModel viewModel;
    private CommonDialog confirmDialog;
    private MembersAddAdapter adapter;
    private String chainID;
    private String medianFee;
    private long airdropCoin;
    private List<User> friends = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_members_add, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = getActivity();
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(TxViewModel.class);
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
        }
    }

    void updateData(List<User> friends) {
        this.friends = friends;
        adapter.submitFriendList(friends);
        calculateTotalCoins();
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        long txFree = 0L;
        if (StringUtil.isNotEmpty(chainID)) {
            txFree = viewModel.getTxFee(chainID);
        }
        medianFee = FmtMicrometer.fmtFeeValue(txFree);
        binding.rbUnified.setChecked(true);

        binding.etAirdropCoins.setText(FmtMicrometer.fmtFormat(String.valueOf(airdropCoin)));
        boolean isUnified = binding.rbUnified.isChecked();
        adapter = new MembersAddAdapter(isUnified, airdropCoin);
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

        binding.etAirdropCoins.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateTotalCoins();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        binding.recyclerList.setLayoutManager(layoutManager);
        binding.recyclerList.setItemAnimator(null);
        binding.recyclerList.setAdapter(adapter);

        binding.rbUnified.setOnCheckedChangeListener(this);
        binding.rbCustom.setOnCheckedChangeListener(this);

        adapter.submitFriendList(friends);
        calculateTotalCoins();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (!isChecked) {
            return;
        }
        boolean isUnified = buttonView.getId() == R.id.rb_unified;
        if (isUnified) {
            binding.rbCustom.setChecked(false);
        } else {
            binding.rbUnified.setChecked(false);
        }
        if (adapter != null) {
            adapter.updateUnified(isUnified);
            adapter.notifyDataSetChanged();
        }
        calculateTotalCoins();
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
        Map<String, String> selectedMap = adapter.getSelectedMap();
        Set<String> keys = selectedMap.keySet();
        if (binding.rbUnified.isChecked()) {
            Map<String, String> newSelectedMap = new ArrayMap<>();
            for (String key : keys) {
                newSelectedMap.put(key, ViewUtils.getText(binding.etAirdropCoins));
            }
            return newSelectedMap;
        } else {
            return selectedMap;
        }
    }

    /**
     * 观察添加社区的状态
     */
    private void observeAirdropState() {
        viewModel.getAirdropState().observe(this, state -> {
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
                    viewModel.airdropToFriends(chainID, getSelectedMap(), medianFee);
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
            binding.tvAirdropFree.setText(getString(R.string.community_airdrop_free,
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
