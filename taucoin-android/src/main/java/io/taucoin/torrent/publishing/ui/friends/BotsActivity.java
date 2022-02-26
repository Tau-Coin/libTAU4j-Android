package io.taucoin.torrent.publishing.ui.friends;

import android.os.Bundle;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.databinding.ActivityBotsBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;

/**
 * Bot页面
 */
public class BotsActivity extends BaseActivity implements View.OnClickListener {
    private ActivityBotsBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_bots);
        binding.setListener(this);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.bot_title);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());
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
}