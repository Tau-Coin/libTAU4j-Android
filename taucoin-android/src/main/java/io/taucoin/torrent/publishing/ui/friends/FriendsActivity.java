package io.taucoin.torrent.publishing.ui.friends;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.data.UserAndFriend;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.ChainUrlUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.databinding.ActivityFriendsBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.community.MembersAddActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.qrcode.UserQRCodeActivity;
import io.taucoin.torrent.publishing.ui.user.UserDetailActivity;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 连接的对等点
 */
public class FriendsActivity extends BaseActivity implements FriendsListAdapter.ClickListener,
        View.OnClickListener {
    public static final int PAGE_FRIENDS_LIST = 0;
    public static final int PAGE_SELECT_CONTACT = 1;
    public static final int PAGE_ADD_MEMBERS = 2;
    public static final int PAGE_CREATION_ADD_MEMBERS = 4;
    private ActivityFriendsBinding binding;
    private UserViewModel userViewModel;
    private CommunityViewModel communityViewModel;
    private FriendsListAdapter adapter;
    private CompositeDisposable disposables = new CompositeDisposable();
    private String chainID;
    // 代表不同的入口页面
    private int page;
    private String scannedFriendPk; // 新扫描的朋友的公钥
    private int order = 0; // 0:last seen, 1:last communication

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_friends);
        binding.setListener(this);
        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        initParameter(getIntent());
        initView();
        onRefresh();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initParameter(intent);
        initView();
        subscribeUserList();
        onRefresh();
    }

    /**
     * 初始化参数
     */
    private void initParameter(Intent intent) {
        if (intent != null) {
            chainID = intent.getStringExtra(IntentExtra.CHAIN_ID);
            page = intent.getIntExtra(IntentExtra.TYPE, PAGE_FRIENDS_LIST);
            scannedFriendPk = intent.getStringExtra(IntentExtra.PUBLIC_KEY);
        }
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.drawer_peers);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        binding.refreshLayout.setRefreshing(false);
        binding.refreshLayout.setEnabled(false);
        adapter = new FriendsListAdapter(this, page, order, scannedFriendPk);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerList.setLayoutManager(layoutManager);
        binding.recyclerList.setItemAnimator(null);
        binding.recyclerList.setAdapter(adapter);

        if (page != PAGE_FRIENDS_LIST) {
            binding.llYourself.setVisibility(View.GONE);
            binding.tvYourselfTip.setVisibility(View.GONE);
        }
    }

    private void updateMyselfInfo(User user) {
        if(null == user){
            return;
        }
        String showName = UsersUtil.getCurrentUserName(user);
        binding.tvNickName.setText(showName);
        binding.ivHeadPic.setImageBitmap(UsersUtil.getHeadPic(user));
    }

    private void subscribeUserList() {
        disposables.add(userViewModel.observeUserDataSetChanged()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> onRefresh()));
        disposables.add(userViewModel.observeMemberDataSetChanged()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> onRefresh()));
        disposables.add(userViewModel.observeFriendDataSetChanged()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> onRefresh()));
        userViewModel.getUserList().observe(this, list -> {
            adapter.setOrder(order);
            adapter.submitList(list);
        });

        if (page == PAGE_FRIENDS_LIST) {
            disposables.add(userViewModel.observeCurrentUser()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::updateMyselfInfo));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        onRefresh();
        subscribeUserList();

        userViewModel.getAddFriendResult().observe(this, result -> {
            if (result.isSuccess()) {
                ToastUtils.showShortToast(result.getMsg());
            } else {
                userViewModel.closeDialog();
                scannedFriendPk = result.getMsg();
                adapter = new FriendsListAdapter(this, page, order, scannedFriendPk);
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
        menuItem.setVisible(page == PAGE_ADD_MEMBERS || page == PAGE_CREATION_ADD_MEMBERS);
        menuRankC.setVisible(page == PAGE_FRIENDS_LIST && order == 0);
        menuRankA.setVisible(page == PAGE_FRIENDS_LIST && order != 0);
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * 右上角Menu选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_done) {
            Intent intent = new Intent();
            intent.putParcelableArrayListExtra(IntentExtra.BEAN, adapter.getSelectedList());
            if (page == PAGE_CREATION_ADD_MEMBERS) {
                setResult(RESULT_OK, intent);
                this.finish();
            } else {
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                ActivityUtil.startActivity(intent, this, MembersAddActivity.class);
            }
        } else if (item.getItemId() == R.id.menu_rank_c) {
            order = 1;
            invalidateOptionsMenu();
            ToastUtils.showShortToast(R.string.menu_rank_a);
            onRefresh();
        } else if (item.getItemId() == R.id.menu_rank_a) {
            order = 0;
            invalidateOptionsMenu();
            ToastUtils.showShortToast(R.string.menu_rank_c);
            onRefresh();
        }
        return true;
    }

    @Override
    public void onItemClicked(@NonNull User user) {
        onItemClicked(user.publicKey);
    }

    private void onItemClicked(@NonNull String publicKey) {
        if (page == PAGE_SELECT_CONTACT) {
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.PUBLIC_KEY, publicKey);
            setResult(RESULT_OK, intent);
            onBackPressed();
        } else {
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.PUBLIC_KEY, publicKey);
            intent.putExtra(IntentExtra.TYPE, UserDetailActivity.TYPE_FRIEND_LIST);
            ActivityUtil.startActivity(intent, this, UserDetailActivity.class);
        }
    }

    @Override
    public void onShareClicked(UserAndFriend user) {
        showShareDialog();
    }

    /**
     * 显示联系平台的对话框
     */
    private void showShareDialog() {
        disposables.add(communityViewModel.getCommunityMembersLimit(chainID, Constants.CHAIN_LINK_BS_LIMIT)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(list -> {
                    String chainUrl = ChainUrlUtil.encode(chainID, list);
                    ActivityUtil.shareText(this, getString(R.string.contacts_share_link_via), chainUrl);
                }));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.ll_exchange_qr) {
            ActivityUtil.startActivity(FriendsActivity.this, UserQRCodeActivity.class);
        } else if (v.getId() == R.id.ll_add_friend) {
            userViewModel.showAddFriendDialog(this);
        } else if (v.getId() == R.id.iv_bot) {
            ActivityUtil.startActivity(FriendsActivity.this, YourselfActivity.class);
        } else if (v.getId() == R.id.ll_yourself) {
            onItemClicked(MainApplication.getInstance().getPublicKey());
        }
    }

    @Override
    public void onRefresh() {
        // 立即执行刷新
        userViewModel.loadUsersList(order, page != PAGE_FRIENDS_LIST, scannedFriendPk);
    }
}