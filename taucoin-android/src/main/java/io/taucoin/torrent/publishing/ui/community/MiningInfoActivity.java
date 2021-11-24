package io.taucoin.torrent.publishing.ui.community;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.databinding.ActivityMiningInfoBinding;
import io.taucoin.torrent.publishing.databinding.ViewHelpDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;

/**
 * 挖矿信息页面
 */
public class MiningInfoActivity extends BaseActivity implements View.OnClickListener {
    private static final int CHOOSE_REQUEST_CODE = 0x01;
    private ActivityMiningInfoBinding binding;
    private CommunityViewModel viewModel;
    private CommonDialog helpDialog;
    private CommonDialog closeDialog;
    private String chainID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_mining_info);
        binding.setListener(this);
        initLayout();
        observeJoinedList();
    }

    /**
     * 观察加入的社区列表
     */
    private void observeJoinedList() {
        viewModel.getJoinedCommunityList();
        viewModel.getJoinedList().observe(this, communities -> {
            if (communities != null && communities.size() > 0) {
                chainID = communities.get(0).chainID;
                loadMiningInfo();
            }
        });
    }

    /**
     * 初始化布局
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.community_mining_info);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        binding.itemSwitch.setOnTouchListener((v, event) -> {
            if (binding.itemSwitch.isChecked()) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    showAutoRenewalClosedDialog();
                }
                return true;
            }
            return false;
        });
        binding.itemSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ToastUtils.showShortToast("Auto-Renewal::" + isChecked);
        });
    }

    private void loadMiningInfo() {
        String communityName = ChainIDUtil.getName(chainID);
        binding.tvCommunityName.setText(communityName);
        binding.itemMiningIncome.setRightText(FmtMicrometer.fmtBalance(0));
        binding.itemMiningPower.setRightText(FmtMicrometer.fmtLong(0));
        binding.itemLastBlock.setRightText(FmtMicrometer.fmtLong(0));
        binding.itemDifficulty.setRightText(FmtMicrometer.fmtLong(0));
        binding.itemPerishableDate.setRightText(DateUtil.format(DateUtil.getMillisTime(), DateUtil.pattern6));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_community_name:
            case R.id.iv_arrow:
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                ActivityUtil.startActivityForResult(intent, this, CommunityChooseActivity.class,
                        CHOOSE_REQUEST_CODE);
                break;
            case R.id.iv_auto_renewal:
                showAutoRenewalHelpDialog();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == CHOOSE_REQUEST_CODE) {
            if (data != null) {
                chainID = data.getStringExtra(IntentExtra.CHAIN_ID);
                loadMiningInfo();
            }
        }
    }

    /**
     * 显示Auto-Renewal帮助对话框
     */
    private void showAutoRenewalHelpDialog() {
        ViewHelpDialogBinding binding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.view_help_dialog, null, false);
        binding.tvTitle.setText(R.string.community_auto_renewal_help_title);
        binding.tvContent.setText(R.string.community_auto_renewal_help_content);
        helpDialog = new CommonDialog.Builder(this)
                .setContentView(binding.getRoot())
                .setCanceledOnTouchOutside(false)
                .create();
        binding.ivClose.setOnClickListener(v -> {
            helpDialog.closeDialog();
        });
        helpDialog.show();
    }

    /**
     * 显示关闭Auto-Renewal对话框
     */
    private void showAutoRenewalClosedDialog() {
        ViewHelpDialogBinding binding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.view_help_dialog, null, false);
        binding.ivClose.setVisibility(View.GONE);
        binding.tvContent.setVisibility(View.GONE);
        binding.tvTitle.setTextColor(getResources().getColor(R.color.color_black));
        binding.tvTitle.setText(R.string.community_auto_renewal_turn_off_tip);

        CommonDialog.Builder builder = new CommonDialog.Builder(this)
                .setContentView(binding.getRoot())
                .setCanceledOnTouchOutside(false)
                .setHorizontal()
                .setNegativeButton(R.string.common_turn_off, (dialog, which) -> {
                    dialog.dismiss();
                    MiningInfoActivity.this.binding.itemSwitch.setChecked(false);
                }).setPositiveButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        closeDialog = builder.create();
        closeDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (helpDialog != null) {
            helpDialog.closeDialog();
        }
        if (closeDialog != null ) {
            closeDialog.closeDialog();
        }
    }
}