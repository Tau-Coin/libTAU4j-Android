package io.taucoin.tauapp.publishing.ui.community;

import android.os.Bundle;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.utils.ViewUtils;
import io.taucoin.tauapp.publishing.databinding.ActivityChainTopBinding;
import io.taucoin.tauapp.publishing.ui.BaseActivity;
import io.taucoin.tauapp.publishing.ui.constant.IntentExtra;

/**
 * 社区排名页面Tab页
 */
public class ChainTopActivity extends BaseActivity {

    private ActivityChainTopBinding binding;
    private String chainID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chain_top);
        initParameter();
        initLayout();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if(getIntent() != null){
            chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setTitle(R.string.chain_top_peers);
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        binding.tvCol1.setText(R.string.chain_top_tab_rank);
        binding.tvCol2.setText(R.string.chain_top_tab_peer);
        binding.tvCol3.setText(R.string.chain_top_tab_coin);

        ViewUtils.updateViewWeight(binding.tvCol2, 3 );
        ViewUtils.updateViewWeight(binding.tvCol3, 4);

        Fragment fragment = new TopPeersFragment();
        Bundle chainBundle = new Bundle();
        chainBundle.putString(IntentExtra.CHAIN_ID, chainID);
        fragment.setArguments(chainBundle);

        FragmentManager fm = getSupportFragmentManager();
        if (fm.isDestroyed()) {
            return;
        }
        FragmentTransaction transaction = fm.beginTransaction();
        // Replace whatever is in the fragment container view with this fragment,
        // and add the transaction to the back stack
        transaction.replace(R.id.fragment_container, fragment);
        // 执行此方法后，fragment的onDestroy()方法和ViewModel的onCleared()方法都不执行
        // transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
    }
}