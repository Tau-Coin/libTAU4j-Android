package io.taucoin.torrent.publishing.ui.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.UserAndFriend;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.databinding.ActivityEscrowServiceBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.main.MainActivity;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * Escrow Service页面
 */
public class EscrowServiceActivity extends BaseActivity implements View.OnClickListener {

    private ActivityEscrowServiceBinding binding;

    private CommunityViewModel viewModel;
    private UserViewModel userViewModel;
    private String chainID;
    private UserAndFriend user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(CommunityViewModel.class);
        userViewModel = provider.get(UserViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_escrow_service);
        binding.setListener(this);
        initParameter();
        initLayout();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if(getIntent() != null){
            chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.escrow_service);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        viewModel.getLargestCoinHolder().observe(this, user -> {
            this.user = user;
            if (user.isDiscovered()) {
                userViewModel.addFriend(user.publicKey);
            } else {
                openChatActivity();
            }
        });

        userViewModel.getAddFriendResult().observe(this, result -> {
            if (result.isSuccess()) {
                ToastUtils.showShortToast(result.getMsg());
            } else {
                openChatActivity();
            }
        });
    }

    /**
     * 打开聊天页面
     */
    private void openChatActivity() {
        if (null == user) {
            return;
        }
        onBackPressed();
        User userParent = user;
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.CHAIN_ID, user.publicKey);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(IntentExtra.TYPE, 1);
        intent.putExtra(IntentExtra.BEAN, userParent);
        ActivityUtil.startActivity(intent, this, MainActivity.class);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_escrow_now) {
            viewModel.getCommunityLargestCoinHolder(chainID);
        }
    }
}
