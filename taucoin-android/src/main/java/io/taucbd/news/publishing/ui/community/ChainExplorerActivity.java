package io.taucbd.news.publishing.ui.community;

import android.os.Bundle;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.databinding.ActivityChainExplorerBinding;
import io.taucbd.news.publishing.ui.BaseActivity;
import io.taucbd.news.publishing.ui.constant.IntentExtra;
import io.taucbd.news.publishing.ui.transaction.TransactionsTabFragment;

/**
 * 区块链浏览器页面
 */
public class ChainExplorerActivity extends BaseActivity {

    private ActivityChainExplorerBinding binding;
    private TransactionsTabFragment currentFragment;
    private String chainID;
    private boolean isJoined;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chain_explorer);
        initParameter();
        initLayout();
        loadFragment();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if (getIntent() != null) {
            chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
            isJoined = getIntent().getBooleanExtra(IntentExtra.IS_JOINED, false);
        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.community_chain_explorer);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadFragment() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.isDestroyed()) {
            return;
        }
        currentFragment = new TransactionsTabFragment();
        Bundle bundle = new Bundle();
        bundle.putString(IntentExtra.CHAIN_ID, chainID);
        bundle.putBoolean(IntentExtra.IS_JOINED, isJoined);
        currentFragment.setArguments(bundle);
        FragmentTransaction transaction = fm.beginTransaction();
        // Replace whatever is in the fragment container view with this fragment,
        // and add the transaction to the back stack
        transaction.replace(R.id.explorer_fragment, currentFragment);
        // 执行此方法后，fragment的onDestroy()方法和ViewModel的onCleared()方法都不执行
        // transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
    }
}