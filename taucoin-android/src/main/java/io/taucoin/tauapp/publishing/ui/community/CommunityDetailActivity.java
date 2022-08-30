package io.taucoin.tauapp.publishing.ui.community;

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
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.model.data.MemberAndFriend;
import io.taucoin.tauapp.publishing.core.model.data.message.AirdropStatus;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.tauapp.publishing.core.utils.ActivityUtil;
import io.taucoin.tauapp.publishing.core.utils.ChainIDUtil;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.core.utils.ToastUtils;
import io.taucoin.tauapp.publishing.databinding.ActivityCommunityDetailBinding;
import io.taucoin.tauapp.publishing.ui.BaseActivity;
import io.taucoin.tauapp.publishing.ui.constant.IntentExtra;
import io.taucoin.tauapp.publishing.ui.customviews.CommonDialog;
import io.taucoin.tauapp.publishing.ui.friends.AirdropDetailActivity;
import io.taucoin.tauapp.publishing.ui.friends.AirdropSetupActivity;
import io.taucoin.tauapp.publishing.ui.friends.FriendsActivity;
import io.taucoin.tauapp.publishing.ui.qrcode.CommunityQRCodeActivity;
import io.taucoin.tauapp.publishing.ui.user.UserDetailActivity;

/**
 * 社区详情页面
 */
public class CommunityDetailActivity extends BaseActivity implements MemberListAdapter.ClickListener {

    private static final Logger logger = LoggerFactory.getLogger("CommunityDetailActivity");
    private ActivityCommunityDetailBinding binding;
    private CommunityViewModel communityViewModel;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private Disposable memberDisposable;
    private MemberListAdapter adapter;
    private String chainID;
    private CommonDialog blacklistDialog;
    private boolean isJoined;
    private boolean noBalance;

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
            isJoined = getIntent().getBooleanExtra(IntentExtra.IS_JOINED, false);
            noBalance = getIntent().getBooleanExtra(IntentExtra.NO_BALANCE, true);
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

        if (!isJoined || noBalance) {
            binding.itemAddMember.setVisibility(View.GONE);
            binding.itemAirdropCoins.setVisibility(View.GONE);
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
            case R.id.item_airdrop_coins:
                if (memberDisposable != null && !memberDisposable.isDisposed()) {
                    return;
                }
                memberDisposable = communityViewModel.getMemberSingle(chainID)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::enterAirdropPage, it -> {});
                break;
        }
    }

    private void enterAirdropPage(Member member) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.CHAIN_ID, member.chainID);
        if (member.airdropStatus == AirdropStatus.ON.getStatus()) {
            ActivityUtil.startActivity(intent, this, AirdropDetailActivity.class);
        } else {
            ActivityUtil.startActivity(intent, this, AirdropSetupActivity.class);
        }
    }

    @Override
    public void onItemClicked(MemberAndFriend member) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.PUBLIC_KEY, member.publicKey);
        ActivityUtil.startActivity(intent, this, UserDetailActivity.class);
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
        if (memberDisposable != null && !memberDisposable.isDisposed()) {
            memberDisposable.dispose();
        }
    }
}