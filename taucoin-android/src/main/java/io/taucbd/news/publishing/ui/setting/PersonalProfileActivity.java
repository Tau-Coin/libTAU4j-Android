package io.taucbd.news.publishing.ui.setting;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.core.utils.ToastUtils;
import io.taucbd.news.publishing.core.utils.Utils;
import io.taucbd.news.publishing.core.utils.ViewUtils;
import io.taucbd.news.publishing.databinding.ActivityPersonalProfileBinding;
import io.taucbd.news.publishing.ui.BaseActivity;
import io.taucbd.news.publishing.ui.user.UserViewModel;

/**
 * 个人简介页面
 */
public class PersonalProfileActivity extends BaseActivity {

    private ActivityPersonalProfileBinding binding;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private UserViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_personal_profile);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(UserViewModel.class);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.setting_personal_profile);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        viewModel.getEditProfileResult().observe(this, result -> {
            ToastUtils.showShortToast(R.string.setting_personal_profile_result);
            this.finish();
        });
    }

    /**
     *  创建右上角Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_done, menu);
        return true;
    }

    /**
     * 右上角Menu选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_done) {
            String profile = ViewUtils.getText(binding.etPersonalProfile);
            byte[] profileBytes = Utils.textStringToBytes(profile);
            if (profileBytes != null && profileBytes.length > 100) {
                ToastUtils.showShortToast(R.string.setting_personal_profile_too_long);
                return true;
            }
            viewModel.updatePersonalProfile(profile);
        }
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        disposables.add(viewModel.observeCurrentUser()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(user -> {
                String profile = ViewUtils.getText(binding.etPersonalProfile);
                if (StringUtil.isEmpty(profile)) {
                    binding.etPersonalProfile.setText(user.profile);
                }
            }, it -> {}));
    }


    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }
}