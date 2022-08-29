package io.taucoin.torrent.publishing.ui.community;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.databinding.ActivityMembersAddBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;

/**
 * 群组成员添加页面
 */
public class MembersAddActivity extends BaseActivity {
    private ActivityMembersAddBinding binding;
    private String chainID;
    private MembersAddFragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_members_add);
        initParameter();
        initLayout();
        loadFragment();
    }

    private void initParameter() {
        if (getIntent() != null) {
            chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.community_added_members);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        binding.getRoot().setFocusable(true);
        binding.getRoot().setFocusableInTouchMode(true);
    }

    private void loadFragment() {
        if (null == currentFragment) {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.isDestroyed()) {
                return;
            }
            currentFragment = new MembersAddFragment();
            Bundle bundle = new Bundle();
            bundle.putString(IntentExtra.CHAIN_ID, chainID);
            bundle.putLong(IntentExtra.AIRDROP_COIN, Constants.AIRDROP_COIN.longValue());
            currentFragment.setArguments(bundle);
            FragmentTransaction transaction = fm.beginTransaction();
            // Replace whatever is in the fragment container view with this fragment,
            // and add the transaction to the back stack
            transaction.replace(R.id.members_fragment, currentFragment);
            // 执行此方法后，fragment的onDestroy()方法和ViewModel的onCleared()方法都不执行
            // transaction.addToBackStack(null);
            transaction.commitAllowingStateLoss();
        }
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
        // 添加新社区处理事件
        if (item.getItemId() == R.id.menu_done) {
            // 进入社区页面
            binding.getRoot().setFocusable(true);
            binding.getRoot().setFocusableInTouchMode(true);
            if (currentFragment != null) {
                currentFragment.showConfirmDialog();
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        currentFragment = null;
    }
}