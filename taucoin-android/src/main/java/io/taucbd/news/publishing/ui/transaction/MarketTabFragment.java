package io.taucbd.news.publishing.ui.transaction;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.andview.refreshview.XRefreshView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.noober.menu.FloatMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucbd.news.publishing.MainApplication;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.model.data.CommunityAndMember;
import io.taucbd.news.publishing.core.model.data.OperationMenuItem;
import io.taucbd.news.publishing.core.model.data.UserAndTx;
import io.taucbd.news.publishing.core.utils.ActivityUtil;
import io.taucbd.news.publishing.core.utils.CopyManager;
import io.taucbd.news.publishing.core.utils.FixMemLeak;
import io.taucbd.news.publishing.core.utils.KeyboardUtils;
import io.taucbd.news.publishing.core.utils.LinkUtil;
import io.taucbd.news.publishing.core.utils.ObservableUtil;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.core.utils.ToastUtils;
import io.taucbd.news.publishing.core.utils.UsersUtil;
import io.taucbd.news.publishing.core.utils.media.MediaUtil;
import io.taucbd.news.publishing.databinding.FragmentTxsMarketTabBinding;
import io.taucbd.news.publishing.ui.BaseActivity;
import io.taucbd.news.publishing.ui.BaseFragment;
import io.taucbd.news.publishing.ui.TauNotifier;
import io.taucbd.news.publishing.ui.community.CommunityViewModel;
import io.taucbd.news.publishing.ui.constant.IntentExtra;
import io.taucbd.news.publishing.ui.constant.Page;
import io.taucbd.news.publishing.ui.customviews.CustomXRefreshViewFooter;
import io.taucbd.news.publishing.ui.customviews.PopUpDialog;
import io.taucbd.news.publishing.ui.user.UserDetailActivity;
import io.taucbd.news.publishing.ui.user.UserViewModel;

import static io.taucbd.news.publishing.ui.transaction.CommunityTabFragment.TAB_MARKET;

/**
 * Market Tab页
 */
public class MarketTabFragment extends BaseFragment implements View.OnClickListener, NewsListAdapter.ClickListener,
        XRefreshView.XRefreshViewListener {

    private static final Logger logger = LoggerFactory.getLogger("MarketTabFragment");
    private NewsListAdapter adapter;
    private FragmentTxsMarketTabBinding binding;
    private TxViewModel txViewModel;
    private UserViewModel userViewModel;
    protected CompositeDisposable disposables = new CompositeDisposable();
    private CommunityViewModel communityViewModel;
    protected BaseActivity activity;
    private FloatMenu operationsMenu;
    private PopUpDialog retweetDialog;
    private String chainID;
    private boolean isJoined;
    private boolean isVisibleToUser;

    private int currentPos = 0;
    private int currentTab = TAB_MARKET;
    private boolean isScrollToTop = true;
    private boolean isLoadMore = false;
    private boolean dataChanged = false;
    private int dataTimeCount = 0;
    private boolean isLoadData = false;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof BaseActivity)
            activity = (BaseActivity)context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_txs_market_tab, container, false);
        binding.setListener(this);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activity = (BaseActivity) getActivity();
        assert activity != null;
        ViewModelProvider provider = new ViewModelProvider(this);
        txViewModel = provider.get(TxViewModel.class);
        userViewModel = provider.get(UserViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        initParameter();
        initView();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if(getArguments() != null){
            chainID = getArguments().getString(IntentExtra.CHAIN_ID);
            isJoined = getArguments().getBoolean(IntentExtra.IS_JOINED, false);
        }
    }

    /**
     * 初始化视图
     */
    public void initView() {
        if (getRecyclerView() != null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
//        layoutManager.setStackFromEnd(true);
            getRecyclerView().setLayoutManager(layoutManager);
            getRecyclerView().setItemAnimator(null);
        }
        adapter = new NewsListAdapter(this, binding.txList);
        binding.txList.setAdapter(adapter);
//        binding.txList.setItemViewCacheSize(20);
        binding.txList.setDrawingCacheEnabled(true);
        binding.txList.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        initFabSpeedDial();
        binding.refreshLayout.setXRefreshViewListener(this);
        binding.refreshLayout.setPullRefreshEnable(false);
        binding.refreshLayout.setPullLoadEnable(true);
        binding.refreshLayout.setMoveForHorizontal(true);
        binding.refreshLayout.setAutoLoadMore(true);

        CustomXRefreshViewFooter footer = new CustomXRefreshViewFooter(getContext());
        binding.refreshLayout.setCustomFooterView(footer);

        txViewModel.observerChainTxs().observe(getViewLifecycleOwner(), txs -> {
            if (StringUtil.isEmpty(chainID)) {
                return;
            }
            List<UserAndTx> currentList = new ArrayList<>(txs);
            int size;
            if (currentPos == 0) {
                adapter.submitList(currentList, handleUpdateAdapter);
                size = currentList.size();
                isLoadMore = size != 0 && size % Page.PAGE_SIZE == 0;
            } else {
                currentList.addAll(0, adapter.getCurrentList());
                adapter.submitList(currentList, handlePullAdapter);
                isLoadMore = txs.size() != 0 && txs.size() % Page.PAGE_SIZE == 0;
            }
            binding.refreshLayout.setLoadComplete(!isLoadMore);
            binding.refreshLayout.stopLoadMore();
            if (isVisibleToUser) {
                communityViewModel.clearNewsUnread(chainID);
            }
            logger.debug("txs.size::{}", txs.size());
            closeProgressDialog();
            TauNotifier.getInstance().cancelNotify(chainID);
            binding.refreshLayout.setVisibility(View.VISIBLE);
        });
    }

    private final Runnable handleUpdateAdapter = () -> {
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

    private final Runnable handlePullAdapter = () -> {
        if (null == getRecyclerView()) {
            return;
        }
        int dx = binding.txList.getScrollX();
        int dy = binding.txList.getScrollY();
        int offset = getResources().getDimensionPixelSize(R.dimen.widget_size_100);
        getRecyclerView().smoothScrollBy(dx, dy + offset);
    };

    public RecyclerView getRecyclerView() {
        return binding.txList;
    }

    public XRefreshView getRefreshLayout() {
        return binding.refreshLayout;
    }

    /**
     * 初始化右下角悬浮按钮组件
     */
    private void initFabSpeedDial() {
        FloatingActionButton mainFab = binding.fabButton.getMainFab();
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mainFab.getLayoutParams();
        layoutParams.gravity = Gravity.END | Gravity.BOTTOM;
        mainFab.setLayoutParams(layoutParams);
        mainFab.setCustomSize(getResources().getDimensionPixelSize(R.dimen.widget_size_44));

        binding.fabButton.getMainFab().setOnClickListener(v -> {
            isScrollToTop = true;
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.CHAIN_ID, chainID);
            ActivityUtil.startActivity(intent, activity, NewsCreateActivity.class);
        });
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        this.isVisibleToUser = isVisibleToUser;
        logger.debug("setUserVisibleHint1::{}", isVisibleToUser);
        if (communityViewModel != null && isVisibleToUser && StringUtil.isNotEmpty(chainID)) {
            communityViewModel.clearNewsUnread(chainID);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isHidden()) {
            return;
        }
        if (getUserVisibleHint()) {
            this.isVisibleToUser = true;
        }
        if (communityViewModel != null && isVisibleToUser && StringUtil.isNotEmpty(chainID)) {
            communityViewModel.clearNewsUnread(chainID);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        this.isVisibleToUser = false;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!isLoadData && StringUtil.isNotEmpty(chainID)) {
            isLoadData = true;
            subscribeCommunityViewModel();
        }
    }

    /**
     * 订阅社区相关的被观察者
     */
    private void subscribeCommunityViewModel() {
        if (StringUtil.isEmpty(chainID)) {
            return;
        }
        txViewModel.getDeletedResult().observe(this, isSuccess -> {
            loadData(0);
        });
        loadData(0);
        disposables.add(ObservableUtil.intervalSeconds(1)
                .subscribeOn(Schedulers.io())
                .subscribe(o -> {
                    if (dataChanged || dataTimeCount == 60) {
                        loadData(0);
                        dataChanged = false;
                        dataTimeCount = 0;
                    }
                    dataTimeCount ++;
                }));

        disposables.add(txViewModel.observeDataSetChanged()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    // 跟当前用户有关系的才触发刷新
                    if (result != null && StringUtil.isNotEmpty(result.getMsg())) {
                        dataChanged = true;
                    }
                }));

        disposables.add(txViewModel.observeLatestPinnedMsg(chainID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    boolean isHavePinnedMsg = list != null && list.size() > 0;
                    binding.llPinnedMessage.setVisibility(isHavePinnedMsg ? View.VISIBLE : View.GONE);
                    if (isHavePinnedMsg) {
                        binding.tvPinnedContent.setText(list.get(0).memo);
                    }
                }));
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

    @Override
    public void onStop() {
        super.onStop();
        disposables.clear();
        isLoadData = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeAllDialog();
        if (adapter != null) {
            adapter.onCleared();
        }
        binding.refreshLayout.setXRefreshViewListener(null);
    }

    @Override
    public void onClick(View v) {
        closeAllDialog();
        switch (v.getId()) {
            case R.id.ll_pinned_message:
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                intent.putExtra(IntentExtra.TYPE, currentTab);
                ActivityUtil.startActivityForResult(intent, activity, PinnedActivity.class,
                        NotesTabFragment.TX_REQUEST_CODE);
                break;
        }
    }

    int getItemCount() {
        if (adapter != null) {
            return adapter.getItemCount();
        }
        return 0;
    }

    public void handleMember(CommunityAndMember member) {
        isJoined = member.isJoined();
        int color = !isJoined ? R.color.gray_light : R.color.primary;
        binding.fabButton.setMainFabClosedBackgroundColor(getResources().getColor(color));
    }

    public void loadData(int pos) {
        if (StringUtil.isEmpty(chainID)) {
            return;
        }
        currentPos = pos;
        txViewModel.loadMarketData(chainID, pos, getItemCount());
    }

    /**
     * 显示每个item长按操作选项对话框
     */
    @Override
    public void onItemLongClicked(TextView view, UserAndTx tx) {
        KeyboardUtils.hideSoftInput(activity);
        List<OperationMenuItem> menuList = new ArrayList<>();
        menuList.add(new OperationMenuItem(R.string.tx_operation_copy));
        if (tx != null && StringUtil.isNotEmpty(tx.link)) {
            menuList.add(new OperationMenuItem(R.string.tx_operation_copy_link));
        }
        // 用户不能拉黑自己
        if(StringUtil.isNotEquals(tx.senderPk,
                MainApplication.getInstance().getPublicKey())){
            menuList.add(new OperationMenuItem(R.string.tx_operation_blacklist));
        }
        menuList.add(new OperationMenuItem(tx.pinnedTime <= 0 ? R.string.tx_operation_pin : R.string.tx_operation_unpin));
        if (tx.favoriteTime <= 0) {
            menuList.add(new OperationMenuItem(R.string.tx_operation_favorite));
        }
//        menuList.add(new OperationMenuItem(R.string.tx_operation_msg_hash));

        operationsMenu = new FloatMenu(activity);
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
                case R.string.tx_operation_blacklist:
                    String publicKey = tx.senderPk;
                    userViewModel.setUserBlacklist(publicKey, true);
                    ToastUtils.showShortToast(R.string.blacklist_successfully);
                    break;
                case R.string.tx_operation_pin:
                case R.string.tx_operation_unpin:
                    txViewModel.setMessagePinned(tx, false);
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
        operationsMenu.show(activity.getPoint());
    }

    @Override
    public void onUserClicked(String senderPk) {
        KeyboardUtils.hideSoftInput(activity);
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.PUBLIC_KEY, senderPk);
        ActivityUtil.startActivity(intent, this, UserDetailActivity.class);
    }

//    @Override
//    public void onEditNameClicked(String senderPk){
//        KeyboardUtils.hideSoftInput(activity);
//        String userPk = MainApplication.getInstance().getPublicKey();
//        if (StringUtil.isEquals(userPk, senderPk)) {
//            userViewModel.showEditNameDialog(activity, senderPk);
//        } else {
//            userViewModel.showRemarkDialog(activity, senderPk);
//        }
//    }

    @Override
    public void onBanClicked(UserAndTx tx){
        KeyboardUtils.hideSoftInput(activity);
        String showName = UsersUtil.getShowName(tx.sender, tx.senderPk);
        userViewModel.showBanDialog(activity, tx.senderPk, showName);
    }

    @Override
    public void onItemClicked(UserAndTx tx) {
        KeyboardUtils.hideSoftInput(activity);
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.HASH, tx.txID);
        ActivityUtil.startActivity(intent, activity, NewsDetailActivity.class);
    }

    @Override
    public void onLinkClick(String link) {
        KeyboardUtils.hideSoftInput(activity);
        ActivityUtil.openUri(activity, link);
    }

    @Override
    public void onPicturePreview(String picturePath) {
        MediaUtil.previewPicture(activity, picturePath);
    }

    @Override
    public void onRetweetClicked(UserAndTx tx) {
//        if (retweetDialog != null && retweetDialog.isShowing()) {
//            retweetDialog.closeDialog();
//        }
//        retweetDialog = new PopUpDialog.Builder(activity)
//                .addItems(R.mipmap.icon_retwitt, getString(R.string.common_retweet_other))
//                .addItems(R.mipmap.icon_share_gray, getString(R.string.common_share_external))
//                .setOnItemClickListener((dialog, name, code) -> {
//                    dialog.cancel();
//                    if (code == R.mipmap.icon_retwitt) {
                        Intent intent = new Intent();
                        intent.putExtra(IntentExtra.DATA, tx.memo);
                        intent.putExtra(IntentExtra.LINK, tx.link);
                        intent.putExtra(IntentExtra.PICTURE_PATH, tx.picturePath);
                        ActivityUtil.startActivity(intent, activity, NewsCreateActivity.class);
//                    } else if (code == R.mipmap.icon_share_gray) {
//                        loadChainLink(tx);
//                    }
//                })
//                .create();
//        retweetDialog.show();
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
                    ActivityUtil.shareText(activity, getString(R.string.app_share_news), text);
                }));
    }

    @Override
    public void onReplyClicked(UserAndTx tx) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.CHAIN_ID, tx.chainID);
        intent.putExtra(IntentExtra.HASH, tx.txID);
        ActivityUtil.startActivity(intent, activity, NewsCreateActivity.class);
    }

    @Override
    public void onChatClicked(UserAndTx tx) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.CHAIN_ID, tx.chainID);
        intent.putExtra(IntentExtra.HASH, tx.txID);
        ActivityUtil.startActivity(intent, activity, CommunityChatActivity.class);
    }

    @Override
    public void onDeleteClicked(UserAndTx tx) {
        txViewModel.deleteThisNews(activity, tx.txID);
    }

    @Override
    public void onRefresh(boolean b) {

    }

    @Override
    public void onLoadMore(boolean b) {
        loadData(getItemCount());
    }

    @Override
    public void onRelease(float v) {

    }

    @Override
    public void onHeaderMove(double v, int i) {

    }

    public void onHiddenChanged(boolean hidden, String chainID) {
        logger.debug("onHiddenChanged::{}, chainID::{}", hidden, chainID);
        if (!hidden) {
            isScrollToTop = true;
            isVisibleToUser = true;
            if (StringUtil.isNotEquals(this.chainID, chainID)) {
                adapter.submitList(new ArrayList<>());
            }
            this.chainID = chainID;
            if (!isLoadData) {
                isLoadData = true;
                subscribeCommunityViewModel();
            }
        } else {
            isLoadData = false;
            isVisibleToUser = false;
            disposables.clear();
            closeAllDialog();
            this.chainID = null;
            binding.refreshLayout.setVisibility(View.INVISIBLE);
            binding.llPinnedMessage.setVisibility(View.GONE);
        }
    }
}
