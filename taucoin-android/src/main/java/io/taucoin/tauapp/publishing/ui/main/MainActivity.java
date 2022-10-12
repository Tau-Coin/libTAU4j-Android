package io.taucoin.tauapp.publishing.ui.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
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
import io.taucoin.tauapp.publishing.MainApplication;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.model.TauDaemon;
import io.taucoin.tauapp.publishing.core.model.data.MemberTips;
import io.taucoin.tauapp.publishing.core.storage.RepositoryHelper;
import io.taucoin.tauapp.publishing.core.storage.sp.SettingsRepository;
import io.taucoin.tauapp.publishing.core.utils.ActivityUtil;
import io.taucoin.tauapp.publishing.core.utils.LinkUtil;
import io.taucoin.tauapp.publishing.core.utils.CopyManager;
import io.taucoin.tauapp.publishing.core.utils.LocationManagerUtil;
import io.taucoin.tauapp.publishing.core.utils.PermissionUtils;
import io.taucoin.tauapp.publishing.core.utils.RootUtil;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.core.utils.ToastUtils;
import io.taucoin.tauapp.publishing.core.utils.UsersUtil;
import io.taucoin.tauapp.publishing.core.utils.Utils;
import io.taucoin.tauapp.publishing.core.utils.ViewUtils;
import io.taucoin.tauapp.publishing.core.utils.media.MediaUtil;
import io.taucoin.tauapp.publishing.databinding.ActivityMainDrawerBinding;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.User;
import io.taucoin.tauapp.publishing.databinding.ExternalErrorLinkDialogBinding;
import io.taucoin.tauapp.publishing.databinding.PromptDialogBinding;
import io.taucoin.tauapp.publishing.receiver.NotificationReceiver;
import io.taucoin.tauapp.publishing.service.WorkloadManager;
import io.taucoin.tauapp.publishing.ui.BaseFragment;
import io.taucoin.tauapp.publishing.ui.ScanTriggerActivity;
import io.taucoin.tauapp.publishing.ui.chat.ChatFragment;
import io.taucoin.tauapp.publishing.ui.community.PasteLinkActivity;
import io.taucoin.tauapp.publishing.ui.community.WalletActivity;
import io.taucoin.tauapp.publishing.ui.community.CommunityFragment;
import io.taucoin.tauapp.publishing.ui.ExternalLinkActivity;
import io.taucoin.tauapp.publishing.ui.community.CommunityCreateActivity;
import io.taucoin.tauapp.publishing.ui.community.CommunityViewModel;
import io.taucoin.tauapp.publishing.ui.constant.IntentExtra;
import io.taucoin.tauapp.publishing.ui.customviews.CommonDialog;
import io.taucoin.tauapp.publishing.ui.download.DownloadViewModel;
import io.taucoin.tauapp.publishing.ui.friends.AirdropCommunityActivity;
import io.taucoin.tauapp.publishing.ui.friends.FriendsActivity;
import io.taucoin.tauapp.publishing.ui.setting.FontSizeActivity;
import io.taucoin.tauapp.publishing.ui.setting.SettingActivity;
import io.taucoin.tauapp.publishing.ui.qrcode.UserQRCodeActivity;
import io.taucoin.tauapp.publishing.ui.transaction.CommunityTabFragment;
import io.taucoin.tauapp.publishing.ui.user.UserViewModel;

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
    private CommonDialog linkDialog;
    private CommonDialog joinDialog;
    private User user;
    private BaseFragment currentFragment;
    private SettingsRepository settingsRepo;
    private CommonDialog longTimeCreateDialog;
    private boolean isFriendLink = false;
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
        LocationManagerUtil.requestLocationPermissions(this);
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
                showLongTimeCreateDialog(LinkUtil.decode(chainLink));
            }
        } else if (StringUtil.isNotEmpty(action) && StringUtil.isEquals(action,
                ExternalLinkActivity.ACTION_AIRDROP_LINK_CLICK)) {
            logger.info("MainActivity::airdrop link clicked");
            if (intent.hasExtra(IntentExtra.LINK)) {
                String airdropLink = intent.getStringExtra(IntentExtra.LINK);
                showLongTimeCreateDialog(LinkUtil.decode(airdropLink));
            }
        } else if (StringUtil.isNotEmpty(action) && StringUtil.isEquals(action,
                ExternalLinkActivity.ACTION_FRIEND_LINK_CLICK)) {
            logger.info("MainActivity::airdrop link clicked");
            if (intent.hasExtra(IntentExtra.LINK)) {
                String friendLink = intent.getStringExtra(IntentExtra.LINK);
                showLongTimeCreateDialog(LinkUtil.decode(friendLink));
            }
        } else if (StringUtil.isNotEmpty(action) && StringUtil.isEquals(action,
                ExternalLinkActivity.ACTION_ERROR_LINK_CLICK)) {
            logger.info("MainActivity::error link clicked");
            showErrorLinkDialog();
        } else if (0 != (Intent.FLAG_ACTIVITY_CLEAR_TOP & intent.getFlags())) {
            Bundle bundle = new Bundle();
            bundle.putString(IntentExtra.ID, intent.getStringExtra(IntentExtra.CHAIN_ID));
            bundle.putInt(IntentExtra.TYPE, intent.getIntExtra(IntentExtra.TYPE, -1));
            updateMainRightFragment(bundle);
        }
    }

    @Override
    protected void refreshAllView() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_drawer);
        loadFragmentView(R.id.main_left_fragment, new MainFragment());
        currentFragment = new EmptyFragment();
        loadFragmentView(R.id.main_right_fragment, currentFragment);
        initLayout();
        checkCurrentUser();
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

        if (Utils.isTablet(this)) {
            updateViewChanged();
        } else {
            binding.mainRightFragment.setVisibility(View.GONE);
//            ViewUtils.updateViewWeight(binding.mainRightFragment, 0);
        }

        String networkKey = getString(R.string.pref_key_network_switch);
        boolean networkSwitch = settingsRepo.getBooleanValue(networkKey, true);
        binding.drawer.switchNetwork.setChecked(networkSwitch);
        TauDaemon daemon = TauDaemon.getInstance(getApplicationContext());
        binding.drawer.switchNetwork.setOnCheckedChangeListener((compoundButton, checked) -> {
            if (checked) {
                daemon.reconnectNetwork();
            } else {
                daemon.disconnectNetwork();
            }
            settingsRepo.setBooleanValue(networkKey, checked);
        });
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
        binding.drawer.tvPublicKey.setText(UsersUtil.getMidHideName(user.publicKey));
        binding.drawer.tvPublicKey.setTag(user.publicKey);
        binding.drawer.ivPublicKeyCopy.setTag(user.publicKey);
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
    }

    private void handleMemberTips(MemberTips tips) {
//        boolean isShowTips = tips.incomeTime > 0 || tips.rewardTime > 0 || tips.pendingTime > 0;
//        int resId = -1;
//        if (isShowTips) {
//            resId = R.drawable.circle_pink;
//            if (tips.incomeTime > tips.rewardTime) {
//                resId = R.drawable.circle_red;
//                if (tips.pendingTime >= tips.incomeTime) {
//                    resId = R.drawable.circle_yellow;
//                }
//            } else {
//                if (tips.pendingTime >= tips.rewardTime) {
//                    resId = R.drawable.circle_yellow;
//                }
//            }
//        }
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
        if (linkDialog != null) {
            linkDialog.closeDialog();
        }
        if (joinDialog != null) {
            joinDialog.closeDialog();
        }
        if (longTimeCreateDialog != null) {
            longTimeCreateDialog.closeDialog();
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
            case R.id.tv_public_key:
            case R.id.tv_public_key_title:
            case R.id.tv_note_name:
                ActivityUtil.startActivity(this, UserQRCodeActivity.class);
                break;
            case R.id.round_button:
                MediaUtil.openGalleryAndCamera(this);
                break;
            case R.id.iv_public_key_copy:
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
        }
        if (binding != null) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    private void showErrorLinkDialog() {
        ExternalErrorLinkDialogBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.external_error_link_dialog, null, false);
        dialogBinding.tvClose.setOnClickListener(v -> {
            if(linkDialog != null){
                linkDialog.closeDialog();
            }
        });
        linkDialog = new CommonDialog.Builder(this)
                .setContentView(dialogBinding.getRoot())
                .setCanceledOnTouchOutside(false)
                .create();
        linkDialog.show();
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

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateViewChanged();
    }

    private boolean isEmptyView() {
        return null == currentFragment;
    }

    private void updateViewChanged() {
        if (Utils.isTablet(this)) {
            if (Utils.isLandscape()) {
                float leftWeight = calculateLeftWeight();
                ViewUtils.updateViewWeight(binding.rlMainLeft, leftWeight);
                ViewUtils.updateViewWeight(binding.mainRightFragment, 10 - leftWeight);
                binding.rlMainLeft.setVisibility(View.VISIBLE);
                binding.mainRightFragment.setVisibility(View.VISIBLE);
            } else {
                if (isEmptyView()) {
                    nextAndBackChange(true);
                } else {
                    binding.rlMainLeft.setVisibility(View.GONE);
                    binding.mainRightFragment.setVisibility(View.VISIBLE);
//                    ViewUtils.updateViewWeight(binding.rlMainLeft, 0F);
//                    ViewUtils.updateViewWeight(binding.mainRightFragment, 1.0F);
                }
            }
        } else {
            nextAndBackChange(isEmptyView());
        }
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
    protected void updateMainRightFragment(Bundle bundle) {
        // 创建修改实例
        BaseFragment newFragment = null;
        if (!bundle.isEmpty()) {
            int type = bundle.getInt(IntentExtra.TYPE, -1);
            if (type == 0) {
                newFragment = new CommunityFragment();
            } else if (type == 1) {
                newFragment = new ChatFragment();
            }
        }
        currentFragment = newFragment;
        if (null == newFragment) {
            newFragment = new EmptyFragment();
        }
        newFragment.setArguments(bundle);
        loadFragmentView(R.id.main_right_fragment, newFragment);
        updateViewChanged();
    }

    /**
     * 加载Fragment视图
     */
    private void loadFragmentView(int containerViewId, Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        // Replace whatever is in the fragment container view with this fragment,
        // and add the transaction to the back stack
        transaction.replace(containerViewId, fragment);
        // 执行此方法后，fragment的onDestroy()方法和ViewModel的onCleared()方法都不执行
        // transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
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
        String airdropPeer = null;
        if (link.isChainLink()) {
            // 加朋友
            isFriendLink = false;
            userViewModel.addFriend(link.getPeer(), null);
        } else if (link.isAirdropLink()){
            airdropPeer = link.getPeer();
        }
        String chainID = link.getData();
        communityViewModel.addCommunity(airdropPeer, chainID, link);
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

    private void showLongTimeCreateDialog(LinkUtil.Link link) {
        if (longTimeCreateDialog != null && longTimeCreateDialog.isShowing()) {
            longTimeCreateDialog.closeDialog();
        }
        longTimeCreateDialog = communityViewModel.showLongTimeCreateDialog(this, link,
                new CommonDialog.ClickListener() {
                    @Override
                    public void proceed() {
                        onLinkClick(link);
                    }

                    @Override
                    public void close() {

                    }
                }
        );
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
