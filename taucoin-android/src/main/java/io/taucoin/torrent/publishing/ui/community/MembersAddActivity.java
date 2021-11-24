package io.taucoin.torrent.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivityMembersAddBinding;
import io.taucoin.torrent.publishing.databinding.ViewConfirmDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.main.MainActivity;
import io.taucoin.torrent.publishing.ui.transaction.TxViewModel;

/**
 * 群组成员添加页面
 */
public class MembersAddActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener {
    private ActivityMembersAddBinding binding;
    private TxViewModel viewModel;
    private CommonDialog confirmDialog;
    private MembersAddAdapter adapter;
    private String chainID;
    private String medianFee;
    private CompositeDisposable disposables = new CompositeDisposable();
    private List<User> friends = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(TxViewModel.class);
        viewModel.observeNeedStartDaemon();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_members_add);
        initParameter();
        initLayout();
        observeAirdropState();
    }

    private void initParameter() {
        if (getIntent() != null) {
            chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
            friends = getIntent().getParcelableArrayListExtra(IntentExtra.BEAN);
        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        long txFree = viewModel.getTxFee(chainID);
        medianFee = FmtMicrometer.fmtFeeValue(txFree);
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.community_added_members);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());
        
        binding.rbUnified.setChecked(true);

        binding.etAirdropCoins.setText(FmtMicrometer.fmtFormat(Constants.AIRDROP_COIN.toString()));
        boolean isUnified = binding.rbUnified.isChecked();
        adapter = new MembersAddAdapter(isUnified);
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
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerList.setLayoutManager(layoutManager);
        binding.recyclerList.setItemAnimator(null);
        binding.recyclerList.setAdapter(adapter);

        binding.rbUnified.setOnCheckedChangeListener(this);
        binding.rbCustom.setOnCheckedChangeListener(this);
        
        adapter.submitFriendList(friends);
        calculateTotalCoins();
    }

    @Override
    protected void onStart() {
        super.onStart();
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

    public void calculateTotalCoins() {
        if (adapter != null) {
            Map<String, String> map = adapter.getSelectedMap();
            int selectedFriends = map.size();
            binding.tvSelectedFriends.setText(getString(R.string.community_selected_friends, selectedFriends));

            boolean isUnified = binding.rbUnified.isChecked();
            double totalCoins = 0d;
            if (isUnified) {
                totalCoins = selectedFriends * ViewUtils.getDoubleText(binding.etAirdropCoins);
            } else {
                Collection<String> values = map.values();
                for (String value : values) {
                    totalCoins += StringUtil.getDoubleString(value);
                }
            }
            binding.tvAirdropCoins.setText(getString(R.string.community_total_coins,
                    FmtMicrometer.formatTwoDecimal(totalCoins)));
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
    private void showConfirmDialog() {
        ViewConfirmDialogBinding binding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.view_confirm_dialog, null, false);
        MembersConfirmAdapter adapter = new MembersConfirmAdapter(this.adapter.getSelectedMap());
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerList.setLayoutManager(layoutManager);
        binding.recyclerList.setItemAnimator(null);
        binding.recyclerList.setAdapter(adapter);
        adapter.submitList(this.adapter.getSelectedList());

        calculateConfirmCoins(binding);

        confirmDialog = new CommonDialog.Builder(this)
                .setContentView(binding.getRoot())
                .setButtonWidth(R.dimen.widget_size_240)
                .setPositiveButton(R.string.common_confirm, (dialog, which) -> {
                    showProgressDialog();
                    viewModel.airdropToFriends(chainID, this.adapter.getSelectedMap(), medianFee);
                }).create();
        confirmDialog.show();

    }

    private void calculateConfirmCoins(ViewConfirmDialogBinding binding) {
        if (adapter != null) {
            Map<String, String> map = adapter.getSelectedMap();
            int selectedFriends = map.size();
            binding.tvAirdropPeers.setText(getString(R.string.community_airdrop_peers, selectedFriends));
            boolean isUnified = this.binding.rbUnified.isChecked();
            double totalCoins = 0d;
            if (isUnified) {
                totalCoins = selectedFriends * ViewUtils.getDoubleText(this.binding.etAirdropCoins);
            } else {
                Collection<String> values = map.values();
                for (String value : values) {
                    totalCoins += StringUtil.getDoubleString(value);
                }
            }
            binding.tvAirdropCoins.setText(getString(R.string.community_airdrop_coins,
                    FmtMicrometer.formatTwoDecimal(totalCoins)));
            double totalFree = selectedFriends * Double.parseDouble(medianFee);
            binding.tvAirdropFree.setText(getString(R.string.community_airdrop_free,
                    FmtMicrometer.formatTwoDecimal(totalFree)));
        }
    }

    /**
     *  创建右上角Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_done, menu);
        return true;
    }

    /**
     * 右上角Menu选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 添加新社区处理事件
        if (item.getItemId() == R.id.menu_done) {
            // 进入社区页面
            showConfirmDialog();
        }
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeProgressDialog();
        if (confirmDialog != null) {
            confirmDialog.closeDialog();
        }
    }
}