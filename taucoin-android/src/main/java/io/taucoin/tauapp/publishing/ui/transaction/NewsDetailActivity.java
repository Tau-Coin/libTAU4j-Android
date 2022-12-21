package io.taucoin.tauapp.publishing.ui.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.View;
import android.widget.TextView;

import com.noober.menu.FloatMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cn.bingoogolapple.refreshlayout.BGARefreshLayout;
import cn.bingoogolapple.refreshlayout.BGAStickinessRefreshViewHolder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.tauapp.publishing.MainApplication;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.model.data.OperationMenuItem;
import io.taucoin.tauapp.publishing.core.model.data.UserAndTx;
import io.taucoin.tauapp.publishing.core.utils.ActivityUtil;
import io.taucoin.tauapp.publishing.core.utils.ChainIDUtil;
import io.taucoin.tauapp.publishing.core.utils.CopyManager;
import io.taucoin.tauapp.publishing.core.utils.DateUtil;
import io.taucoin.tauapp.publishing.core.utils.DrawablesUtil;
import io.taucoin.tauapp.publishing.core.utils.FmtMicrometer;
import io.taucoin.tauapp.publishing.core.utils.KeyboardUtils;
import io.taucoin.tauapp.publishing.core.utils.LinkUtil;
import io.taucoin.tauapp.publishing.core.utils.Logarithm;
import io.taucoin.tauapp.publishing.core.utils.SpanUtils;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.core.utils.ToastUtils;
import io.taucoin.tauapp.publishing.core.utils.UsersUtil;
import io.taucoin.tauapp.publishing.databinding.ActivityNewsDetailBinding;
import io.taucoin.tauapp.publishing.databinding.ItemNewsBinding;
import io.taucoin.tauapp.publishing.ui.BaseActivity;
import io.taucoin.tauapp.publishing.ui.constant.IntentExtra;
import io.taucoin.tauapp.publishing.ui.constant.Page;
import io.taucoin.tauapp.publishing.ui.customviews.AutoLinkTextView;
import io.taucoin.tauapp.publishing.ui.customviews.PopUpDialog;
import io.taucoin.tauapp.publishing.ui.user.UserDetailActivity;
import io.taucoin.tauapp.publishing.ui.user.UserViewModel;

/**
 * news detail
 */
public class NewsDetailActivity extends BaseActivity implements NewsListAdapter.BaseClickListener,
        BGARefreshLayout.BGARefreshLayoutDelegate {
    private ActivityNewsDetailBinding binding;
    private TxViewModel txViewModel;
    private UserViewModel userViewModel;
    private NewsListAdapter adapter;
    private FloatMenu operationsMenu;
    private PopUpDialog retweetDialog;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private String txID;
    private UserAndTx tx;
    private int currentPos = 0;
    private boolean isScrollToTop = true;
    private boolean isLoadMore = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        txViewModel = provider.get(TxViewModel.class);
        txViewModel.observeNeedStartDaemon();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_news_detail);
        initParam();
        initView();
        initRefreshLayout();
    }

    /**
     * 初始化参数
     */
    private void initParam() {
        if (getIntent() != null) {
            txID = getIntent().getStringExtra(IntentExtra.ID);
        }
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.main_title);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        binding.news.ivBan.setVisibility(View.INVISIBLE);
        binding.news.ivLongPress.setVisibility(View.INVISIBLE);
        binding.news.ivArrow.setVisibility(View.INVISIBLE);

        if (getRecyclerView() != null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            getRecyclerView().setLayoutManager(layoutManager);
            getRecyclerView().setItemAnimator(null);
        }
        adapter = new NewsListAdapter(this, true);
        binding.txList.setAdapter(adapter);
    }

    private void initRefreshLayout() {
        binding.refreshLayout.setDelegate(this);
        BGAStickinessRefreshViewHolder refreshViewHolder = new BGAStickinessRefreshViewHolder(getApplicationContext(), true);
        refreshViewHolder.setRotateImage(R.mipmap.ic_launcher_foreground);
        refreshViewHolder.setStickinessColor(R.color.color_yellow);

        refreshViewHolder.setLoadingMoreText(getString(R.string.common_loading));
        binding.refreshLayout.setPullDownRefreshEnable(false);

        binding.refreshLayout.setRefreshViewHolder(refreshViewHolder);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Disposable disposable = txViewModel.observeNewsDetail(txID)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::showNewsDetail);
        disposables.add(disposable);

        txViewModel.observerChainTxs().observe(this, txs -> {
            List<UserAndTx> currentList = new ArrayList<>(txs);
            int size;
            if (currentPos == 0) {
                initScrollToTop();
                adapter.submitList(currentList, handleUpdateAdapter);
                size = currentList.size();
                isLoadMore = size != 0 && size % Page.PAGE_SIZE == 0;
            } else {
                currentList.addAll(0, adapter.getCurrentList());
                adapter.submitList(currentList, handlePullAdapter);
                isLoadMore = txs.size() != 0 && txs.size() % Page.PAGE_SIZE == 0;
            }
            binding.refreshLayout.endLoadingMore();
        });
        loadData(0);

        disposables.add(txViewModel.observeDataSetChanged()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    // 跟当前用户有关系的才触发刷新
                    if (result != null && StringUtil.isNotEmpty(result.getMsg())) {
                        // 立即执行刷新
                        loadData(0);
                    }
                }));
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    private void showNewsDetail(UserAndTx tx) {
        if (null == this.tx) {
            binding.news.ivHeadPic.setImageBitmap(UsersUtil.getHeadPic(tx.sender));
            String userName = UsersUtil.getShowName(tx.sender);
            userName = null == userName ? "" : userName;
            String communityName = ChainIDUtil.getName(tx.chainID);
            String communityCode = ChainIDUtil.getCode(tx.chainID);
            int nameColor = binding.getRoot().getResources().getColor(R.color.color_black);
            SpannableStringBuilder name = new SpanUtils()
                    .append(userName)
                    .setForegroundColor(nameColor)
                    .append("(")
                    .append(UsersUtil.getLastPublicKey(tx.senderPk, 4))
                    .append(")")
                    .append("@")
                    .append(communityName)
                    .append("(").append(communityCode).append(")")
                    .append(" · ")
                    .append(DateUtil.getNewsTime(tx.timestamp))
                    .create();
            binding.news.tvName.setText(name);

            double showPower = Logarithm.log2(2 + tx.power);
            String power = FmtMicrometer.formatThreeDecimal(showPower);
            String balance = FmtMicrometer.fmtBalance(tx.getDisplayBalance());
            binding.news.tvBalance.setText(balance);
            binding.news.tvPower.setText(power);

            binding.news.tvMsg.setText(TxUtils.createTxSpan(tx, CommunityTabFragment.TAB_NEWS));
            // 添加link解析
            binding.news.tvMsg.setAutoLinkMask(0);
            Linkify.addLinks(binding.news.tvMsg, Linkify.WEB_URLS);
            Pattern referral = Pattern.compile(LinkUtil.REFERRAL_PATTERN, 0);
            Linkify.addLinks(binding.news.tvMsg, referral, null);
            Pattern airdrop = Pattern.compile(LinkUtil.AIRDROP_PATTERN, 0);
            Linkify.addLinks(binding.news.tvMsg, airdrop, null);
            Pattern chain = Pattern.compile(LinkUtil.CHAIN_PATTERN, 0);
            Linkify.addLinks(binding.news.tvMsg, chain, null);
            Pattern friend = Pattern.compile(LinkUtil.FRIEND_PATTERN, 0);
            Linkify.addLinks(binding.news.tvMsg, friend, null);

            boolean isShowLink = StringUtil.isNotEmpty(tx.link);
            binding.news.tvLink.setText(tx.link);
            binding.news.tvLink.setVisibility(isShowLink ? View.VISIBLE : View.GONE);
            if (isShowLink) {
                DrawablesUtil.setUnderLine(binding.news.tvLink);
                int linkDrawableSize = binding.getRoot().getResources().getDimensionPixelSize(R.dimen.widget_size_14);
                DrawablesUtil.setEndDrawable(binding.news.tvLink, R.mipmap.icon_share_link, linkDrawableSize);
            }
            setClickListener(binding.news, tx);
            setAutoLinkListener(binding.news.tvMsg, tx);
        }
        if (null == this.tx || this.tx.repliesNum != tx.repliesNum) {
            binding.news.tvRepliesNum.setText(FmtMicrometer.fmtLong(tx.repliesNum));
        }
        if (null == this.tx || this.tx.chatsNum != tx.chatsNum) {
            binding.news.tvChatNum.setText(FmtMicrometer.fmtLong(tx.chatsNum));
        }
        this.tx = tx;
    }

    public void setAutoLinkListener(AutoLinkTextView autoLinkTextView, UserAndTx tx) {
        AutoLinkTextView.AutoLinkListener autoLinkListener = new AutoLinkTextView.AutoLinkListener() {

            @Override
            public void onClick(AutoLinkTextView view) {

            }

            @Override
            public void onLongClick(AutoLinkTextView view) {

            }

            @Override
            public void onLinkClick(String link) {
                KeyboardUtils.hideSoftInput(NewsDetailActivity.this);
                ActivityUtil.openUri(NewsDetailActivity.this, link);
            }
        };
        autoLinkTextView.setAutoLinkListener(autoLinkListener);
    }

    private void setClickListener(ItemNewsBinding binding, UserAndTx tx) {

        binding.ivRetweet.setOnClickListener(view -> {
            if (retweetDialog != null && retweetDialog.isShowing()) {
                retweetDialog.closeDialog();
            }
            retweetDialog = new PopUpDialog.Builder(this)
                    .addItems(R.mipmap.icon_retweet, getString(R.string.common_retweet))
                    .addItems(R.mipmap.icon_share_gray, getString(R.string.common_share))
                    .setOnItemClickListener((dialog, name, code) -> {
                        dialog.cancel();
                        if (code == R.mipmap.icon_retweet) {
                            Intent intent = new Intent();
                            intent.putExtra(IntentExtra.DATA, TxUtils.createTxSpan(tx, CommunityTabFragment.TAB_NEWS));
                            intent.putExtra(IntentExtra.LINK, tx.link);
                            ActivityUtil.startActivity(intent, this, NewsCreateActivity.class);
                        } else if (code == R.mipmap.icon_share_gray) {
                            String text = tx.memo;
                            if (StringUtil.isNotEmpty(tx.link)) {
                                text = tx.memo + "\n" + tx.link;
                            }
                            ActivityUtil.shareText(this, getString(R.string.app_share_news), text);
                        }
                    })
                    .create();
            retweetDialog.show();
        });
        binding.ivReply.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.CHAIN_ID, tx.chainID);
            intent.putExtra(IntentExtra.HASH, tx.txID);
            ActivityUtil.startActivity(intent, this, NewsCreateActivity.class);
        });
        binding.tvLink.setOnClickListener(view -> {
            KeyboardUtils.hideSoftInput(this);
            ActivityUtil.openUri(this, tx.link);
        });
        binding.ivChat.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.CHAIN_ID, tx.chainID);
            intent.putExtra(IntentExtra.HASH, tx.txID);
            ActivityUtil.startActivity(intent, this, CommunityChatActivity.class);
        });
    }


    final Runnable handleUpdateAdapter = () -> {
        if (null == getRecyclerView()) {
            return;
        }
        LinearLayoutManager layoutManager = (LinearLayoutManager) getRecyclerView().getLayoutManager();
        if (layoutManager != null) {
            logger.debug("handleUpdateAdapter isScrollToTop::{}", isScrollToTop);
            if (isScrollToTop) {
                isScrollToTop = false;
                logger.debug("handleUpdateAdapter scrollToPosition::{}", 0);
                layoutManager.scrollToPositionWithOffset(0, Integer.MIN_VALUE);
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

    private int getItemCount() {
        if (adapter != null) {
            return adapter.getItemCount();
        }
        return 0;
    }

    private void loadData(int pos) {
        currentPos = pos;
        txViewModel.loadNewsRepliesData(txID, pos, getItemCount());
    }

    @Override
    public void onBGARefreshLayoutBeginRefreshing(BGARefreshLayout refreshLayout) {
        refreshLayout.endRefreshing();
        refreshLayout.setPullDownRefreshEnable(false);
    }

    @Override
    public boolean onBGARefreshLayoutBeginLoadingMore(BGARefreshLayout refreshLayout) {
        logger.debug("LoadingMore isLoadMore::{}", isLoadMore);
        if (isLoadMore) {
            loadData(getItemCount());
            return true;
        } else {
            refreshLayout.endLoadingMore();
            return false;
        }
    }

    public RecyclerView getRecyclerView() {
        return binding.txList;
    }

    public BGARefreshLayout getRefreshLayout() {
        return binding.refreshLayout;
    }

    void initScrollToTop() {
        if (!isScrollToTop) {
            if (null == getRecyclerView()) {
                return;
            }
            LinearLayoutManager layoutManager = (LinearLayoutManager) getRecyclerView().getLayoutManager();
            if (layoutManager != null) {
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                this.isScrollToTop = firstVisibleItemPosition < 2;
                logger.debug("handleUpdateAdapter firstVisibleItemPosition::{}, isScrollToTop::{}",
                        firstVisibleItemPosition, isScrollToTop);
            }
        }
    }

    @Override
    public void onUserClicked(String publicKey) {
        KeyboardUtils.hideSoftInput(this);
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.PUBLIC_KEY, publicKey);
        ActivityUtil.startActivity(intent, this, UserDetailActivity.class);
    }

    @Override
    public void onItemLongClicked(TextView view, UserAndTx tx) {
        KeyboardUtils.hideSoftInput(this);
        List<OperationMenuItem> menuList = new ArrayList<>();
        menuList.add(new OperationMenuItem(R.string.tx_operation_copy));
        menuList.add(new OperationMenuItem(R.string.tx_operation_reply));
        if (tx != null && StringUtil.isNotEmpty(tx.link)) {
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
                    CopyManager.copyText(tx.memo);
                    ToastUtils.showShortToast(R.string.copy_successfully);
                    break;
                case R.string.tx_operation_reply:
                    Intent intent = new Intent();
                    intent.putExtra(IntentExtra.CHAIN_ID, tx.chainID);
                    intent.putExtra(IntentExtra.HASH, this.txID);
                    intent.putExtra(IntentExtra.PUBLIC_KEY, tx.senderPk);
                    ActivityUtil.startActivity(intent, this, NewsCreateActivity.class);
                    break;
                case R.string.tx_operation_copy_link:
                    CopyManager.copyText(tx.link);
                    ToastUtils.showShortToast(R.string.copy_link_successfully);
                    break;
                case R.string.tx_operation_blacklist:
                    KeyboardUtils.hideSoftInput(this);
                    String showName = UsersUtil.getShowName(tx.sender, tx.senderPk);
                    userViewModel.showBanDialog(this, tx.senderPk, showName);
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
    public void onBanClicked(UserAndTx tx) {
        KeyboardUtils.hideSoftInput(this);
        String showName = UsersUtil.getShowName(tx.sender, tx.senderPk);
        userViewModel.showBanDialog(this, tx.senderPk, showName);
    }

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
}