package io.taucoin.torrent.publishing.ui.setting;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.databinding.ActivityDebugBinding;
import io.taucoin.torrent.publishing.databinding.DebugDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
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
    private CommonDialog debugDialog;
    private Disposable debugDisposable;
    private static boolean NON_REFERABLE = true;

    @Override
    public void triggerSearch(String query, @Nullable Bundle appSearchData) {
        super.triggerSearch(query, appSearchData);
    }

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
        binding.tvNonReferable.setText(getString(R.string.debug_set_non_referable, !NON_REFERABLE));
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
                batchAddTask(name, num, R.id.tv_batch_add_friends);
                break;
            case R.id.tv_batch_add_community:
                name = StringUtil.getText(binding.tvCommunityName);
                num = StringUtil.getIntText(binding.tvCommunityNum);
                communityViewModel.batchAddCommunities(name, num);
                batchAddTask(name, num, 1);
                break;
            case R.id.tv_update_bootstrap_interval:
                int interval = StringUtil.getIntText(binding.etBootstrapInterval);
                if (interval > 0) {
                    boolean isSuccess = tauDaemon.updateBootstrapInterval(interval);
                    ToastUtils.showShortToast(getString(R.string.debug_update_bootstrap_interval_toast,
                            interval, isSuccess));
                }
                break;
            case R.id.tv_non_referable:
                NON_REFERABLE = !NON_REFERABLE;
                tauDaemon.setNonReferable(NON_REFERABLE);
                binding.tvNonReferable.setText(getString(R.string.debug_set_non_referable, !NON_REFERABLE));
                ToastUtils.showShortToast(getString(R.string.debug_update_non_referable_toast, NON_REFERABLE));
                break;
        }
    }

    private void batchAddTask(String name, int num, int id) {
        if (debugDialog != null && debugDialog.isShowing()) {
            return;
        }
        DebugDialogBinding debugBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.debug_dialog, null, false);
        debugDialog = new CommonDialog.Builder(this)
                .setContentView(debugBinding.getRoot())
                .setCanceledOnTouchOutside(false)
                .create();
        debugDialog.show();

        debugBinding.tvCancel.setOnClickListener(v -> {
            debugDialog.closeDialog();
            if (debugDisposable != null && !debugDisposable.isDisposed()) {
                debugDisposable.dispose();
            }
        });

        if (debugDisposable != null && !debugDisposable.isDisposed()) {
            return;
        }
        Observable<Integer> observable;
        if (id == R.id.tv_batch_add_friends) {
            observable = viewModel.batchAddFriends(name, num);
        } else {
            observable = communityViewModel.batchAddCommunities(name, num);
        }
        debugDisposable = observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(i -> {
                    int progress = i * 100 / num;
                    debugBinding.cvProgress.setProgress(progress);
                    debugBinding.tvNum.setText(i + "/" + num);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (debugDisposable != null && !debugDisposable.isDisposed()) {
            debugDisposable.dispose();
        }
        if (debugDialog != null && debugDialog.isShowing()) {
            debugDialog.closeDialog();
        }
    }
}