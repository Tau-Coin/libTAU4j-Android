package io.taucoin.torrent.publishing.ui.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndFriend;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.ChainLinkUtil;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.RootUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivityMainDrawerBinding;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.databinding.ExternalLinkDialogBinding;
import io.taucoin.torrent.publishing.databinding.UserDialogBinding;
import io.taucoin.torrent.publishing.receiver.NotificationReceiver;
import io.taucoin.torrent.publishing.service.WorkloadManager;
import io.taucoin.torrent.publishing.ui.ScanTriggerActivity;
import io.taucoin.torrent.publishing.ui.chat.ChatFragment;
import io.taucoin.torrent.publishing.ui.community.CommunityFragment;
import io.taucoin.torrent.publishing.ui.ExternalLinkActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityCreateActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.download.DownloadViewModel;
import io.taucoin.torrent.publishing.ui.notify.NotificationViewModel;
import io.taucoin.torrent.publishing.ui.friends.FriendsActivity;
import io.taucoin.torrent.publishing.ui.qrcode.KeyQRCodeActivity;
import io.taucoin.torrent.publishing.ui.setting.DataCostActivity;
import io.taucoin.torrent.publishing.ui.setting.FontSizeActivity;
import io.taucoin.torrent.publishing.ui.setting.SettingActivity;
import io.taucoin.torrent.publishing.ui.setting.WorkingConditionActivity;
import io.taucoin.torrent.publishing.ui.user.UserDetailActivity;
import io.taucoin.torrent.publishing.ui.qrcode.UserQRCodeActivity;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * APP主页面：包含左侧抽屉页面，顶部工具栏，群组列表
 */
public class MainActivity extends ScanTriggerActivity {
    private static final Logger logger = LoggerFactory.getLogger("MainActivity");
    private ActivityMainDrawerBinding binding;
    private ActionBarDrawerToggle toggle;

    private UserViewModel userViewModel;
    private MainViewModel mainViewModel;
    private CommunityViewModel communityViewModel;
    private DownloadViewModel downloadViewModel;
    private NotificationViewModel notificationViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private Subject<Integer> mBackClick = PublishSubject.create();
    private CommonDialog seedDialog;
    private CommonDialog linkDialog;
    private User user;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        mainViewModel = provider.get(MainViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        notificationViewModel = provider.get(NotificationViewModel.class);
        downloadViewModel = provider.get(DownloadViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_drawer);
        initLayout();
        checkCurrentUser();
        initExitApp();
        subscribeAddCommunity();
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
            if (intent.hasExtra(IntentExtra.CHAIN_LINK)) {
                String chainLink = intent.getStringExtra(IntentExtra.CHAIN_LINK);
                ChainLinkUtil.ChainLink decode = ChainLinkUtil.decode(chainLink);
                if(decode.isValid()){
                    openExternalLink(decode.getDn(), chainLink);
                }
            }
        } else if (0 != (Intent.FLAG_ACTIVITY_CLEAR_TOP & intent.getFlags())) {
            String chainID = intent.getStringExtra(IntentExtra.CHAIN_ID);
            int type = intent.getIntExtra(IntentExtra.TYPE, -1);
            updateMainRightFragment(type, chainID, intent);
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
            updateViewWeight(binding.mainRightFragment, 0);
        }
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
    private void subscribeNeedStartDaemon(){
        disposables.add(mainViewModel.observeNeedStartEngine()
                .subscribeOn(Schedulers.io())
                .filter((needStart) -> needStart)
                .subscribe((needStart) -> {
                    mainViewModel.startDaemon();
                }));
    }

    /**
     * 订阅是否需要启动TauDaemon
     */
    private void subscribeAddCommunity(){
        communityViewModel.getAddCommunityState().observe(this, result -> {
            if(result.isSuccess()){
                updateMainRightFragment(0, result.getMsg());
            }
        });
    }

    /**
     * 更新当前用户信息
     * @param user 当前用户
     */
    private void updateUserInfo(User user) {
        if(null == user){
            return;
        }
        MainApplication.getInstance().setCurrentUser(user);
        this.user = user;
        binding.drawer.tvPublicKey.setText(UsersUtil.getMidHideName(user.publicKey));
        binding.drawer.tvPublicKey.setTag(user.publicKey);
        binding.drawer.ivPublicKeyCopy.setTag(user.publicKey);
        String showName = UsersUtil.getCurrentUserName(user);
        binding.drawer.tvNoteName.setText(showName);
        binding.drawer.roundButton.setText(StringUtil.getFirstLettersOfName(showName));
        userViewModel.promptUserFirstStartApp(this, user);
    }

    @Override
    public void onStart() {
        super.onStart();
        subscribeCurrentUser();
        subscribeNeedStartDaemon();
//        downloadViewModel.checkAppVersion(this);
    }

    private void handleClipboardContent() {
        String content = CopyManager.getClipboardContent(this);
        if(StringUtil.isNotEmpty(content)){
            boolean isShowLinkDialog = showOpenExternalLinkDialog(content);
            if(isShowLinkDialog){
                CopyManager.clearClipboardContent();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // android10中规定, 目前处于焦点的应用, 才能访问到剪贴板数据
        this.getWindow().getDecorView().post(this::handleClipboardContent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(seedDialog != null){
            seedDialog.closeDialog();
        }
        if(linkDialog != null){
            linkDialog.closeDialog();
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
                ActivityUtil.startActivity(this, UserQRCodeActivity.class);
                break;
            case R.id.round_button:
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.PUBLIC_KEY, user.publicKey);
                ActivityUtil.startActivity(intent, this, UserDetailActivity.class);
                break;
            case R.id.tv_public_key:
            case R.id.tv_public_key_title:
            case R.id.tv_note_name:
                showSeedDialog();
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
            case R.id.item_setting:
                ActivityUtil.startActivityForResult(this, SettingActivity.class,
                        FontSizeActivity.REQUEST_CODE_FONT_SIZE);
                break;
            case R.id.item_share:
                ActivityUtil.shareText(this, getString(R.string.app_share), Constants.APP_SHARE_URL);
                break;
            case R.id.item_data_cost:
                ActivityUtil.startActivity(this, DataCostActivity.class);
                break;
            case R.id.item_working_condition:
                ActivityUtil.startActivity(this, WorkingConditionActivity.class);
                break;
        }
        if (binding != null) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    /**
     * 显示用户Seed的对话框
     */
    private void showSeedDialog() {
        if(null == user){
            return;
        }
        UserDialogBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.user_dialog, null, false);
        dialogBinding.tvPublicKey.setText(UsersUtil.getMidHideName(user.publicKey));
        dialogBinding.ivClose.setOnClickListener(v -> {
            if(seedDialog != null){
                seedDialog.closeDialog();
            }
        });
        dialogBinding.ivPublicKeyCopy.setOnClickListener(v -> {
            CopyManager.copyText(user.publicKey);
            ToastUtils.showShortToast(R.string.copy_public_key);
        });
        dialogBinding.llExportSeed.setOnClickListener(v -> {
            seedDialog.closeDialog();
            ActivityUtil.startActivity(this, KeyQRCodeActivity.class);
        });
        seedDialog = new CommonDialog.Builder(this)
                .setContentView(dialogBinding.getRoot())
                .enableWarpWidth(true)
                .setCanceledOnTouchOutside(false)
                .create();
        seedDialog.show();
    }

    /**
     * 显示打开外部chain link的对话框（来自剪切板或外部链接）
     */
    private boolean showOpenExternalLinkDialog(String chainLink) {
        ChainLinkUtil.ChainLink decode = ChainLinkUtil.decode(chainLink);
        if(decode.isValid()){
            String chainID = decode.getDn();
            ExternalLinkDialogBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                    R.layout.external_link_dialog, null, false);
            dialogBinding.tvName.setText(UsersUtil.getCommunityName(chainID));
            dialogBinding.ivClose.setOnClickListener(v -> {
                if(linkDialog != null){
                    linkDialog.closeDialog();
                }
            });
            dialogBinding.tvYes.setOnClickListener(v -> {
                if(linkDialog != null){
                    linkDialog.closeDialog();
                }
                openExternalLink(chainID, chainLink);
            });
            linkDialog = new CommonDialog.Builder(this)
                    .setContentView(dialogBinding.getRoot())
                    .setCanceledOnTouchOutside(false)
                    .create();
            linkDialog.show();
            return true;
        }
        return false;
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
                updateViewWeight(binding.rlMainLeft, leftWeight);
                updateViewWeight(binding.mainRightFragment, 10 - leftWeight);
            } else {
                if (isEmptyView()) {
                    nextAndBackChange(true);
                } else {
                    updateViewWeight(binding.rlMainLeft, 0F);
                    updateViewWeight(binding.mainRightFragment, 1.0F);
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

    private void updateViewWeight(View view, float weight) {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams)
                view.getLayoutParams();
        layoutParams.weight = weight;
        view.setLayoutParams(layoutParams);
    }

    private void nextAndBackChange(boolean isBack) {
       updateViewWeight(binding.rlMainLeft, isBack ? 1F : 0F);
       updateViewWeight(binding.mainRightFragment, isBack ? 0F : 1F);
    }

    public void goBack() {
        updateMainRightFragment(null);
    }

    public void updateMainRightFragment(int type, String ID) {
        CommunityAndFriend bean = new CommunityAndFriend();
        bean.type = type;
        bean.ID = ID;
        updateMainRightFragment(bean);
    }

    public void updateMainRightFragment(int type, String ID, Intent intent) {
        CommunityAndFriend bean = new CommunityAndFriend();
        bean.type = type;
        bean.ID = ID;
        updateMainRightFragment(bean, intent);
    }

    public void updateMainRightFragment(CommunityAndFriend bean) {
        updateMainRightFragment(bean, getIntent());
    }

    /**
     * 更新主页右面Fragment
     */
    private void updateMainRightFragment(CommunityAndFriend bean, Intent intent) {
        // 创建修改实例
        Fragment newFragment = null;
        Bundle bundle = new Bundle();
        if (bean != null) {
            if (bean.type == 0) {
                newFragment = new CommunityFragment();
            } else if (bean.type == 1) {
                User friend = bean.friend;
                if (null == friend && intent != null) {
                    friend = intent.getParcelableExtra(IntentExtra.BEAN);
                }
                if (friend != null) {
                    bundle.putParcelable(IntentExtra.BEAN, friend);
                }
                newFragment = new ChatFragment();
            }
            bundle.putString(IntentExtra.ID, bean.ID);
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
     * 打开外部chain link
     * @param chainID
     */
    private void openExternalLink(String chainID, String chainLink) {
        disposables.add(communityViewModel.getCommunityByChainIDSingle(chainID)
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(community -> {
                updateMainRightFragment(0, chainID);
            }, it -> {
                communityViewModel.addCommunity(chainID, chainLink);
            }));
    }

    /**
     *  创建右上角Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
        if (requestCode == CommunityFragment.REQUEST_CODE) {
            goBack();
        } else if (resultCode == RESULT_OK && requestCode == FontSizeActivity.REQUEST_CODE_FONT_SIZE) {
            refreshAllView();
        }
    }
}
