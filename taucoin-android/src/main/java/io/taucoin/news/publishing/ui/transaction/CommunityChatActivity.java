package io.taucoin.news.publishing.ui.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.TextView;

import com.noober.menu.FloatMenu;

import java.util.ArrayList;
import java.util.List;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.news.publishing.MainApplication;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.model.data.OperationMenuItem;
import io.taucoin.news.publishing.core.model.data.UserAndTx;
import io.taucoin.news.publishing.core.model.data.message.TxType;
import io.taucoin.news.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.news.publishing.core.storage.sqlite.entity.TxLog;
import io.taucoin.news.publishing.core.utils.ActivityUtil;
import io.taucoin.news.publishing.core.utils.CopyManager;
import io.taucoin.news.publishing.core.utils.KeyboardUtils;
import io.taucoin.news.publishing.core.utils.StringUtil;
import io.taucoin.news.publishing.core.utils.ToastUtils;
import io.taucoin.news.publishing.core.utils.UsersUtil;
import io.taucoin.news.publishing.core.utils.ViewUtils;
import io.taucoin.news.publishing.databinding.ActivityCommunityChatBinding;
import io.taucoin.news.publishing.ui.BaseActivity;
import io.taucoin.news.publishing.ui.TauNotifier;
import io.taucoin.news.publishing.ui.community.CommunityViewModel;
import io.taucoin.news.publishing.ui.constant.IntentExtra;
import io.taucoin.news.publishing.ui.constant.Page;
import io.taucoin.news.publishing.ui.customviews.TxLogsDialog;
import io.taucoin.news.publishing.ui.user.UserDetailActivity;
import io.taucoin.news.publishing.ui.user.UserViewModel;

/**
 * news detail
 */
public class CommunityChatActivity extends BaseActivity implements CommunityChatAdapter.ClickListener,
        View.OnClickListener {
    private ActivityCommunityChatBinding binding;
    private TxViewModel txViewModel;
    private UserViewModel userViewModel;
    private CommunityViewModel communityViewModel;
    private CommunityChatAdapter adapter;
    private FloatMenu operationsMenu;
    private TxLogsDialog txLogsDialog;
    private Disposable logsDisposable;
    private final Handler handler = new Handler();
    private final CompositeDisposable disposables = new CompositeDisposable();
    private String repliesHash;
    private String chainID;
    int currentPos = 0;
    boolean isScrollToBottom = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        txViewModel = provider.get(TxViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_community_chat);
        binding.setListener(this);
        initParam();
        initView();
    }

    /**
     * 初始化参数
     */
    private void initParam() {
        if (getIntent() != null) {
            repliesHash = getIntent().getStringExtra(IntentExtra.HASH);
            chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
        }
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.main_community_chat);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        if (getRecyclerView() != null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            getRecyclerView().setLayoutManager(layoutManager);
            getRecyclerView().setItemAnimator(null);
        }
        if (getRefreshLayout() != null) {
            getRefreshLayout().setOnRefreshListener(this);
        }

        adapter = new CommunityChatAdapter(this);
        binding.txList.setAdapter(adapter);

        binding.etMessage.addTextChangedListener(textWatcher);

        binding.getRoot().setOnTouchListener((v, event) -> {
            KeyboardUtils.hideSoftInput(this);
            return false;
        });

        binding.etMessage.setOnFocusChangeListener((v, hasFocus) -> {
            isScrollToBottom = true;
            handler.postDelayed(handleUpdateAdapter, 200);
        });

        binding.etMessage.setOnClickListener(v -> {
            boolean isVisible = KeyboardUtils.isSoftInputVisible(this);
            logger.debug("onSoftInputChanged2::{}", isVisible);
            if (!isVisible) {
                isScrollToBottom = true;
                handler.postDelayed(handleUpdateAdapter, 200);
            }
        });

        showBottomView();
    }

    final Runnable handleUpdateAdapter = () -> {
        if (null == getRecyclerView()) {
            return;
        }
        LinearLayoutManager layoutManager = (LinearLayoutManager) getRecyclerView().getLayoutManager();
        if (layoutManager != null) {
            logger.debug("handleUpdateAdapter isScrollToBottom::{}", isScrollToBottom);
            if (isScrollToBottom) {
                isScrollToBottom = false;
                // 滚动到底部
                int bottomPosition = getItemCount() - 1;
                logger.debug("handleUpdateAdapter scrollToPosition::{}", bottomPosition);
                layoutManager.scrollToPositionWithOffset(bottomPosition, Integer.MIN_VALUE);
            }
        }
    };

    final Runnable handlePullAdapter = () -> {
        if (null == getRecyclerView()) {
            return;
        }
        LinearLayoutManager layoutManager = (LinearLayoutManager) getRecyclerView().getLayoutManager();
        if (layoutManager != null) {
            int bottomPosition = getItemCount() - 1;
            int position = bottomPosition - currentPos;
            layoutManager.scrollToPositionWithOffset(position, 0);
        }
    };

    public RecyclerView getRecyclerView() {
        return binding.txList;
    }

    public SwipeRefreshLayout getRefreshLayout() {
        return binding.refreshLayout;
    }

    private void showBottomView() {
        if (null == binding) {
            return;
        }
        binding.llBottomInput.setVisibility(View.VISIBLE);
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            boolean isEmpty = StringUtil.isEmpty(s);
            binding.tvSend.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    private void closeAllDialog() {
        if (operationsMenu != null) {
            operationsMenu.setOnItemClickListener(null);
            operationsMenu.dismiss();
        }
        if (txLogsDialog != null) {
            txLogsDialog.closeDialog();
        }
    }

    @Override
    public void onClick(View v) {
        closeAllDialog();
        if (v.getId() == R.id.tv_send) {
            Tx tx = buildTx();
            if (txViewModel.validateNoteTx(tx)) {
                isScrollToBottom = true;
                txViewModel.addTransaction(tx);
            }
        }
    }

    /**
     * 构建交易数据
     * @return Tx
     */
    private Tx buildTx() {
        int txType = TxType.NOTE_TX.getType();
        String memo = ViewUtils.getText(binding.etMessage);
        Tx tx = new Tx(chainID, 0L, txType, memo);
        tx.repliedHash = repliesHash;
        return tx;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding.etMessage.removeTextChangedListener(textWatcher);
    }

    @Override
    public void onStart() {
        super.onStart();
        txViewModel.getAddState().observe(this, result -> {
            if (StringUtil.isNotEmpty(result)) {
                ToastUtils.showShortToast(result);
            } else {
                binding.etMessage.getText().clear();
            }
        });

        txViewModel.observerChainTxs().observe(this, txs -> {
            List<UserAndTx> currentList = new ArrayList<>(txs);
            if (currentPos == 0) {
                initScrollToBottom();
                adapter.submitList(currentList, handleUpdateAdapter);
            } else {
                currentList.addAll(adapter.getCurrentList());
                adapter.submitList(currentList, handlePullAdapter);
            }
            binding.refreshLayout.setRefreshing(false);
            binding.refreshLayout.setEnabled(txs.size() != 0 && txs.size() % Page.PAGE_SIZE == 0);

            logger.debug("txs.size::{}", txs.size());

            communityViewModel.clearMsgUnread(chainID);

            closeProgressDialog();
            TauNotifier.getInstance().cancelNotify(chainID);
        });

        loadData(0);

        disposables.add(txViewModel.observeDataSetChanged()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    // 跟当前用户有关系的才触发刷新
                    if (result != null && StringUtil.isNotEmpty(result.getMsg())) {
                        binding.refreshLayout.setRefreshing(false);
                        binding.refreshLayout.setEnabled(false);
                        // 立即执行刷新
                        loadData(0);
                    }
                }));

        userViewModel.getEditBlacklistResult().observe(this, result -> {
            if (result.isSuccess()) {
                ToastUtils.showShortToast(R.string.ban_successfully);
            } else {
                ToastUtils.showShortToast(R.string.ban_failed);
            }
        });
    }
    private void initScrollToBottom() {
        if (!isScrollToBottom) {
            this.isScrollToBottom = true;
            if (null == getRecyclerView()) {
                return;
            }
            LinearLayoutManager layoutManager = (LinearLayoutManager) getRecyclerView().getLayoutManager();
            if (layoutManager != null) {
                int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
                int bottomPosition = getItemCount() - 1;
                this.isScrollToBottom = lastVisibleItemPosition <= bottomPosition &&
                        lastVisibleItemPosition >= bottomPosition - 2;
                logger.debug("handleUpdateAdapter lastVisibleItemPosition::{}, bottomPosition::{}",
                        lastVisibleItemPosition, bottomPosition);
            }
        }
    }


    @Override
    public void onStop() {
        super.onStop();
        disposables.clear();
        // 关闭键盘
        binding.etMessage.clearFocus();
    }

    int getItemCount() {
        if (adapter != null) {
            return adapter.getItemCount();
        }
        return 0;
    }

    @Override
    public void onRefresh() {
        loadData(getItemCount());
    }

    public void loadData(int pos) {
        currentPos = pos;
        txViewModel.loadNotesData(repliesHash, pos, getItemCount());
    }

    @Override
    public void onUserClicked(String publicKey) {
        KeyboardUtils.hideSoftInput(this);
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.PUBLIC_KEY, publicKey);
        ActivityUtil.startActivity(intent, this, UserDetailActivity.class);
    }

//    @Override
//    public void onEditNameClicked(String publicKey) {
//        KeyboardUtils.hideSoftInput(this);
//        String userPk = MainApplication.getInstance().getPublicKey();
//        if (StringUtil.isEquals(userPk, publicKey)) {
//            userViewModel.showEditNameDialog(this, publicKey);
//        } else {
//            userViewModel.showRemarkDialog(activity, senderPk);
//        }
//    }

    @Override
    public void onBanClicked(UserAndTx tx) {
        KeyboardUtils.hideSoftInput(this);
        String showName = UsersUtil.getShowName(tx.sender, tx.senderPk);
        userViewModel.showBanDialog(this, tx.senderPk, showName);
    }

    @Override
    public void onItemLongClicked(TextView view, UserAndTx tx) {
        KeyboardUtils.hideSoftInput(this);
        List<OperationMenuItem> menuList = new ArrayList<>();
        menuList.add(new OperationMenuItem(R.string.tx_operation_copy));
        final URLSpan[] urls = view.getUrls();
        if (urls != null && urls.length > 0) {
            menuList.add(new OperationMenuItem(R.string.tx_operation_copy_link));
        }
        // 用户不能拉黑自己
        if(StringUtil.isNotEquals(tx.senderPk,
                MainApplication.getInstance().getPublicKey())){
            menuList.add(new OperationMenuItem(R.string.tx_operation_blacklist));
        }
        if (tx.favoriteTime <= 0) {
            menuList.add(new OperationMenuItem(R.string.tx_operation_favorite));
        }
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
                case R.string.tx_operation_favorite:
                    txViewModel.setMessageFavorite(tx, false);
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
    public void onLinkClick(String link) {
        KeyboardUtils.hideSoftInput(this);
        ActivityUtil.openUri(this, link);
    }

    @Override
    public void onTxLogClick(String txID, int version) {
        KeyboardUtils.hideSoftInput(this);
        if (logsDisposable != null) {
            disposables.remove(logsDisposable);
        }
        logsDisposable = communityViewModel.observerTxLogs(txID)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(logs -> {
                    showTxLogsDialog(txID, logs, version);
                });
        disposables.add(logsDisposable);
    }

    /**
     * 显示交易确认的对话框
     */
    private void showTxLogsDialog(String txID, List<TxLog> logs, int version) {
        if (txLogsDialog != null && txLogsDialog.isShowing()) {
            txLogsDialog.submitList(logs);
            return;
        }
        txLogsDialog = new TxLogsDialog.Builder(this)
                .setResend(version > 0)
                .setMsgLogsListener(new TxLogsDialog.MsgLogsListener() {
                    @Override
                    public void onRetry() {
                        txViewModel.resendTransaction(txID);
                    }

                    @Override
                    public void onCancel() {
                        if (logsDisposable != null) {
                            disposables.remove(logsDisposable);
                        }
                    }
                }).create();
        txLogsDialog.submitList(logs);
        txLogsDialog.show();
    }
}