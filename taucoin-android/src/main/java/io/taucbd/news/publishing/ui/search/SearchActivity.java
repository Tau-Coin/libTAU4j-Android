package io.taucbd.news.publishing.ui.search;

import android.os.Bundle;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.databinding.ActivitySearchBinding;
import io.taucbd.news.publishing.ui.BaseActivity;
import io.taucbd.news.publishing.ui.constant.IntentExtra;
import io.taucbd.news.publishing.ui.transaction.SearchNewsFragment;

/**
 * 搜索页面
 */
public class SearchActivity extends BaseActivity {
    private ActivitySearchBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_search);
        initView();
        loadFragment();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.community_search_result);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadFragment() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.isDestroyed()) {
            return;
        }
        SearchNewsFragment currentFragment = new SearchNewsFragment();
        Bundle bundle = new Bundle();
        if (getIntent() != null) {
            bundle.putString(IntentExtra.DATA, getIntent().getStringExtra(IntentExtra.DATA));
        }
        currentFragment.setArguments(bundle);
        FragmentTransaction transaction = fm.beginTransaction();
        // Replace whatever is in the fragment container view with this fragment,
        // and add the transaction to the back stack
        transaction.replace(R.id.fragment_container, currentFragment);
        // 执行此方法后，fragment的onDestroy()方法和ViewModel的onCleared()方法都不执行
        // transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();

    }
}