package io.taucoin.tauapp.publishing.ui.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.tauapp.publishing.MainApplication;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.model.data.UserAndFriend;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.tauapp.publishing.core.utils.ActivityUtil;
import io.taucoin.tauapp.publishing.core.utils.BitmapUtil;
import io.taucoin.tauapp.publishing.core.utils.CopyManager;
import io.taucoin.tauapp.publishing.core.utils.DrawablesUtil;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.core.utils.ToastUtils;
import io.taucoin.tauapp.publishing.core.utils.UsersUtil;
import io.taucoin.tauapp.publishing.databinding.ActivityUserDetailBinding;
import io.taucoin.tauapp.publishing.ui.BaseActivity;
import io.taucoin.tauapp.publishing.ui.community.CommunityViewModel;
import io.taucoin.tauapp.publishing.ui.constant.IntentExtra;
import io.taucoin.tauapp.publishing.ui.main.MainActivity;

/**
 * 用户详情
 */
public class UserDetailActivity extends BaseActivity implements View.OnClickListener,
        UserCommunityListAdapter.ClickListener {
    public static final int TYPE_COMMUNITY = 0x01;
    public static final int TYPE_FRIEND_LIST = 0x02;
    public static final int TYPE_CHAT_PAGE = 0x03;
    private ActivityUserDetailBinding binding;
    private UserViewModel userViewModel;
    private CommunityViewModel communityViewModel;
    private UserCommunityListAdapter adapter;
    private String publicKey;
    private UserAndFriend user;
    private int type;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        userViewModel.observeNeedStartDaemon();
        communityViewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_user_detail);
        binding.setListener(this);
        initParam();
        initView();
        initData();
        userViewModel.requestFriendInfo(publicKey);
        userViewModel.focusFriend(publicKey);
    }

    /**
     * 初始化参数
     */
    private void initParam() {
        if (getIntent() != null) {
            type = getIntent().getIntExtra(IntentExtra.TYPE, TYPE_COMMUNITY);
            publicKey = getIntent().getStringExtra(IntentExtra.PUBLIC_KEY);
        }
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle("");
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new UserCommunityListAdapter(this);
        //        /*
//         * A RecyclerView by default creates another copy of the ViewHolder in order to
//         * fade the views into each other. This causes the problem because the old ViewHolder gets
//         * the payload but then the new one doesn't. So needs to explicitly tell it to reuse the old one.
//         */
        DefaultItemAnimator animator = new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
                return true;
            }
        };
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerList.setLayoutManager(layoutManager);
        binding.recyclerList.setItemAnimator(animator);
        binding.recyclerList.setAdapter(adapter);

        // 获取用户详情数据
        userViewModel.getUserDetail(publicKey);

        boolean isMine = StringUtil.isEquals(publicKey, MainApplication.getInstance().getPublicKey());
        boolean isShowBan = (type == TYPE_CHAT_PAGE || type == TYPE_FRIEND_LIST) && !isMine;
        binding.leftView.tvBlacklist.setVisibility(isShowBan ? View.VISIBLE : View.GONE);
        binding.leftView.tvBlacklist.setOnClickListener(this);
    }

    private void initData() {
        userViewModel.getUserDetail().observe(this, this::showUserInfo);

        userViewModel.getAddFriendResult().observe(this, result -> {
            closeProgressDialog();
            if (result.isSuccess()) {
                ToastUtils.showShortToast(result.getMsg());
            } else {
                binding.tvAddToContact.setVisibility(View.GONE);
                binding.tvStartChat.setVisibility(View.VISIBLE);
            }
        });

        userViewModel.getEditBlacklistResult().observe(this, result -> {
            if (result.isSuccess()) {
                ToastUtils.showShortToast(R.string.blacklist_successfully);
                onBackPressed();
            } else {
                ToastUtils.showShortToast(R.string.blacklist_failed);
            }
        });

        userViewModel.getEditRemarkResult().observe(this, result -> {
            userViewModel.getUserDetail(publicKey);
        });
        communityViewModel.getJoinedCommunityList(publicKey);
        communityViewModel.getJoinedCommunity().observe(this, members -> {
            if (adapter != null) {
                if (members != null) {
                    adapter.submitList(members);
                    if (members.size() > 0) {
                        binding.llMutualCommunities.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
    }

    private void showUserInfo(UserAndFriend userInfo) {
        boolean isMine = StringUtil.isEquals(publicKey, MainApplication.getInstance().getPublicKey());
        binding.tvAddToContact.setVisibility(!isMine && userInfo.isDiscovered() ? View.VISIBLE : View.GONE);
        boolean isShowChat = !isMine && !userInfo.isDiscovered() && type != TYPE_CHAT_PAGE;
        binding.tvStartChat.setVisibility(isShowChat ? View.VISIBLE : View.GONE);
        if (!isMine) {
            DrawablesUtil.setEndDrawable(binding.tvShowName, R.mipmap.icon_edit,
                    getResources().getDimension(R.dimen.widget_size_20));
            binding.tvShowName.setOnClickListener(this);
        }
        this.user = userInfo;
        if (!isMine) {
            String nickName = UsersUtil.getCurrentUserName(user);
            binding.tvNickName.setText(getString(R.string.user_nick_name, nickName));
        }
        binding.tvNickName.setVisibility(isMine ? View.GONE : View.VISIBLE);
        String showName = UsersUtil.getShowName(user);
        binding.tvShowName.setText(showName);
        binding.leftView.ivHeadPic.setImageBitmap(UsersUtil.getHeadPic(userInfo));
        String midHideName = UsersUtil.getMidHideName(user.publicKey);
        binding.tvPublicKey.setText(getString(R.string.user_public_key, midHideName));
        DrawablesUtil.setEndDrawable(binding.tvPublicKey, R.mipmap.icon_copy_text,
                getResources().getDimension(R.dimen.widget_size_16));
        binding.tvPublicKey.setOnClickListener(v -> {
            copyPublicKey(user.publicKey);
        });

        boolean isHaveProfile = StringUtil.isNotEmpty(user.profile);
        binding.llPersonalProfile.setVisibility(isHaveProfile ? View.VISIBLE : View.GONE);
        binding.tvPersonalProfile.setText(user.profile);
    }

    /**
     * 复制公钥
     * @param publicKey public Key
     */
    private void copyPublicKey(String publicKey) {
        CopyManager.copyText(publicKey);
        ToastUtils.showShortToast(R.string.copy_public_key);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_add_to_contact:
                showProgressDialog();
                userViewModel.addFriend(publicKey);
                break;
            case R.id.tv_start_chat:
                onBackPressed();
                openChatActivity(user.publicKey);
                break;
            case R.id.tv_show_name:
                userViewModel.showRemarkDialog(this, publicKey);
                break;
            case R.id.tv_blacklist:
                userViewModel.setUserBlacklist(publicKey, true);
                break;
            case R.id.iv_public_key_copy:
                CopyManager.copyText(publicKey);
                ToastUtils.showShortToast(R.string.copy_successfully);
                break;
        }
    }

    @Override
    public void onItemClicked(@NonNull Member member) {
        openCommunityActivity(member.chainID);
    }

    /**
     * 打开社区页面
     * @param chainID
     */
    private void openCommunityActivity(String chainID){
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.CHAIN_ID, chainID);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(IntentExtra.TYPE, 0);
        ActivityUtil.startActivity(intent, this, MainActivity.class);
    }

    /**
     * 打开聊天页面
     * @param chainID
     */
    private void openChatActivity(String chainID) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.CHAIN_ID, chainID);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(IntentExtra.TYPE, 1);
        intent.putExtra(IntentExtra.BEAN, this.user);
        ActivityUtil.startActivity(intent, this, MainActivity.class);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BitmapUtil.recycleImageView(binding.leftView.ivHeadPic);
    }
}