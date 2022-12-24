package io.taucoin.news.publishing.ui.setting;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;

import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.entity.LocalMedia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.news.publishing.BuildConfig;
import io.taucoin.news.publishing.MainApplication;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.Constants;
import io.taucoin.news.publishing.core.utils.ActivityUtil;
import io.taucoin.news.publishing.core.utils.AppUtil;
import io.taucoin.news.publishing.core.utils.BitmapUtil;
import io.taucoin.news.publishing.core.utils.DrawablesUtil;
import io.taucoin.news.publishing.core.utils.StringUtil;
import io.taucoin.news.publishing.core.utils.ToastUtils;
import io.taucoin.news.publishing.core.utils.UsersUtil;
import io.taucoin.news.publishing.core.utils.media.MediaUtil;
import io.taucoin.news.publishing.databinding.ActivitySettingBinding;
import io.taucoin.news.publishing.core.storage.sqlite.entity.User;
import io.taucoin.news.publishing.ui.ScanTriggerActivity;
import io.taucoin.news.publishing.ui.download.DownloadViewModel;
import io.taucoin.news.publishing.ui.user.UserViewModel;

/**
 * 设置页面
 */
public class SettingActivity extends ScanTriggerActivity implements View.OnClickListener {

    private static final Logger logger = LoggerFactory.getLogger("SettingActivity");
    private ActivitySettingBinding binding;
    private UserViewModel viewModel;
    private DownloadViewModel downloadViewModel;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(UserViewModel.class);
        downloadViewModel = provider.get(DownloadViewModel.class);
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

        binding.itemUpdates.setRightText2(AppUtil.getVersionName());

        DrawablesUtil.setEndDrawable(binding.tvUsernameTitle, R.mipmap.icon_edit,
                getResources().getDimension(R.dimen.widget_size_14));

        viewModel.getChangeResult().observe(this, result -> {
            if (StringUtil.isNotEmpty(result)) {
                ToastUtils.showShortToast(result);
            } else {
                subscribeCurrentUser();
            }
        });

        boolean showDebug = BuildConfig.DEBUG || BuildConfig.DISPLAY_DEBUG_VIEW;
        binding.debugLayout.setVisibility(showDebug ? View.VISIBLE : View.GONE);
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
        if (null == user) {
            return;
        }
        logger.info("updateUserInfo::{}", user.nickname);
        binding.tvPublicKey.setText(UsersUtil.getMidHideName(user.publicKey));
        String userName = UsersUtil.getCurrentUserName(user);
        binding.tvUsername.setText(userName);

        Bitmap bitmap = UsersUtil.getHeadPic(user);
        if (bitmap != null) {
            BitmapUtil.recycleImageView(binding.ivHeadPic);
            binding.ivHeadPic.setImageBitmap(bitmap);
        }
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
            case R.id.item_dash_board:
                ActivityUtil.startActivity(this, WorkingConditionActivity.class);
                break;
            case R.id.item_data_cost:
                ActivityUtil.startActivity(this, DataCostActivity.class);
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
            case R.id.tv_username:
            case R.id.tv_username_title:
                String publicKey = MainApplication.getInstance().getPublicKey();
                viewModel.showEditNameDialog(this, publicKey);
                break;
            case R.id.iv_head_pic:
                MediaUtil.openGalleryAndCamera(this);
                break;
            case R.id.item_updates:
                showProgressDialog(getString(R.string.app_upgrade_checking));
                downloadViewModel.checkAppVersion(this, true);
                break;
            case R.id.item_personal_profile:
                ActivityUtil.startActivity(this, PersonalProfileActivity.class);
                break;
            case R.id.item_official_telegram:
                ActivityUtil.openUri(Constants.OFFICIAL_TELEGRAM_URL);
                break;
            case R.id.item_share:
                ActivityUtil.shareText(this, getString(R.string.app_share), Constants.APP_SHARE_URL);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case FontSizeActivity.REQUEST_CODE_FONT_SIZE:
                    refreshAllView();
                    break;
                case PictureConfig.CHOOSE_REQUEST:
                    // 例如 LocalMedia 里面返回五种path
                    // 1.media.getPath(); 原图path
                    // 2.media.getCutPath();裁剪后path，需判断media.isCut();切勿直接使用
                    // 3.media.getCompressPath();压缩后path，需判断media.isCompressed();切勿直接使用
                    // 4.media.getOriginalPath()); media.isOriginal());为true时此字段才有值
                    // 5.media.getAndroidQToPath();Android Q版本特有返回的字段，但如果开启了压缩或裁剪还是取裁剪或压缩路径；
                    // 注意：.isAndroidQTransform 为false 此字段将返回空
                    // 如果同时开启裁剪和压缩，则取压缩路径为准因为是先裁剪后压缩
                    List<LocalMedia> result = PictureSelector.obtainMultipleResult(data);
                    if (result != null && result.size() > 0) {
                        LocalMedia media = result.get(0);
                        viewModel.updateHeadPic(media);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeProgressDialog();
        BitmapUtil.recycleImageView(binding.ivHeadPic);
    }
}