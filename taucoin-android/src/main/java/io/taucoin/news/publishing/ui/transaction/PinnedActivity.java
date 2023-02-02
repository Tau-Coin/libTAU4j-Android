package io.taucoin.news.publishing.ui.transaction;

import android.content.Intent;
import android.os.Bundle;
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
import io.taucoin.news.publishing.MainApplication;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.model.data.OperationMenuItem;
import io.taucoin.news.publishing.core.model.data.UserAndTx;
import io.taucoin.news.publishing.core.utils.ActivityUtil;
import io.taucoin.news.publishing.core.utils.CopyManager;
import io.taucoin.news.publishing.core.utils.KeyboardUtils;
import io.taucoin.news.publishing.core.utils.LinkUtil;
import io.taucoin.news.publishing.core.utils.StringUtil;
import io.taucoin.news.publishing.core.utils.ToastUtils;
import io.taucoin.news.publishing.core.utils.UsersUtil;
import io.taucoin.news.publishing.core.utils.media.MediaUtil;
import io.taucoin.news.publishing.databinding.ActivityPinnedBinding;
import io.taucoin.news.publishing.ui.BaseActivity;
import io.taucoin.news.publishing.ui.community.CommunityViewModel;
import io.taucoin.news.publishing.ui.constant.IntentExtra;
import io.taucoin.news.publishing.ui.customviews.PopUpDialog;
import io.taucoin.news.publishing.ui.user.UserDetailActivity;
import io.taucoin.news.publishing.ui.user.UserViewModel;

/**
 * Pinned Message
 */
public class PinnedActivity extends BaseActivity implements NewsListAdapter.ClickListener {
    private ActivityPinnedBinding binding;
    private TxViewModel txViewModel;
    private UserViewModel userViewModel;
    private CommunityViewModel communityViewModel;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private NewsListAdapter adapter;
    private String chainID;
    private FloatMenu operationsMenu;
    private PopUpDialog retweetDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        txViewModel = provider.get(TxViewModel.class);
        userViewModel = provider.get(UserViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_pinned);
        initParam();
        initView();
    }

    /**
     * 初始化参数
     */
    private void initParam() {
        if (getIntent() != null) {
            chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
        }
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.community_pinned_message);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new NewsListAdapter(this, binding.recyclerView);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
//        layoutManager.setStackFromEnd(true);
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setItemAnimator(null);
        binding.recyclerView.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();

        txViewModel.getDeletedResult().observe(this, isSuccess -> {
            loadData();
        });

        loadData();

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
        if (tx != null && StringUtil.isNotEmpty(tx.link)) {
            menuList.add(new OperationMenuItem(R.string.tx_operation_copy_link));
        }
        menuList.add(new OperationMenuItem(tx.pinnedTime <= 0 ? R.string.tx_operation_pin : R.string.tx_operation_unpin));
//        menuList.add(new OperationMenuItem(R.string.tx_operation_msg_hash));

        operationsMenu = new FloatMenu(this);
        operationsMenu.items(menuList);
        operationsMenu.setOnItemClickListener((v, position) -> {
            OperationMenuItem item = menuList.get(position);
            int resId = item.getResId();
            switch (resId) {
                case R.string.tx_operation_copy:
                    CopyManager.copyText(tx.memo);
                    ToastUtils.showShortToast(R.string.copy_successfully);
                    break;
                case R.string.tx_operation_copy_link:
                    CopyManager.copyText(tx.link);
                    ToastUtils.showShortToast(R.string.copy_link_successfully);
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
    public void onUserClicked(String senderPk) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.PUBLIC_KEY, senderPk);
        ActivityUtil.startActivity(intent, this, UserDetailActivity.class);
    }

    @Override
    public void onBanClicked(UserAndTx tx){
        KeyboardUtils.hideSoftInput(this);
        String showName = UsersUtil.getShowName(tx.sender, tx.senderPk);
        userViewModel.showBanDialog(this, tx.senderPk, showName);
    }

    @Override
    public void onItemClicked(UserAndTx tx) {
        KeyboardUtils.hideSoftInput(this);
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.HASH, tx.txID);
        ActivityUtil.startActivity(intent, this, NewsDetailActivity.class);
    }

    @Override
    public void onLinkClick(String link) {
        KeyboardUtils.hideSoftInput(this);
        ActivityUtil.openUri(this, link);
    }

    @Override
    public void onPicturePreview(String picturePath) {
        MediaUtil.previewPicture(this, picturePath);
    }

    @Override
    public void onRetweetClicked(UserAndTx tx) {
        if (retweetDialog != null && retweetDialog.isShowing()) {
            retweetDialog.closeDialog();
        }
        retweetDialog = new PopUpDialog.Builder(this)
                .addItems(R.mipmap.icon_retwitt, getString(R.string.common_retweet_other))
                .addItems(R.mipmap.icon_share_gray, getString(R.string.common_share_external))
                .setOnItemClickListener((dialog, name, code) -> {
                    dialog.cancel();
                    if (code == R.mipmap.icon_retwitt) {
                        Intent intent = new Intent();
                        intent.putExtra(IntentExtra.DATA, tx.memo);
                        intent.putExtra(IntentExtra.LINK, tx.link);
                        intent.putExtra(IntentExtra.PICTURE_PATH, tx.picturePath);
                        ActivityUtil.startActivity(intent, this, NewsCreateActivity.class);
                    } else if (code == R.mipmap.icon_share_gray) {
                        loadChainLink(tx);
                    }
                })
                .create();
        retweetDialog.show();
    }

    private void loadChainLink(UserAndTx tx) {
        disposables.add(communityViewModel.observeLatestMiner(tx.chainID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(miner -> {
                    String userPk = MainApplication.getInstance().getPublicKey();
                    String chainUrl = LinkUtil.encodeChain(userPk, tx.chainID, miner);
                    logger.info("chainUrl::{}", chainUrl);
                    String text = tx.memo;
                    if (StringUtil.isNotEmpty(tx.link)) {
                        text = tx.memo + "\n" + tx.link;
                    }
                    text += "\n" + chainUrl;
                    ActivityUtil.shareText(this, getString(R.string.app_share_news), text);
                }));
    }

    @Override
    public void onReplyClicked(UserAndTx tx) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.CHAIN_ID, tx.chainID);
        intent.putExtra(IntentExtra.HASH, tx.txID);
        ActivityUtil.startActivity(intent, this, NewsCreateActivity.class);
    }

    @Override
    public void onChatClicked(UserAndTx tx) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.CHAIN_ID, tx.chainID);
        intent.putExtra(IntentExtra.HASH, tx.txID);
        ActivityUtil.startActivity(intent, this, CommunityChatActivity.class);
    }

    @Override
    public void onDeleteClicked(UserAndTx tx) {
        txViewModel.deleteThisNews(this, tx.txID);
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
        if (retweetDialog != null && retweetDialog.isShowing()) {
            retweetDialog.closeDialog();
        }
    }

    private void loadData() {
        txViewModel.loadPinnedTxsData(chainID);
    }
}