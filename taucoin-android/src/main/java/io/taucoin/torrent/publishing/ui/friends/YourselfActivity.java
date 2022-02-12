package io.taucoin.torrent.publishing.ui.friends;

import android.os.Bundle;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.BitmapUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.databinding.ActivityYourselfBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * Yourself
 */
public class YourselfActivity extends BaseActivity implements View.OnClickListener {
    private CompositeDisposable disposables = new CompositeDisposable();
    private ActivityYourselfBinding binding;
    private UserViewModel userViewModel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        userViewModel.observeNeedStartDaemon();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_yourself);
        binding.setListener(this);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.bot_yourself);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void updateMyselfInfo(User user) {
        String nickName = UsersUtil.getCurrentUserName(user);
        binding.tvNickName.setText(nickName);
        binding.leftView.setImageBitmap(UsersUtil.getHeadPic(user));
        String midHideName = UsersUtil.getMidHideName(user.publicKey);
        binding.tvPublicKey.setText(midHideName);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ll_airdrop:
                ActivityUtil.startActivity(this, AirdropCommunityActivity.class);
                break;
            case R.id.ll_hello_world:
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        disposables.add(userViewModel.observeCurrentUser()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateMyselfInfo));
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BitmapUtil.recycleImageView(binding.leftView);
    }
}