package io.taucoin.torrent.publishing.ui.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

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
import io.taucoin.torrent.publishing.core.model.data.OperationMenuItem;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.databinding.ActivityListBinding;
import io.taucoin.torrent.publishing.databinding.ItemLeftNoteBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityTabs;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.user.UserDetailActivity;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * Pinned Message
 */
public class PinnedActivity extends BaseActivity implements PinnedListAdapter.ClickListener {
    private ActivityListBinding binding;
    private TxViewModel txViewModel;
    private UserViewModel userViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private PinnedListAdapter adapter;
    private String chainID;
    private int currentTab;
    private FloatMenu operationsMenu;
    private ItemLeftNoteBinding headViewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        txViewModel = provider.get(TxViewModel.class);
        txViewModel.observeNeedStartDaemon();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_list);
        initParam();
        initView();
    }

    /**
     * 初始化参数
     */
    private void initParam() {
        if (getIntent() != null) {
            chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
            currentTab = getIntent().getIntExtra(IntentExtra.TYPE, -1);
        }
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.community_pinned_message);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new PinnedListAdapter(this, chainID);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
//        layoutManager.setStackFromEnd(true);
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setItemAnimator(null);
        binding.recyclerView.setAdapter(adapter);

        if (currentTab == CommunityTabs.MARKET.getIndex()) {
            headViewBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                    R.layout.item_left_note, null, false);
            int padding = getResources().getDimensionPixelSize(R.dimen.widget_size_10);
            headViewBinding.getRoot().setPadding(padding, padding, padding, 0);
            headViewBinding.tvName.setVisibility(View.GONE);
            headViewBinding.leftView.tvBlacklist.setVisibility(View.GONE);
            headViewBinding.leftView.ivHeadPic.setImageResource(R.mipmap.icon_escrow_service_logo);
            headViewBinding.tvMsg.setText(R.string.community_escrow_service_enter);
            headViewBinding.tvTime.setVisibility(View.GONE);
            binding.recyclerView.addHeaderView(headViewBinding.getRoot());
            headViewBinding.middleView.setOnClickListener(v -> {
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                ActivityUtil.startActivity(intent, this, EscrowServiceActivity.class);
            });
        }

        loadData();
    }

    @Override
    public void onStart() {
        super.onStart();
        txViewModel.observerChainTxs().observe(this, txs -> {
            List<UserAndTx> currentList = new ArrayList<>(txs);
            adapter.submitList(currentList);
        });

        disposables.add(txViewModel.observeDataSetChanged()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    // 跟当前用户有关系的才触发刷新
                    if (result != null && StringUtil.isNotEmpty(result.getMsg())) {
                        // 立即执行刷新
                        loadData();
                    }
                }));
    }

    @Override
    public void onStop() {
        super.onStop();
        disposables.clear();
    }

    /**
     * 显示每个item长按操作选项对话框
     */
    @Override
    public void onItemLongClicked(TextView view, UserAndTx tx) {
        List<OperationMenuItem> menuList = new ArrayList<>();
        menuList.add(new OperationMenuItem(R.string.tx_operation_copy));
        final URLSpan[] urls = view.getUrls();
        if (urls != null && urls.length > 0) {
            menuList.add(new OperationMenuItem(R.string.tx_operation_copy_link));
        }
        menuList.add(new OperationMenuItem(tx.pinned == 0 ? R.string.tx_operation_pin : R.string.tx_operation_unpin));
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
                case R.string.tx_operation_blacklist:
                    String publicKey = tx.senderPk;
                    userViewModel.setUserBlacklist(publicKey, true);
                    ToastUtils.showShortToast(R.string.blacklist_successfully);
                    break;
                case R.string.tx_operation_pin:
                case R.string.tx_operation_unpin:
                    txViewModel.setMessagePinned(tx);
                    setResult(RESULT_OK);
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

    @Override
    public void onUserClicked(String senderPk) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.PUBLIC_KEY, senderPk);
        ActivityUtil.startActivity(intent, this, UserDetailActivity.class);
    }

    @Override
    public void onItemClicked(UserAndTx tx) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.ID, tx.txID);
        intent.putExtra(IntentExtra.CHAIN_ID, tx.chainID);
        intent.putExtra(IntentExtra.PUBLIC_KEY, tx.senderPk);
        ActivityUtil.startActivity(intent, this, SellDetailActivity.class);
    }

    @Override
    public void onLinkClick(String link) {
        ActivityUtil.openUri(this, link);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeAllDialog();
    }

    private void closeAllDialog() {
        if (operationsMenu != null) {
            operationsMenu.setOnItemClickListener(null);
            operationsMenu.dismiss();
        }
    }

    private void loadData() {
        txViewModel.loadPinnedTxsData(currentTab, chainID);
    }
}