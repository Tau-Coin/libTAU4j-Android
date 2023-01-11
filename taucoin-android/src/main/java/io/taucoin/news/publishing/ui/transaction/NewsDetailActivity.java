package io.taucoin.news.publishing.ui.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.util.Linkify;
import android.view.LayoutInflater;
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
import io.taucoin.news.publishing.MainApplication;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.model.data.OperationMenuItem;
import io.taucoin.news.publishing.core.model.data.UserAndTx;
import io.taucoin.news.publishing.core.utils.ActivityUtil;
import io.taucoin.news.publishing.core.utils.ChainIDUtil;
import io.taucoin.news.publishing.core.utils.CopyManager;
import io.taucoin.news.publishing.core.utils.DateUtil;
import io.taucoin.news.publishing.core.utils.DrawablesUtil;
import io.taucoin.news.publishing.core.utils.FmtMicrometer;
import io.taucoin.news.publishing.core.utils.KeyboardUtils;
import io.taucoin.news.publishing.core.utils.LinkUtil;
import io.taucoin.news.publishing.core.utils.Logarithm;
import io.taucoin.news.publishing.core.utils.ObservableUtil;
import io.taucoin.news.publishing.core.utils.SpanUtils;
import io.taucoin.news.publishing.core.utils.StringUtil;
import io.taucoin.news.publishing.core.utils.ToastUtils;
import io.taucoin.news.publishing.core.utils.UsersUtil;
import io.taucoin.news.publishing.core.utils.media.MediaUtil;
import io.taucoin.news.publishing.databinding.ActivityNewsDetailBinding;
import io.taucoin.news.publishing.databinding.ItemNewsBinding;
import io.taucoin.news.publishing.databinding.ItemNewsDetailHeaderBinding;
import io.taucoin.news.publishing.ui.BaseActivity;
import io.taucoin.news.publishing.ui.community.CommunityViewModel;
import io.taucoin.news.publishing.ui.constant.IntentExtra;
import io.taucoin.news.publishing.ui.constant.Page;
import io.taucoin.news.publishing.ui.customviews.AutoLinkTextView;
import io.taucoin.news.publishing.ui.customviews.PopUpDialog;
import io.taucoin.news.publishing.ui.user.UserDetailActivity;
import io.taucoin.news.publishing.ui.user.UserViewModel;

/**
 * news detail
 */
public class NewsDetailActivity extends BaseActivity implements ReplyListAdapter.ClickListener,
        BGARefreshLayout.BGARefreshLayoutDelegate {
    private ActivityNewsDetailBinding binding;
    private TxViewModel txViewModel;
    private UserViewModel userViewModel;
    private CommunityViewModel communityViewModel;
    private ReplyListAdapter adapter;
    private FloatMenu operationsMenu;
    private PopUpDialog retweetDialog;
    private ItemNewsDetailHeaderBinding headerBinding;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private String txID;
    private UserAndTx tx;
    private int currentPos = 0;
    private boolean isScrollToTop = true;
    private boolean isLoadMore = false;
    private boolean dataChanged = false;
    private String newTxID;
    private boolean isLocateTx = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        txViewModel = provider.get(TxViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
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
            txID = getIntent().getStringExtra(IntentExtra.HASH);
            newTxID = getIntent().getStringExtra(IntentExtra.ID);
        }
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.main_title);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        if (getRecyclerView() != null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            getRecyclerView().setLayoutManager(layoutManager);
            getRecyclerView().setItemAnimator(null);
        }
        adapter = new ReplyListAdapter(this);
        binding.txList.setAdapter(adapter);

        headerBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.item_news_detail_header, null, false);
        headerBinding.news.ivBan.setVisibility(View.GONE);
        headerBinding.news.ivLongPress.setVisibility(View.INVISIBLE);
        headerBinding.news.ivArrow.setVisibility(View.INVISIBLE);
        binding.txList.addHeaderView(headerBinding.getRoot());
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
                isLocateTx = adapter.getCurrentList().size() == 0;
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

        disposables.add(ObservableUtil.intervalSeconds(1)
                .subscribeOn(Schedulers.io())
                .subscribe(o -> {
                    if (dataChanged) {
                        loadData(0);
                        dataChanged = false;
                    }
                }));

        disposables.add(txViewModel.observeDataSetChanged()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    // 跟当前用户有关系的才触发刷新
                    if (result != null && StringUtil.isNotEmpty(result.getMsg())) {
                        // 立即执行刷新
                        dataChanged = true;
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
            headerBinding.news.ivHeadPic.setImageBitmap(UsersUtil.getHeadPic(tx.sender));
            String userName = UsersUtil.getShowName(tx.sender);
            userName = null == userName ? "" : userName;
            String communityName = ChainIDUtil.getName(tx.chainID);
            String communityCode = ChainIDUtil.getCode(tx.chainID);
            int nameColor = getResources().getColor(R.color.color_black);
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
            headerBinding.news.tvName.setText(name);

            double showPower = Logarithm.log2(2 + tx.power);
            String power = FmtMicrometer.formatThreeDecimal(showPower);
            String balance = FmtMicrometer.fmtBalance(tx.getInterimBalance());
            headerBinding.news.tvBalance.setText(balance);
            headerBinding.news.tvPower.setText(power);

            headerBinding.news.tvMsg.setText(tx.memo);
            // 添加link解析
            headerBinding.news.tvMsg.setAutoLinkMask(0);
            Linkify.addLinks(headerBinding.news.tvMsg, Linkify.WEB_URLS);
            Pattern referral = Pattern.compile(LinkUtil.REFERRAL_PATTERN, 0);
            Linkify.addLinks(headerBinding.news.tvMsg, referral, null);
            Pattern airdrop = Pattern.compile(LinkUtil.AIRDROP_PATTERN, 0);
            Linkify.addLinks(headerBinding.news.tvMsg, airdrop, null);
            Pattern chain = Pattern.compile(LinkUtil.CHAIN_PATTERN, 0);
            Linkify.addLinks(headerBinding.news.tvMsg, chain, null);
            Pattern friend = Pattern.compile(LinkUtil.FRIEND_PATTERN, 0);
            Linkify.addLinks(headerBinding.news.tvMsg, friend, null);

            boolean isShowLink = StringUtil.isNotEmpty(tx.link);
            headerBinding.news.tvLink.setText(tx.link);
            headerBinding.news.tvLink.setVisibility(isShowLink ? View.VISIBLE : View.GONE);
            if (isShowLink) {
                DrawablesUtil.setUnderLine(headerBinding.news.tvLink);
                int linkDrawableSize = binding.getRoot().getResources().getDimensionPixelSize(R.dimen.widget_size_14);
                DrawablesUtil.setEndDrawable(headerBinding.news.tvLink, R.mipmap.icon_share_link, linkDrawableSize);
            }
            boolean isHavePicture = StringUtil.isNotEmpty(tx.picturePath);
            headerBinding.news.ivPicture.setEnabled(isHavePicture);
            headerBinding.news.ivPicture.setImageResource(isHavePicture ? R.mipmap.icon_picture_gray : R.mipmap.icon_picture_white);
            setClickListener(headerBinding.news, tx);
            setAutoLinkListener(headerBinding.news.tvMsg, tx);
        }
        if (null == this.tx || this.tx.repliesNum != tx.repliesNum) {
            headerBinding.news.tvRepliesNum.setText(FmtMicrometer.fmtLong(tx.repliesNum));
        }
        if (null == this.tx || this.tx.chatsNum != tx.chatsNum) {
            headerBinding.news.tvChatNum.setText(FmtMicrometer.fmtLong(tx.chatsNum));
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

        binding.ivDelete.setOnClickListener(view -> {
            txViewModel.deleteThisNews(this, tx.txID);
        });
        txViewModel.getDeletedResult().observe(this, isSuccess -> {
            finish();
        });
        binding.ivPicture.setOnClickListener(view -> {
            MediaUtil.previewPicture(this, tx.picturePath);
        });
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


    final Runnable handleUpdateAdapter = () -> {
        if (null == getRecyclerView()) {
            return;
        }
        LinearLayoutManager layoutManager = (LinearLayoutManager) getRecyclerView().getLayoutManager();
        if (layoutManager != null) {
            logger.debug("handleUpdateAdapter isScrollToTop::{}", isScrollToTop);
            if (isLocateTx && StringUtil.isNotEmpty(newTxID)) {
                isScrollToTop = false;
                isLocateTx = false;
                List<UserAndTx> currentList = adapter.getCurrentList();
                for (int i = 0; i < currentList.size(); i++) {
                    if (StringUtil.isEquals(newTxID, currentList.get(i).txID)) {
                        layoutManager.scrollToPositionWithOffset(i, Integer.MIN_VALUE);
                        break;
                    }
                }
                return;
            }
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
//        LinearLayoutManager layoutManager = (LinearLayoutManager) getRecyclerView().getLayoutManager();
//        if (layoutManager != null) {
//            int bottomPosition = getItemCount() - 1;
//            int position = bottomPosition - currentPos;
//            layoutManager.scrollToPositionWithOffset(position, 0);
//        }
    };

    private int getItemCount() {
        if (adapter != null) {
            return adapter.getItemCount();
        }
        return 0;
    }

    private void loadData(int pos) {
        currentPos = pos;
        txViewModel.loadNewsRepliesData(txID, pos, getItemCount(), newTxID);
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