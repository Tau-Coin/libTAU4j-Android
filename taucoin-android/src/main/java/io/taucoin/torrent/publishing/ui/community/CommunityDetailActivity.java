package io.taucoin.torrent.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.MemberAndFriend;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.databinding.ActivityCommunityDetailBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.friends.FriendsActivity;
import io.taucoin.torrent.publishing.ui.qrcode.CommunityQRCodeActivity;
import io.taucoin.torrent.publishing.ui.user.UserDetailActivity;

/**
 * 社区详情页面
 */
public class CommunityDetailActivity extends BaseActivity implements MemberListAdapter.ClickListener {

    private static final Logger logger = LoggerFactory.getLogger("CommunityDetailActivity");
    private ActivityCommunityDetailBinding binding;
    private CommunityViewModel communityViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MemberListAdapter adapter;
    private String chainID;
    private CommonDialog blacklistDialog;
    private boolean isReadOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        communityViewModel.observeNeedStartDaemon();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_community_detail);
        initParameter();
        initLayout();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if(getIntent() != null){
            chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
            isReadOnly = getIntent().getBooleanExtra(IntentExtra.READ_ONLY, true);
        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        if(StringUtil.isNotEmpty(chainID)){
            String communityName = ChainIDUtil.getName(chainID);
            binding.toolbarInclude.tvGroupName.setText(Html.fromHtml(communityName));
            binding.toolbarInclude.tvUsersStats.setVisibility(View.GONE);
        }
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        if (isReadOnly) {
            binding.itemAddMember.setVisibility(View.GONE);
        }

        adapter = new MemberListAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setItemAnimator(null);
        binding.recyclerView.setAdapter(adapter);

        communityViewModel.observerCommunityMembers(chainID).observe(this, members -> {
            adapter.submitList(members);
            logger.debug("adapter.size::{}, newSize::{}", adapter.getItemCount(), members.size());
        });

    }

    /**
     * 左侧抽屉布局点击事件
     */
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.item_chain_status:
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                ActivityUtil.startActivity(intent, this, ChainStatusActivity.class);
                break;
            case R.id.item_add_member:
                intent = new Intent();
                intent.putExtra(IntentExtra.TYPE, FriendsActivity.PAGE_ADD_MEMBERS);
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                ActivityUtil.startActivity(intent, this, FriendsActivity.class);
                break;
            case R.id.item_qr_code:
                intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                ActivityUtil.startActivity(intent, this, CommunityQRCodeActivity.class);
                break;
            case R.id.item_blacklist:
                blacklistDialog = communityViewModel.showBanCommunityTipsDialog(this, chainID);
                break;
        }
    }

    @Override
    public void onItemClicked(MemberAndFriend member) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.PUBLIC_KEY, member.publicKey);
        ActivityUtil.startActivity(intent, this, UserDetailActivity.class);
    }

    @Override
    public void onShareClicked(MemberAndFriend member) {
    }

    @Override
    public void onStart() {
        super.onStart();
        communityViewModel.getSetBlacklistState().observe(this, isSuccess -> {
            if (isSuccess) {
                ToastUtils.showShortToast(R.string.blacklist_successfully);
                this.setResult(RESULT_OK);
                this.finish();
            } else {
                ToastUtils.showShortToast(R.string.blacklist_failed);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (blacklistDialog != null) {
            blacklistDialog.closeDialog();
        }
    }
}