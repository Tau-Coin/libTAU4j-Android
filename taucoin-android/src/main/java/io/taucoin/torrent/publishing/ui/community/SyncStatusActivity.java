package io.taucoin.torrent.publishing.ui.community;

import android.os.Bundle;

import com.noober.menu.FloatMenu;

import java.util.ArrayList;
import java.util.List;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.BlockAndTx;
import io.taucoin.torrent.publishing.core.model.data.OperationMenuItem;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.databinding.ActivityListBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;

/**
 * 同步状态页面
 */
public class SyncStatusActivity extends BaseActivity implements BlockListAdapter.ClickListener{

    private CompositeDisposable disposables = new CompositeDisposable();
    private ActivityListBinding binding;
    private BlockListAdapter adapter;
    private CommunityViewModel communityViewModel;
    private String chainID;
    private FloatMenu operationsMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_list);
        initParameter();
        initLayout();
    }

    /**
     * 初始化参数
     */
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
        binding.toolbarInclude.toolbar.setTitle(R.string.community_sync_status);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new BlockListAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        disposables.add(communityViewModel.observeCommunitySyncStatus(chainID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> adapter.submitList(list, handleUpdateAdapter)));
    }

    private final Runnable handleUpdateAdapter = () -> {
        LinearLayoutManager layoutManager = (LinearLayoutManager) binding.recyclerView.getLayoutManager();
        boolean isTop = !binding.recyclerView.canScrollVertically(-1);
        if (layoutManager != null && isTop) {
            layoutManager.scrollToPositionWithOffset(0, 0);
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (operationsMenu != null) {
            operationsMenu.setOnItemClickListener(null);
            operationsMenu.dismiss();
        }
    }

    @Override
    public void onLongClick(BlockAndTx block) {
        List<OperationMenuItem> menuList = new ArrayList<>();
        menuList.add(new OperationMenuItem(R.string.tx_operation_copy_miner));
        menuList.add(new OperationMenuItem(R.string.tx_operation_copy_hash));
        if (StringUtil.isNotEmpty(block.previousBlockHash)) {
            menuList.add(new OperationMenuItem(R.string.tx_operation_copy_previous_hash));
        }
        operationsMenu = new FloatMenu(this);
        operationsMenu.items(menuList);
        operationsMenu.setOnItemClickListener((v, position) -> {
            OperationMenuItem item = menuList.get(position);
            int resId = item.getResId();
            switch (resId) {
                case R.string.tx_operation_copy_miner:
                    CopyManager.copyText(block.miner);
                    ToastUtils.showShortToast(R.string.copy_successfully);
                    break;
                case R.string.tx_operation_copy_hash:
                    CopyManager.copyText(block.blockHash);
                    ToastUtils.showShortToast(R.string.copy_successfully);
                    break;
                case R.string.tx_operation_copy_previous_hash:
                    CopyManager.copyText(block.previousBlockHash);
                    ToastUtils.showShortToast(R.string.copy_successfully);
                    break;
            }
        });
        operationsMenu.show(getPoint());
    }
}