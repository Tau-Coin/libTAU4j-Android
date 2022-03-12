package io.taucoin.torrent.publishing.ui.setting;

import android.content.Intent;
import android.os.Bundle;
import android.text.style.URLSpan;
import android.widget.TextView;

import com.noober.menu.FloatMenu;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.OperationMenuItem;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.databinding.ActivityFavoritesBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.transaction.TxViewModel;
import io.taucoin.torrent.publishing.ui.user.UserDetailActivity;

/**
 * 设置页面
 */
public class FavoritesActivity extends BaseActivity implements FavoriteListAdapter.ClickListener{

    private ActivityFavoritesBinding binding;
    private TxViewModel viewModel;
    private FavoriteListAdapter adapter;
    private FloatMenu operationsMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(TxViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_favorites);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.setting_favorites);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new FavoriteListAdapter(this);
        DefaultItemAnimator animator = new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
                return true;
            }
        };
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerView.setLayoutManager(layoutManager);

        binding.recyclerView.setItemAnimator(animator);
        binding.recyclerView.setAdapter(adapter);

        viewModel.observerFavorites().observe(this, favorites -> {
            adapter.submitList(favorites);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
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
    public void onUserClicked(String publicKey) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.PUBLIC_KEY, publicKey);
        ActivityUtil.startActivity(intent, this, UserDetailActivity.class);
    }

    @Override
    public void onLinkClick(String link) {
        ActivityUtil.openUri(this, link);
    }

    @Override
    public void onItemLongClicked(TextView view, UserAndTx tx) {
        List<OperationMenuItem> menuList = new ArrayList<>();
        menuList.add(new OperationMenuItem(R.string.tx_operation_copy));
        final URLSpan[] urls = view.getUrls();
        if (urls != null && urls.length > 0) {
            menuList.add(new OperationMenuItem(R.string.tx_operation_copy_link));
        }
        menuList.add(new OperationMenuItem(R.string.tx_operation_favorite_delete));
        menuList.add(new OperationMenuItem(R.string.tx_operation_msg_hash));

        operationsMenu = new FloatMenu(this);
        operationsMenu.items(menuList);
        operationsMenu.setOnItemClickListener((v, position) -> {
            OperationMenuItem item = menuList.get(position);
            int resId = item.getResId();
            switch (resId) {
                case R.string.tx_operation_copy:
                    CopyManager.copyText(view.getText());
                    ToastUtils.showShortToast(R.string.copy_successfully);
                    break;
                case R.string.tx_operation_copy_link:
                    if (urls != null && urls.length > 0) {
                        String link = urls[0].getURL();
                        CopyManager.copyText(link);
                        ToastUtils.showShortToast(R.string.copy_link_successfully);
                    }
                    break;
                case R.string.tx_operation_favorite_delete:
                    viewModel.setMessageFavorite(tx, false);
                    break;
                case R.string.tx_operation_msg_hash:
                    String msgHash = tx.txID;
                    CopyManager.copyText(msgHash);
                    ToastUtils.showShortToast(R.string.copy_message_hash);
                    break;

            }
        });
        operationsMenu.show(getPoint());
    }
}