package io.taucoin.torrent.publishing.ui.community;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivityChainTopBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;

/**
 * 社区排名页面Tab页
 */
public class ChainTopActivity extends BaseActivity {

    public static final int TOP_PEERS = 0x01;
    public static final int TOP_VOTES = 0x02;
    private ActivityChainTopBinding binding;
    private List<Fragment> fragmentList = new ArrayList<>();
    private int[] titles = null;
    private String chainID;
    private int type;

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
            type = getIntent().getIntExtra(IntentExtra.TYPE, TOP_PEERS);
            chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        boolean isTopPeers = type == TOP_PEERS;
        binding.toolbarInclude.toolbar.setTitle(isTopPeers ? R.string.chain_top_peers :
                R.string.chain_votes);
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        titles = isTopPeers ? new int[]{R.string.chain_top_tab_coin, R.string.chain_top_tab_power} :
                new int[]{R.string.chain_top_tab_consensus, R.string.chain_top_tab_head};

        binding.tvCol1.setText(R.string.chain_top_tab_rank);
        binding.tvCol2.setText(isTopPeers ? R.string.chain_top_tab_peer : R.string.chain_block_number);
        binding.tvCol3.setText(isTopPeers ? R.string.chain_top_tab_coin : R.string.chain_block_hash);

        ViewUtils.updateViewWeight(binding.tvCol2, isTopPeers ? 3 : 3);
        ViewUtils.updateViewWeight(binding.tvCol3, isTopPeers ? 2 : 4);

        Fragment tab1;
        // 添加Tab1页面
        if (isTopPeers) {
            tab1 = new TopPeersFragment();
        } else {
            tab1 = new TopConsensusFragment();
        }
        Bundle chainBundle = new Bundle();
        chainBundle.putString(IntentExtra.CHAIN_ID, chainID);
        chainBundle.putInt(IntentExtra.TYPE, isTopPeers ? TopPeersFragment.TOP_COIN :
                TopConsensusFragment.TOP_CONSENSUS);
        tab1.setArguments(chainBundle);
        fragmentList.add(tab1);

        Fragment tab2;
        // 添加Tab2页面
        if (isTopPeers) {
            tab2 = new TopPeersFragment();
        } else {
            tab2 = new TopConsensusFragment();
        }
        Bundle queueBundle = new Bundle();
        queueBundle.putString(IntentExtra.CHAIN_ID, chainID);
        queueBundle.putInt(IntentExtra.TYPE, isTopPeers ? TopPeersFragment.TOP_POWER :
                TopConsensusFragment.TOP_TIP);
        tab2.setArguments(queueBundle);
        fragmentList.add(tab2);

        FragmentManager fragmentManager = getSupportFragmentManager();
        MyAdapter fragmentAdapter = new MyAdapter(fragmentManager);
        binding.viewPager.setAdapter(fragmentAdapter);
        binding.tabLayout.setupWithViewPager(binding.viewPager);
        binding.viewPager.setOffscreenPageLimit(fragmentList.size());
        binding.viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (isTopPeers) {
                    binding.tvCol3.setText(position == 0 ? R.string.chain_top_tab_coin : R.string.chain_top_tab_power);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    public class MyAdapter extends FragmentPagerAdapter {
        MyAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return getResources().getText(titles[position]);
        }
    }
}