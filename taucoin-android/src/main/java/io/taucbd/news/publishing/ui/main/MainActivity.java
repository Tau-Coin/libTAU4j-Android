package io.taucbd.news.publishing.ui.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.entity.LocalMedia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import io.taucbd.news.publishing.MainApplication;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.model.TauDaemon;
import io.taucbd.news.publishing.core.model.data.MemberTips;
import io.taucbd.news.publishing.core.storage.RepositoryHelper;
import io.taucbd.news.publishing.core.storage.sp.SettingsRepository;
import io.taucbd.news.publishing.core.utils.ActivityUtil;
import io.taucbd.news.publishing.core.utils.DrawablesUtil;
import io.taucbd.news.publishing.core.utils.FmtMicrometer;
import io.taucbd.news.publishing.core.utils.LinkUtil;
import io.taucbd.news.publishing.core.utils.CopyManager;
import io.taucbd.news.publishing.core.utils.LocationManagerUtil;
import io.taucbd.news.publishing.core.utils.PermissionUtils;
import io.taucbd.news.publishing.core.utils.RootUtil;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.core.utils.ToastUtils;
import io.taucbd.news.publishing.core.utils.UsersUtil;
import io.taucbd.news.publishing.core.utils.ViewUtils;
import io.taucbd.news.publishing.core.utils.media.MediaUtil;
import io.taucbd.news.publishing.databinding.ActivityMainDrawerBinding;
import io.taucbd.news.publishing.core.storage.sqlite.entity.User;
import io.taucbd.news.publishing.databinding.PromptDialogBinding;
import io.taucbd.news.publishing.receiver.NotificationReceiver;
import io.taucbd.news.publishing.service.WorkloadManager;
import io.taucbd.news.publishing.ui.BaseFragment;
import io.taucbd.news.publishing.ui.ScanTriggerActivity;
import io.taucbd.news.publishing.ui.chat.ChatFragment;
import io.taucbd.news.publishing.ui.community.PasteLinkActivity;
import io.taucbd.news.publishing.ui.community.WalletActivity;
import io.taucbd.news.publishing.ui.community.CommunityFragment;
import io.taucbd.news.publishing.ui.ExternalLinkActivity;
import io.taucbd.news.publishing.ui.community.CommunityCreateActivity;
import io.taucbd.news.publishing.ui.community.CommunityViewModel;
import io.taucbd.news.publishing.ui.constant.IntentExtra;
import io.taucbd.news.publishing.ui.customviews.CommonDialog;
import io.taucbd.news.publishing.ui.download.DownloadViewModel;
import io.taucbd.news.publishing.ui.friends.AirdropCommunityActivity;
import io.taucbd.news.publishing.ui.friends.FriendsActivity;
import io.taucbd.news.publishing.ui.setting.FontSizeActivity;
import io.taucbd.news.publishing.ui.setting.SettingActivity;
import io.taucbd.news.publishing.ui.qrcode.UserQRCodeActivity;
import io.taucbd.news.publishing.ui.transaction.CommunityTabFragment;
import io.taucbd.news.publishing.ui.user.UserViewModel;

/**
 * APP主页面：包含左侧抽屉页面，顶部工具栏，群组列表
 */
public class MainActivity extends ScanTriggerActivity {
    private static final Logger logger = LoggerFactory.getLogger("MainActivity");
    private ActivityMainDrawerBinding binding;
    private ActionBarDrawerToggle toggle;

    private UserViewModel userViewModel;
    private CommunityViewModel communityViewModel;
    private DownloadViewModel downloadViewModel;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final Subject<Integer> mBackClick = PublishSubject.create();
    private CommonDialog joinDialog;
    private CommonDialog locationDialog;
    private User user;
    private BaseFragment currentFragment;
    private SettingsRepository settingsRepo;
    private boolean isFriendLink = false;
    private CommunityFragment communityFragment = new CommunityFragment();
    private ChatFragment chatFragment = new ChatFragment();
//    private BadgeActionProvider badgeProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        MainViewModel mainViewModel = provider.get(MainViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        downloadViewModel = provider.get(DownloadViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_drawer);
        mainViewModel.observeNeedStartDaemon();
        settingsRepo = RepositoryHelper.getSettingsRepository(getApplicationContext());
        initLayout();
        checkCurrentUser();
        initExitApp();
        subscribeAddCommunity();
        if (LocationManagerUtil.isLocServiceEnable()) {
            LocationManagerUtil.requestLocationPermissions(this);
        } else {
            showLocationDialog();
        }
        WorkloadManager.startWakeUpWorker(getApplicationContext());
        RootUtil.checkRoot();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action =  intent.getAction();
        if (StringUtil.isNotEmpty(action) && StringUtil.isEquals(action,
                NotificationReceiver.NOTIFY_ACTION_SHUTDOWN_APP)) {
            logger.info("MainActivity finished");
            finish();
        } else if (StringUtil.isNotEmpty(action) && StringUtil.isEquals(action,
                ExternalLinkActivity.ACTION_CHAIN_LINK_CLICK)) {
            logger.info("MainActivity::chain link clicked");
            if (intent.hasExtra(IntentExtra.LINK)) {
                String chainLink = intent.getStringExtra(IntentExtra.LINK);
                onLinkClick(LinkUtil.decode(chainLink));
            }
        } else if (StringUtil.isNotEmpty(action) && StringUtil.isEquals(action,
                ExternalLinkActivity.ACTION_AIRDROP_LINK_CLICK)) {
            logger.info("MainActivity::airdrop link clicked");
            if (intent.hasExtra(IntentExtra.LINK)) {
                String airdropLink = intent.getStringExtra(IntentExtra.LINK);
                onLinkClick(LinkUtil.decode(airdropLink));
            }
        } else if (StringUtil.isNotEmpty(action) && StringUtil.isEquals(action,
                ExternalLinkActivity.ACTION_FRIEND_LINK_CLICK)) {
            logger.info("MainActivity::airdrop link clicked");
            if (intent.hasExtra(IntentExtra.LINK)) {
                String friendLink = intent.getStringExtra(IntentExtra.LINK);
                onLinkClick(LinkUtil.decode(friendLink));
            }
        } else if (0 != (Intent.FLAG_ACTIVITY_CLEAR_TOP & intent.getFlags())) {
            Bundle bundle = new Bundle();
            bundle.putString(IntentExtra.ID, intent.getStringExtra(IntentExtra.CHAIN_ID));
            bundle.putInt(IntentExtra.TYPE, intent.getIntExtra(IntentExtra.TYPE, -1));
            logger.info("MainActivity:: open community::{}", intent.getStringExtra(IntentExtra.CHAIN_ID));
            updateMainRightFragment(bundle);
        }
    }

    @Override
    protected void refreshAllView() {
        FragmentManager fm = getSupportFragmentManager();
        List<Fragment> fragments = fm.getFragments();
        FragmentTransaction transaction = fm.beginTransaction();
        for (Fragment fragment : fragments) {
            if (fragment instanceof CommunityFragment || fragment instanceof ChatFragment) {
                transaction.remove(fragment);
            }
        }
        transaction.commitAllowingStateLoss();
        communityFragment = new CommunityFragment();
        chatFragment = new ChatFragment();

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_drawer);
        refreshLeftFragment();
//        loadFragmentView(R.id.main_left_fragment, new MainFragment());
//        loadFragmentView(null);
        nextAndBackChange(true);
        initLayout();
        checkCurrentUser();
    }

    private void initRightFragment() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.add(R.id.main_right_fragment, chatFragment);
        transaction.hide(chatFragment);
        transaction.add(R.id.main_right_fragment, communityFragment);
        transaction.hide(communityFragment);
        transaction.commitAllowingStateLoss();

    }

    private void refreshLeftFragment() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        // Replace whatever is in the fragment container view with this fragment,
        // and add the transaction to the back stack
        transaction.replace(R.id.main_left_fragment, new MainFragment());
        // 执行此方法后，fragment的onDestroy()方法和ViewModel的onCleared()方法都不执行
        // transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
    }


    /**
     * 检查当前用户
     */
    private void checkCurrentUser() {
        userViewModel.checkCurrentUser();
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setTitle(R.string.main_title);
        setSupportActionBar(binding.toolbarInclude.toolbar);

        toggle = new ActionBarDrawerToggle(this,
                binding.drawerLayout,
                binding.toolbarInclude.toolbar,
                R.string.open_navigation_drawer,
                R.string.close_navigation_drawer);

        toggle.getDrawerArrowDrawable();

        DrawerArrowDrawable drawable = toggle.getDrawerArrowDrawable();
        int barLength = getResources().getDimensionPixelSize(R.dimen.widget_size_20);
        drawable.setBarLength(barLength);
        int gapSize = getResources().getDimensionPixelSize(R.dimen.widget_size_5);
        drawable.setGapSize(gapSize);
        toggle.setDrawerArrowDrawable(drawable);

        binding.drawerLayout.addDrawerListener(toggle);

        binding.mainRightFragment.setVisibility(View.GONE);

        DrawablesUtil.setStartDrawable(binding.drawer.tvCommunities, R.mipmap.icon_community,
                getResources().getDimensionPixelSize(R.dimen.widget_size_22));
        DrawablesUtil.setStartDrawable(binding.drawer.tvContacts, R.mipmap.icon_contacts,
                getResources().getDimensionPixelSize(R.dimen.widget_size_18));
        initRightFragment();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (toggle != null){
            toggle.syncState();
        }
    }

    /**
     * 订阅当前用户
     */
    private void subscribeCurrentUser() {
        disposables.add(userViewModel.observeCurrentUser()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::updateUserInfo));

    }

    /**
     * 订阅是否需要启动TauDaemon
     */
    private void subscribeAddCommunity(){
        communityViewModel.getAddCommunityState().observe(this, result -> {
            if (result.isSuccess()) {
                updateCommunityFragment(result.getMsg());
                if (joinDialog != null) {
                    joinDialog.show();
                }
            }
        });

        userViewModel.getAddFriendResult().observe(this, result -> {
            logger.debug("getAddFriendResult::{}, {}, {}, {}", result.isSuccess(), result.isExist(),
                    isFriendLink, result.getKey());
            if (result.isSuccess() && isFriendLink) {
                updateFriendFragment(result.getKey());
            }
        });
    }

    /**
     * 更新当前用户信息
     * @param user 当前用户
     */
    private void updateUserInfo(User user) {
        if (null == user) {
            return;
        }
        MainApplication.getInstance().setCurrentUser(user);
        logger.info("Update userPk::{}", user.publicKey);
        this.user = user;
        DrawablesUtil.setEndDrawable(binding.drawer.tvPublicKey, R.mipmap.icon_copy_text,
                getResources().getDimensionPixelSize(R.dimen.widget_size_18));
        binding.drawer.tvPublicKey.setText(getString(R.string.main_public_key,
                UsersUtil.getMidHideName(user.publicKey)));
        binding.drawer.tvPublicKey.setTag(user.publicKey);
        String showName = UsersUtil.getCurrentUserName(user);
        binding.drawer.tvNoteName.setText(showName);
        binding.drawer.roundButton.setImageBitmap(UsersUtil.getHeadPic(user));
        userViewModel.promptUserFirstStartApp(this, user);
    }

    @Override
    public void onStart() {
        super.onStart();
        subscribeCurrentUser();
        downloadViewModel.checkAppVersion(this);

        handleSettingsChanged(getString(R.string.pref_key_dht_nodes));
        disposables.add(settingsRepo.observeSettingsChanged()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleSettingsChanged));

        disposables.add(communityViewModel.observeMemberTips()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleMemberTips));

        disposables.add(communityViewModel.observeCommunitiesAndContacts()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(statistics -> {
                    int communities = ViewUtils.getIntTag(binding.drawer.tvCommunities);
                    if (statistics != null && statistics.getCommunities() != communities) {
                        binding.drawer.tvCommunities.setText(FmtMicrometer.fmtLong(statistics.getCommunities()));
                    }
                    int contacts = ViewUtils.getIntTag(binding.drawer.tvContacts);
                    if (statistics != null && statistics.getContacts() != contacts) {
                        binding.drawer.tvContacts.setText(FmtMicrometer.fmtLong(statistics.getContacts()));
                    }
                }));
    }

    private void handleMemberTips(MemberTips tips) {
        boolean isShowTips = tips.pendingTime > 0;
        int resId = isShowTips ? R.drawable.circle_red : -1;
        binding.drawer.itemWallet.setRightPoint(resId);
    }

    private void handleSettingsChanged(String key) {
        if (StringUtil.isEquals(key, getString(R.string.pref_key_internet_state)) ||
                StringUtil.isEquals(key, getString(R.string.pref_key_dht_nodes))) {
            long nodes = settingsRepo.getLongValue(getString(R.string.pref_key_dht_nodes), 0L);
            boolean isConnecting = settingsRepo.internetState() && nodes <= 0;
            binding.toolbarInclude.toolbar.setTitle(isConnecting ? R.string.main_connecting :
                    R.string.main_title);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (joinDialog != null) {
            joinDialog.closeDialog();
        }
        if (locationDialog != null) {
            locationDialog.closeDialog();
        }
        if (currentFragment != null) {
            FragmentManager fm = getSupportFragmentManager();
            if (!fm.isDestroyed()) {
                fm.beginTransaction().remove(currentFragment).commit();
            }
        }
    }

    /**
     * 左侧抽屉布局点击事件
     */
    public void onClick(View view) {
        if(null == user){
            return;
        }
        switch (view.getId()) {
            case R.id.iv_user_qr_code:
            case R.id.tv_note_name:
                ActivityUtil.startActivity(this, UserQRCodeActivity.class);
                break;
            case R.id.round_button:
                MediaUtil.openGalleryAndCamera(this);
                break;
            case R.id.tv_public_key:
                String publicKey = ViewUtils.getStringTag(view);
                CopyManager.copyText(publicKey);
                ToastUtils.showShortToast(R.string.copy_public_key);
                break;
            case R.id.item_new_community:
                ActivityUtil.startActivity(this, CommunityCreateActivity.class);
                break;
            case R.id.item_contacts:
                ActivityUtil.startActivity(this, FriendsActivity.class);
                break;
            case R.id.item_wallet:
                ActivityUtil.startActivity(this, WalletActivity.class);
                break;
            case R.id.item_setting:
                ActivityUtil.startActivityForResult(this, SettingActivity.class,
                        FontSizeActivity.REQUEST_CODE_FONT_SIZE);
                break;
            case R.id.item_airdrop_coins:
                ActivityUtil.startActivity(this, AirdropCommunityActivity.class);
                break;
            case R.id.item_crypto_cities:
                ActivityUtil.startActivity(this, CryptoCitiesActivity.class);
                break;
            case R.id.item_paste_link:
                ActivityUtil.startActivity(this, PasteLinkActivity.class);
                break;
            case R.id.item_share_app:
                ActivityUtil.shareText(this, getString(R.string.app_share), getString(R.string.app_share_content));
                break;
        }
        if (binding != null) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    private void initJoinSuccessDialog(String airdropPeer) {
        PromptDialogBinding joinBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.prompt_dialog, null, false);
        String airdropPeerName = UsersUtil.getShowName(null, airdropPeer);
        String joinSuccess = getString(R.string.main_chain_join_success, airdropPeerName);
        joinBinding.tvTitle.setText(Html.fromHtml(joinSuccess));
        joinDialog = new CommonDialog.Builder(this)
                .setContentView(joinBinding.getRoot())
                .setCanceledOnTouchOutside(true)
                .create();
        joinDialog.setOnCancelListener(l -> joinDialog = null);
    }

    private boolean isEmptyView() {
        return null == currentFragment;
    }

    /**
     * 计算Tablet分版左侧的占比权重
     */
    private float calculateLeftWeight() {
        float defaultLeftWeight = 3.5f;
        int minLeftWidth = getResources().getDimensionPixelSize(R.dimen.widget_size_240);
        int widthPixels = getResources().getDisplayMetrics().widthPixels;
        logger.debug("calculateLeftWeight::{}, minLeftWidth::{}, defaultLeftWeight::{}",
                widthPixels, minLeftWidth, defaultLeftWeight);
        if (widthPixels > minLeftWidth && widthPixels * defaultLeftWeight / 10 < minLeftWidth) {
            defaultLeftWeight = minLeftWidth * 10f / widthPixels;
        }
        logger.debug("calculateLeftWeight::{}, minLeftWidth::{}, defaultLeftWeight::{}",
                widthPixels, minLeftWidth, defaultLeftWeight);
        return defaultLeftWeight;
    }

    private void nextAndBackChange(boolean isBack) {
        logger.debug("updateMainRightFragment isBack::{}", isBack);
       binding.rlMainLeft.setVisibility(isBack ? View.VISIBLE : View.GONE);
       binding.mainRightFragment.setVisibility(isBack ? View.GONE : View.VISIBLE);
//       ViewUtils.updateViewWeight(binding.rlMainLeft, isBack ? 1F : 0F);
//        ViewUtils.updateViewWeight(binding.mainRightFragment, isBack ? 0F : 1F);
    }

    public void goBack() {
        updateMainRightFragment(new Bundle());
    }

    protected void updateCommunityFragment(String ID) {
        Bundle bundle = new Bundle();
        bundle.putInt(IntentExtra.TYPE, 0);
        bundle.putString(IntentExtra.ID, ID);
        updateMainRightFragment(bundle);
    }

    protected void updateFriendFragment(String ID) {
        Bundle bundle = new Bundle();
        bundle.putInt(IntentExtra.TYPE, 1);
        bundle.putString(IntentExtra.ID, ID);
        updateMainRightFragment(bundle);
    }

    /**
     * 更新主页右面Fragment
     */
    public void updateMainRightFragment(Bundle bundle) {
        if (currentFragment != null) {
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction transaction = fm.beginTransaction();
            // Replace whatever is in the fragment container view with this fragment,
            // and add the transaction to the back stack
            transaction.hide(currentFragment);
            // 执行此方法后，fragment的onDestroy()方法和ViewModel的onCleared()方法都不执行
            // transaction.addToBackStack(null);
            transaction.commitAllowingStateLoss();
        }
        // 创建修改实例
        BaseFragment newFragment = null;
        if (!bundle.isEmpty()) {
            int type = bundle.getInt(IntentExtra.TYPE, -1);
            if (type == 0) {
                newFragment = communityFragment;
            } else if (type == 1) {
                newFragment = chatFragment;
            }
        }
        if (newFragment != null) {
            newFragment.setArguments(bundle);
            loadFragmentView(newFragment);
            logger.debug("updateMainRightFragment::{}", bundle.getString(IntentExtra.ID));
            nextAndBackChange(false);
        } else {
            nextAndBackChange(true);
            currentFragment = null;
        }
    }

    /**
     * 加载Fragment视图
     */
    private void loadFragmentView(BaseFragment rightFragment) {
        FragmentManager fm = getSupportFragmentManager();
        List<Fragment> fragments = fm.getFragments();
        Fragment oldFragment = null;
        for (Fragment fragment : fragments) {
            if (StringUtil.isEquals(fragment.getClass().getSimpleName(),
                    rightFragment.getClass().getSimpleName())) {
                logger.debug("loadFragmentView::{}", rightFragment.getClass().getSimpleName());
                oldFragment = fragment;
                break;
            }
        }
        FragmentTransaction transaction = fm.beginTransaction();
        if (currentFragment != null) {
            transaction.hide(currentFragment);
        }
        if (null == oldFragment) {
            transaction.add(R.id.main_right_fragment, rightFragment);
            transaction.show(rightFragment);
        } else {
            transaction.show(oldFragment);
        }
        transaction.commitAllowingStateLoss();
        currentFragment = rightFragment;
    }

    /**
     * 打开外部Friend link
     * @param link Friend  link
     */
    private void openExternalFriendLink(LinkUtil.Link link) {
        // 加朋友
        isFriendLink = true;
        userViewModel.addFriend(link.getPeer(), link.getData());
    }

    /**
     * 打开外部chain link
     * @param link chain link
     */
    private void openExternalChainLink(LinkUtil.Link link) {
        if (link.isChainLink()) {
            // 加朋友
            isFriendLink = false;
            userViewModel.addFriend(link.getPeer(), null);
        }
        String chainID = link.getData();
        communityViewModel.addCommunity(chainID, link);
    }

    /**
     * 打开外部Airdrop link
     * @param link Airdrop link
     */
    private void openExternalAirdropLink(LinkUtil.Link link) {
        // 加朋友
        String airdropPeer = link.getPeer();
        isFriendLink = false;
        userViewModel.addAirdropFriend(airdropPeer, link);
        // 加入社区
        initJoinSuccessDialog(airdropPeer);
        openExternalChainLink(link);
    }

    private void onLinkClick(LinkUtil.Link link) {
        if (link.isAirdropLink()) {
            openExternalAirdropLink(link);
        } else if (link.isChainLink()) {
            openExternalChainLink(link);
        } else if (link.isFriendLink()) {
            openExternalFriendLink(link);
        }
    }

    private void showLocationDialog() {
        String addLocationCommunity = getString(R.string.pref_key_add_location_community);
        if (settingsRepo.getBooleanValue(addLocationCommunity, false)) {
            logger.debug("showLocationDialog: not need");
            return;
        }
        if (locationDialog != null && locationDialog.isShowing()) {
            return;
        }
        PromptDialogBinding joinBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.prompt_dialog, null, false);
        String joinSuccess = getString(R.string.main_system_positioning_switch);
        joinBinding.tvTitle.setText(joinSuccess);
        locationDialog = new CommonDialog.Builder(this)
                .setContentView(joinBinding.getRoot())
                .setCanceledOnTouchOutside(false)
                .setPositiveButton(R.string.common_setup, (dialog, which) -> {
                    dialog.dismiss();
                    LocationManagerUtil.openSetting(MainActivity.this);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create();
        locationDialog.setOnCancelListener(l -> locationDialog = null);
        locationDialog.show();
    }

    /**
     *  创建右上角Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem menuAlert = menu.findItem(R.id.menu_alert);
        menuAlert.setVisible(false);
//        badgeProvider = (BadgeActionProvider) MenuItemCompat.getActionProvider(menuAlert);
//        badgeProvider.setOnClickListener(0, v -> {
//            ActivityUtil.startActivity(this, NotificationActivity.class);
//        });
        return true;
    }

    /**
     * 右上角Menu选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_scan) {
            openScanQRActivity(user);
        }
        return true;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
//        badgeProvider.setVisibility(true);
//        badgeProvider.setBadge(10);
    }

    @Override
    public void onBackPressed() {
        if (isEmptyView()) {
            mBackClick.onNext(1);
        } else {
            goBack();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("CheckResult")
    private void initExitApp() {
        mBackClick.mergeWith(mBackClick.debounce(2000, TimeUnit.MILLISECONDS)
                .map(i -> 0))
                .scan((prev, cur) -> {
                    if (cur == 0) return 0;
                    return prev + 1;
                })
                .filter(v -> v > 0)
                .subscribe(v -> {
                    if (v == 1) {
                        ToastUtils.showLongToast(R.string.main_exit);
                    } else if (v == 2) {
                        appExit();
                    }
                });
    }

    /**
     * APP退出
     */
    private void appExit(){
        this.finish();
        ToastUtils.cancleToast();
        TauDaemon.getInstance(this).forceStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CommunityFragment.MEMBERS_REQUEST_CODE && resultCode == RESULT_OK) {
            goBack();
        } else if (requestCode == CommunityTabFragment.TX_REQUEST_CODE) {
            if (currentFragment != null) {
                currentFragment.onFragmentResult(requestCode, resultCode, data);
            }
        } else if (resultCode == RESULT_OK && requestCode == FontSizeActivity.REQUEST_CODE_FONT_SIZE) {
            refreshAllView();
        } else if (resultCode == RESULT_OK && requestCode == PictureConfig.CHOOSE_REQUEST) {
            List<LocalMedia> result = PictureSelector.obtainMultipleResult(data);
            if (result != null && result.size() > 0) {
                LocalMedia media = result.get(0);
                userViewModel.updateHeadPic(media);
            }
        } else if (requestCode == LocationManagerUtil.LOCATION_OPEN_REQUEST_CODE) {
            if (LocationManagerUtil.isLocServiceEnable()) {
                LocationManagerUtil.requestLocationPermissions(this);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionUtils.REQUEST_PERMISSIONS_LOCATION) {
            LocationManagerUtil.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
        }
    }
}
