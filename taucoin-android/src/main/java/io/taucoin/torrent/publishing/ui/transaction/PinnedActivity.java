package io.taucoin.torrent.publishing.ui.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.text.style.URLSpan;
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
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.databinding.ActivityPinnedBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.user.UserDetailActivity;

/**
 * Pinned Message
 */
public class PinnedActivity extends BaseActivity implements NotesListAdapter.ClickListener {
    private ActivityPinnedBinding binding;
    private TxViewModel txViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private NotesListAdapter adapter;
    private String chainID;
    private int currentTab;
    private FloatMenu operationsMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        txViewModel = provider.get(TxViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_pinned);
        initParam();
        initView();
    }

    /**
     * ???????????????
     */
    private void initParam() {
        if (getIntent() != null) {
            chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
            currentTab = getIntent().getIntExtra(IntentExtra.TYPE, -1);
        }
    }

    /**
     * ???????????????
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.community_pinned_message);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new NotesListAdapter(this, chainID, false);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
//        layoutManager.setStackFromEnd(true);
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setItemAnimator(null);
        binding.recyclerView.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();

        loadData();

        txViewModel.observerChainTxs().observe(this, txs -> {
            List<UserAndTx> currentList = new ArrayList<>(txs);
            adapter.submitList(currentList);
        });

        disposables.add(txViewModel.observeDataSetChanged()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    // ??????????????????????????????????????????
                    if (result != null && StringUtil.isNotEmpty(result.getMsg())) {
                        // ??????????????????
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
     * ????????????item???????????????????????????
     */
    @Override
    public void onItemLongClicked(TextView view, UserAndTx tx) {
        List<OperationMenuItem> menuList = new ArrayList<>();
        menuList.add(new OperationMenuItem(R.string.tx_operation_copy));
        final URLSpan[] urls = view.getUrls();
        if (urls != null && urls.length > 0) {
            menuList.add(new OperationMenuItem(R.string.tx_operation_copy_link));
        }
        menuList.add(new OperationMenuItem(tx.pinnedTime <= 0 ? R.string.tx_operation_pin : R.string.tx_operation_unpin));
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
                case R.string.tx_operation_pin:
                case R.string.tx_operation_unpin:
                    txViewModel.setMessagePinned(tx, true);
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
    public void onItemClicked(TextView view, UserAndTx tx) {

    }

    @Override
    public void onUserClicked(String senderPk) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.PUBLIC_KEY, senderPk);
        ActivityUtil.startActivity(intent, this, UserDetailActivity.class);
    }

    @Override
    public void onEditNameClicked(String publicKey) {

    }

    @Override
    public void onBanClicked(UserAndTx tx) {

    }

    @Override
    public void onLinkClick(String link) {
        ActivityUtil.openUri(this, link);
    }

    @Override
    public void onResendClick(String txID) {

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