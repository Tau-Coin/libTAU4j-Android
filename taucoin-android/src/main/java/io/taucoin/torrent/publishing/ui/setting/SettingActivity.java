package io.taucoin.torrent.publishing.ui.setting;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.BuildConfig;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.databinding.ActivitySettingBinding;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.ui.ScanTriggerActivity;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 设置页面
 */
public class SettingActivity extends ScanTriggerActivity implements View.OnClickListener {

    private static final Logger logger = LoggerFactory.getLogger("SettingActivity");
    private ActivitySettingBinding binding;
    private UserViewModel viewModel;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(UserViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_setting);
        binding.setListener(this);
        initView();
    }

    @Override
    protected void refreshAllView() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_setting);
        binding.setListener(this);
        initView();
        setResult(RESULT_OK);
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.setting_title);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        viewModel.getChangeResult().observe(this, result -> {
            if (StringUtil.isNotEmpty(result)) {
                ToastUtils.showShortToast(result);
            } else {
                subscribeCurrentUser();
            }
        });

        if (!BuildConfig.DEBUG) {
            binding.debugLayout.setVisibility(View.GONE);
        }
    }

    /**
     * 订阅当前用户
     */
    private void subscribeCurrentUser() {
        Disposable observeCurrentUser = viewModel.observeCurrentUser()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateUserInfo);
        disposables.add(observeCurrentUser);
    }

    /**
     * 更新当前用户信息
     * @param user 当前用户
     */
    private void updateUserInfo(User user) {
        if(null == user){
            return;
        }
        logger.debug("updateUserInfo::{}", user.nickname);
        binding.tvPublicKey.setText(UsersUtil.getMidHideName(user.publicKey));
        String userName = UsersUtil.getCurrentUserName(user);
        binding.tvUsername.setText(userName);
    }

    @Override
    public void onStart() {
        super.onStart();
        subscribeCurrentUser();
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.ll_favorites:
                ActivityUtil.startActivity(this, FavoritesActivity.class);
                break;
            case R.id.item_privacy_security:
                ActivityUtil.startActivity(this, PrivacySecurityActivity.class);
                break;
            case R.id.item_font_size:
                ActivityUtil.startActivityForResult(this, FontSizeActivity.class,
                        FontSizeActivity.REQUEST_CODE_FONT_SIZE);
                break;
            case R.id.item_help:
                DisplayMetrics dm = getResources().getDisplayMetrics();
                int screenWidth = dm.widthPixels;
                int screenHeight = dm.heightPixels;
                logger.debug("DisplayMetrics screenWidth::{}", screenWidth);
                logger.debug("DisplayMetrics screenHeight::{}", screenHeight);
                logger.debug("DisplayMetrics density::{}", dm.density);
                logger.debug("DisplayMetrics dpi::{}", dm.density * 160);
                logger.debug("DisplayMetrics density::{}", dm.scaledDensity);
                break;
            case R.id.item_debug:
                ActivityUtil.startActivity(this, DebugActivity.class);
                break;
            case R.id.tv_public_key:
            case R.id.tv_import_new_key:
                viewModel.showSaveSeedDialog(this, false);
                break;
            case R.id.tv_username:
            case R.id.tv_username_title:
                String publicKey = MainApplication.getInstance().getPublicKey();
                viewModel.showEditNameDialog(this, publicKey);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == FontSizeActivity.REQUEST_CODE_FONT_SIZE) {
            refreshAllView();
        }
    }
}