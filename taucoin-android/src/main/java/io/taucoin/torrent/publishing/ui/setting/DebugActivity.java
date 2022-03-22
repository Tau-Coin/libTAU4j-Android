package io.taucoin.torrent.publishing.ui.setting;

import android.os.Bundle;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.databinding.ActivityDebugBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 隐私安全页面页面
 */
public class DebugActivity extends BaseActivity implements View.OnClickListener {

    private ActivityDebugBinding binding;
    private UserViewModel viewModel;
    private CommunityViewModel communityViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private TauDaemon tauDaemon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(UserViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_debug);
        binding.setListener(this);
        tauDaemon = TauDaemon.getInstance(getApplicationContext());
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.debug_title);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }
    @Override
    public void onStart() {
        super.onStart();
    }


    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_batch_add_friends:
                String name = StringUtil.getText(binding.tvFriendsName);
                int num = StringUtil.getIntText(binding.tvFriendsNum);
                viewModel.batchAddFriends(name, num);
                break;
            case R.id.tv_batch_add_community:
                name = StringUtil.getText(binding.tvCommunityName);
                num = StringUtil.getIntText(binding.tvCommunityNum);
                communityViewModel.batchAddCommunities(name, num);
                break;
            case R.id.tv_update_bootstrap_interval:
                int interval = StringUtil.getIntText(binding.etBootstrapInterval);
                if (interval > 0) {
                    boolean isSuccess = tauDaemon.updateBootstrapInterval(interval);
                    ToastUtils.showShortToast(getString(R.string.debug_update_bootstrap_interval_toast,
                            interval, isSuccess));
                }
                break;
        }
    }
}