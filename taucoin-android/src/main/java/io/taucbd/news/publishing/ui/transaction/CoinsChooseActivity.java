package io.taucbd.news.publishing.ui.transaction;

import android.content.Intent;
import android.os.Bundle;

import java.util.Arrays;
import java.util.List;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.utils.ChainIDUtil;
import io.taucbd.news.publishing.databinding.ActivityCommunityChooseBinding;
import io.taucbd.news.publishing.ui.BaseActivity;
import io.taucbd.news.publishing.ui.constant.IntentExtra;

/**
 * 群组选择页面
 */
@Deprecated
public class CoinsChooseActivity extends BaseActivity {

    private ActivityCommunityChooseBinding binding;
    private ChooseListAdapter adapter;
    private String coinName;
    private String chainID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_community_choose);
        initParameter();
        initLayout();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if (getIntent() != null) {
            coinName = getIntent().getStringExtra(IntentExtra.COIN_NAME);
            chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.community_choose_coin);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new ChooseListAdapter(coinName);
        String coinName = ChainIDUtil.getCoinName(chainID);
        String[] items = getResources().getStringArray(R.array.coin_name);
        items[1] = coinName;
        adapter.submitList(Arrays.asList(items));
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.joinedList.setLayoutManager(layoutManager);
        binding.joinedList.setOnItemClickListener((view, adapterPosition) -> {
            List<String> currentList = adapter.getCurrentList();
            String coin = currentList.size() - 1 == adapterPosition ? "" :
                    currentList.get(adapterPosition);
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.COIN_NAME, coin);
            CoinsChooseActivity.this.setResult(RESULT_OK, intent);
            CoinsChooseActivity.this.finish();
        });
        binding.joinedList.setAdapter(adapter);
    }
}