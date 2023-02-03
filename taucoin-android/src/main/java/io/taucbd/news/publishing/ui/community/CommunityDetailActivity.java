package io.taucbd.news.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;

import com.andview.refreshview.XRefreshView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.model.data.MemberAndFriend;
import io.taucbd.news.publishing.core.model.data.message.AirdropStatus;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Member;
import io.taucbd.news.publishing.core.utils.ActivityUtil;
import io.taucbd.news.publishing.core.utils.ChainIDUtil;
import io.taucbd.news.publishing.core.utils.ObservableUtil;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.core.utils.ToastUtils;
import io.taucbd.news.publishing.databinding.ActivityCommunityDetailBinding;
import io.taucbd.news.publishing.databinding.CommunityDetailHeaderBinding;
import io.taucbd.news.publishing.ui.BaseActivity;
import io.taucbd.news.publishing.ui.constant.IntentExtra;
import io.taucbd.news.publishing.ui.constant.Page;
import io.taucbd.news.publishing.ui.customviews.CommonDialog;
import io.taucbd.news.publishing.ui.customviews.CustomXRefreshViewFooter;
import io.taucbd.news.publishing.ui.friends.AirdropDetailActivity;
import io.taucbd.news.publishing.ui.friends.AirdropSetupActivity;
import io.taucbd.news.publishing.ui.friends.FriendsActivity;
import io.taucbd.news.publishing.ui.qrcode.CommunityQRCodeActivity;
import io.taucbd.news.publishing.ui.transaction.TransactionCreateActivity;
import io.taucbd.news.publishing.ui.user.UserDetailActivity;

/**
 * 社区详情页面
 */
public class CommunityDetailActivity extends BaseActivity implements MemberListAdapter.ClickListener,
        XRefreshView.XRefreshViewListener {

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
    private int currentPos = 0;
    private boolean isLoadMore;
    private boolean dataChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        communityViewModel.observeNeedStartDaemon();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_community_detail);
        initParameter();
        initLayout();
        initRefreshLayout();
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
//            binding.itemAddMember.setVisibility(View.GONE);
//            binding.itemAirdropCoins.setVisibility(View.GONE);
        }

        adapter = new MemberListAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setItemAnimator(null);
        binding.recyclerView.setAdapter(adapter);

        communityViewModel.getMemberList().observe(this, members -> {
            List<MemberAndFriend> currentList = new ArrayList<>(members);
            int size;
            if (currentPos == 0) {
                size = currentList.size();
                if (size <= Page.PAGE_SIZE) {
                    isLoadMore = size != 0 && size % Page.PAGE_SIZE == 0;
                } else {
                    if (size % Page.PAGE_SIZE == 0) {
                        isLoadMore = true;
                    }
                }
                adapter.submitList(currentList);
            } else {
                currentList.addAll(0, adapter.getCurrentList());
                isLoadMore = members.size() != 0 && members.size() % Page.PAGE_SIZE == 0;
                adapter.submitList(currentList, handlePullAdapter);
            }

            binding.refreshLayout.setLoadComplete(!isLoadMore);
            binding.refreshLayout.stopLoadMore();
        });

        CommunityDetailHeaderBinding headerBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.community_detail_header, null, false);
        binding.recyclerView.addHeaderView(headerBinding.getRoot());

    }

    private final Runnable handlePullAdapter = () -> {
        int dx = binding.recyclerView.getScrollX();
        int dy = binding.recyclerView.getScrollY();
        int offset = getResources().getDimensionPixelSize(R.dimen.widget_size_50);
        binding.recyclerView.smoothScrollBy(dx, dy + offset);
    };

    private void initRefreshLayout() {
        binding.refreshLayout.setXRefreshViewListener(this);
        binding.refreshLayout.setPullRefreshEnable(false);
        binding.refreshLayout.setPullLoadEnable(true);
        binding.refreshLayout.setAutoLoadMore(true);
        binding.refreshLayout.setMoveForHorizontal(true);
        binding.refreshLayout.enableRecyclerViewPullUp(true);

        CustomXRefreshViewFooter footer = new CustomXRefreshViewFooter(this);
        binding.refreshLayout.setCustomFooterView(footer);
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
            case R.id.item_chain_explorer:
                intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                intent.putExtra(IntentExtra.IS_JOINED, isJoined);
                ActivityUtil.startActivity(intent, this, ChainExplorerActivity.class);
                break;
            case R.id.item_pay_people:
                intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                ActivityUtil.startActivity(intent, this, TransactionCreateActivity.class);
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
        intent.putExtra(IntentExtra.BALANCE, member.balance);
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
        loadData(0);

        disposables.add(ObservableUtil.intervalSeconds(2)
                .subscribeOn(Schedulers.io())
                .subscribe(o -> {
                    if (dataChanged) {
                        loadData(0);
                        dataChanged = false;
                    }
                }));

        disposables.add(communityViewModel.observeMembersDataSetChanged()
                .subscribeOn(Schedulers.io())
                .subscribe(result -> {
                    if (result != null) {
                        // 立即执行刷新
                        dataChanged = true;
                    }
                }));
    }

    private void loadData(int pos) {
        this.currentPos = pos;
        communityViewModel.loadCommunityMembers(chainID, pos, getItemCount());
    }

    private int getItemCount() {
        if (adapter != null) {
            return adapter.getItemCount();
        }
        return 0;
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

    @Override
    public void onRefresh(boolean isPullDown) {

    }

    @Override
    public void onLoadMore(boolean isSilence) {
        loadData(getItemCount());
    }

    @Override
    public void onRelease(float direction) {

    }

    @Override
    public void onHeaderMove(double headerMovePercent, int offsetY) {

    }
}