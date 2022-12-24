package io.taucoin.news.publishing.ui.friends;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import cn.bingoogolapple.refreshlayout.BGARefreshLayout;
import cn.bingoogolapple.refreshlayout.BGAStickinessRefreshViewHolder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.news.publishing.MainApplication;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.model.data.UserAndFriend;
import io.taucoin.news.publishing.core.storage.sqlite.entity.User;
import io.taucoin.news.publishing.core.utils.ActivityUtil;
import io.taucoin.news.publishing.core.utils.ObservableUtil;
import io.taucoin.news.publishing.core.utils.bus.Members;
import io.taucoin.news.publishing.core.utils.bus.RxBus2;
import io.taucoin.news.publishing.core.utils.StringUtil;
import io.taucoin.news.publishing.core.utils.ToastUtils;
import io.taucoin.news.publishing.core.utils.UsersUtil;
import io.taucoin.news.publishing.databinding.ActivityFriendsBinding;
import io.taucoin.news.publishing.ui.BaseActivity;
import io.taucoin.news.publishing.ui.community.MembersAddActivity;
import io.taucoin.news.publishing.ui.constant.IntentExtra;
import io.taucoin.news.publishing.ui.constant.Page;
import io.taucoin.news.publishing.ui.qrcode.UserQRCodeActivity;
import io.taucoin.news.publishing.ui.user.UserDetailActivity;
import io.taucoin.news.publishing.ui.user.UserViewModel;

/**
 * 连接的对等点
 */
public class FriendsActivity extends BaseActivity implements FriendsListAdapter.ClickListener,
        View.OnClickListener, BGARefreshLayout.BGARefreshLayoutDelegate {
    public static final int PAGE_FRIENDS_LIST = 0;
    public static final int PAGE_ADD_MEMBERS = 1;
    private ActivityFriendsBinding binding;
    private UserViewModel userViewModel;
    private FriendsListAdapter adapter;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private String chainID;
    // 代表不同的入口页面
    private int pageType;
    private String scannedFriendPk; // 新扫描的朋友的公钥
    private int order = 0; // 0:last seen, 1:last communication
    private boolean dataChanged = false;
    private boolean isLoadMore = false;
    private int currentPos = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_friends);
        binding.setListener(this);
        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        initParameter(getIntent());
        initView(false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initParameter(intent);
        initView(true);
        subscribeUserList();
        initData();
    }

    /**
     * 初始化参数
     */
    private void initParameter(Intent intent) {
        if (intent != null) {
            chainID = intent.getStringExtra(IntentExtra.CHAIN_ID);
            pageType = intent.getIntExtra(IntentExtra.TYPE, PAGE_FRIENDS_LIST);
            scannedFriendPk = intent.getStringExtra(IntentExtra.PUBLIC_KEY);
            scannedFriendPk = StringUtil.isEquals(MainApplication.getInstance().getPublicKey(),
                    scannedFriendPk) ? "" : scannedFriendPk;
        }
    }

    /**
     * 初始化布局
     */
    private void initView(boolean isNewIntent) {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.drawer_peers);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new FriendsListAdapter(this, pageType, order, scannedFriendPk);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerList.setLayoutManager(layoutManager);
        binding.recyclerList.setItemAnimator(null);
        binding.recyclerList.setAdapter(adapter);

        if (pageType != PAGE_FRIENDS_LIST) {
            binding.llYourself.setVisibility(View.GONE);
            binding.tvYourselfTip.setVisibility(View.GONE);
        }
        if (!isNewIntent) {
            initRefreshLayout();
        }
    }

    private void initRefreshLayout() {
        binding.refreshLayout.setDelegate(this);
        BGAStickinessRefreshViewHolder refreshViewHolder = new BGAStickinessRefreshViewHolder(this, true);
        refreshViewHolder.setRotateImage(R.mipmap.ic_launcher_foreground);
        refreshViewHolder.setStickinessColor(R.color.color_yellow);

        refreshViewHolder.setLoadingMoreText(getString(R.string.common_loading));
        binding.refreshLayout.setPullDownRefreshEnable(false);

        binding.refreshLayout.setRefreshViewHolder(refreshViewHolder);
    }

    private void updateMyselfInfo(User user) {
        if (null == user) {
            return;
        }
        String showName = UsersUtil.getCurrentUserName(user);
        binding.tvNickName.setText(showName);
        binding.ivHeadPic.setImageBitmap(UsersUtil.getHeadPic(user));
    }

    private void subscribeUserList() {
        disposables.add(userViewModel.observeUsersChanged()
                .subscribeOn(Schedulers.io())
                .subscribe(o -> dataChanged = true));

        disposables.add(ObservableUtil.interval(500)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(o -> {
                    if (dataChanged) {
                        initData();
                        dataChanged = false;
                    }
                }));

        userViewModel.getUserList().observe(this, list -> {
            adapter.setOrder(order);
            int size;
            if (currentPos == 0) {
                adapter.submitList(list);
                size = list.size();
                if (StringUtil.isNotEmpty(scannedFriendPk) && size > 0) {
                    size -= 1;
                }
                isLoadMore = size != 0 && size % Page.PAGE_SIZE == 0;
            } else {
                List<UserAndFriend> currentList = new ArrayList<>();
                currentList.addAll(adapter.getCurrentList());
                currentList.addAll(list);
                adapter.submitList(currentList);
                isLoadMore = list.size() != 0 && list.size() % Page.PAGE_SIZE == 0;
            }
            binding.refreshLayout.endLoadingMore();
        });

        if (pageType == PAGE_FRIENDS_LIST) {
            disposables.add(userViewModel.observeCurrentUser()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::updateMyselfInfo));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        initData();
        subscribeUserList();

        userViewModel.getAddFriendResult().observe(this, result -> {
            if (result.isSuccess()) {
                ToastUtils.showShortToast(result.getMsg());
            } else {
                userViewModel.closeDialog();
                scannedFriendPk = result.getMsg();
                adapter = new FriendsListAdapter(this, pageType, order, scannedFriendPk);
                binding.recyclerList.setAdapter(adapter);
                subscribeUserList();
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        disposables.clear();
    }

    /**
     * 创建右上角Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_contacts, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.menu_done);
        MenuItem menuRankC = menu.findItem(R.id.menu_rank_c);
        MenuItem menuRankA = menu.findItem(R.id.menu_rank_a);
        menuItem.setVisible(pageType == PAGE_ADD_MEMBERS);
        menuRankC.setVisible(pageType == PAGE_FRIENDS_LIST && order == 0);
        menuRankA.setVisible(pageType == PAGE_FRIENDS_LIST && order != 0);
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * 右上角Menu选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_done) {
            if (adapter.getSelectedList().size() == 0) {
                ToastUtils.showShortToast(R.string.community_added_members_empty);
            } else {
                RxBus2.getInstance().postSticky(new Members(adapter.getSelectedList()));
                Intent intent = new Intent();
//                intent.putParcelableArrayListExtra(IntentExtra.BEAN, adapter.getSelectedList());
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                ActivityUtil.startActivity(intent, this, MembersAddActivity.class);
            }
        } else if (item.getItemId() == R.id.menu_rank_c) {
            order = 1;
            invalidateOptionsMenu();
            ToastUtils.showShortToast(R.string.menu_rank_a);
            initData();
        } else if (item.getItemId() == R.id.menu_rank_a) {
            order = 0;
            invalidateOptionsMenu();
            ToastUtils.showShortToast(R.string.menu_rank_c);
            initData();
        }
        return true;
    }

    @Override
    public void onItemClicked(@NonNull User user) {
        onItemClicked(user.publicKey);
    }

    private void onItemClicked(@NonNull String publicKey) {
//        if (page == PAGE_SELECT_CONTACT) {
//            Intent intent = new Intent();
//            intent.putExtra(IntentExtra.PUBLIC_KEY, publicKey);
//            setResult(RESULT_OK, intent);
//            onBackPressed();
//        } else {
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.PUBLIC_KEY, publicKey);
            intent.putExtra(IntentExtra.TYPE, UserDetailActivity.TYPE_FRIEND_LIST);
            ActivityUtil.startActivity(intent, this, UserDetailActivity.class);
//        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.ll_exchange_qr) {
            ActivityUtil.startActivity(FriendsActivity.this, UserQRCodeActivity.class);
        } else if (v.getId() == R.id.ll_add_friend) {
            userViewModel.showAddFriendDialog(this);
        } else if (v.getId() == R.id.iv_bot) {
            ActivityUtil.startActivity(FriendsActivity.this, BotsActivity.class);
        } else if (v.getId() == R.id.ll_yourself) {
            onItemClicked(MainApplication.getInstance().getPublicKey());
        }
    }

    private void initData() {
        // 立即执行刷新
        loadData(0);
    }

    private int getItemCount() {
        int count = 0;
        if (adapter != null) {
            count = adapter.getItemCount();
        }
        if (count > 1 && StringUtil.isNotEmpty(scannedFriendPk)) {
            count -= 1;
        }
        return count;
    }

    protected void loadData(int pos) {
        this.currentPos = pos;
        userViewModel.loadUsersList(order, scannedFriendPk, pos, getItemCount());
    }

    @Override
    public void onBGARefreshLayoutBeginRefreshing(BGARefreshLayout refreshLayout) {
        refreshLayout.endRefreshing();
        refreshLayout.setPullDownRefreshEnable(false);
    }

    @Override
    public boolean onBGARefreshLayoutBeginLoadingMore(BGARefreshLayout refreshLayout) {
        logger.debug("LoadingMore isLoadMore::{}", isLoadMore);
        if (isLoadMore) {
            loadData(getItemCount());
            return true;
        } else {
            refreshLayout.endLoadingMore();
            return false;
        }
    }
}