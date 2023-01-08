package io.taucoin.news.publishing.ui.transaction;

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
import io.taucoin.news.publishing.MainApplication;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.model.data.CommunityAndMember;
import io.taucoin.news.publishing.core.model.data.OperationMenuItem;
import io.taucoin.news.publishing.core.model.data.UserAndTx;
import io.taucoin.news.publishing.core.utils.ActivityUtil;
import io.taucoin.news.publishing.core.utils.CopyManager;
import io.taucoin.news.publishing.core.utils.KeyboardUtils;
import io.taucoin.news.publishing.core.utils.LinkUtil;
import io.taucoin.news.publishing.core.utils.ObservableUtil;
import io.taucoin.news.publishing.core.utils.StringUtil;
import io.taucoin.news.publishing.core.utils.ToastUtils;
import io.taucoin.news.publishing.core.utils.UsersUtil;
import io.taucoin.news.publishing.core.utils.media.MediaUtil;
import io.taucoin.news.publishing.databinding.FragmentTxsMarketTabBinding;
import io.taucoin.news.publishing.ui.BaseActivity;
import io.taucoin.news.publishing.ui.BaseFragment;
import io.taucoin.news.publishing.ui.TauNotifier;
import io.taucoin.news.publishing.ui.community.CommunityViewModel;
import io.taucoin.news.publishing.ui.constant.IntentExtra;
import io.taucoin.news.publishing.ui.constant.Page;
import io.taucoin.news.publishing.ui.customviews.CustomXRefreshViewFooter;
import io.taucoin.news.publishing.ui.customviews.PopUpDialog;
import io.taucoin.news.publishing.ui.user.UserDetailActivity;
import io.taucoin.news.publishing.ui.user.UserViewModel;

import static io.taucoin.news.publishing.ui.transaction.CommunityTabFragment.TAB_MARKET;

/**
 * Market Tab页
 */
public class SearchNewsFragment extends BaseFragment implements View.OnClickListener, NewsListAdapter.ClickListener,
        XRefreshView.XRefreshViewListener {

    private static final Logger logger = LoggerFactory.getLogger("SearchNewsFragment");
    private NewsListAdapter adapter;
    private FragmentTxsMarketTabBinding binding;
    private TxViewModel txViewModel;
    private UserViewModel userViewModel;
    protected CompositeDisposable disposables = new CompositeDisposable();
    private CommunityViewModel communityViewModel;
    protected BaseActivity activity;
    private FloatMenu operationsMenu;
    private PopUpDialog retweetDialog;
    private String keywords;
    private boolean isVisibleToUser;

    private int currentPos = 0;
    private int currentTab = TAB_MARKET;
    private boolean isScrollToTop = true;
    private boolean isLoadMore = false;
    private boolean dataChanged = false;

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
            keywords = getArguments().getString(IntentExtra.DATA);
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
        adapter = new NewsListAdapter(this);
        binding.txList.setAdapter(adapter);
        binding.refreshLayout.setXRefreshViewListener(this);
        binding.refreshLayout.setPullRefreshEnable(false);
        binding.refreshLayout.setPullLoadEnable(true);
        binding.refreshLayout.setMoveForHorizontal(true);
        binding.refreshLayout.setAutoLoadMore(true);

        CustomXRefreshViewFooter footer = new CustomXRefreshViewFooter(getContext());
        binding.refreshLayout.setCustomFooterView(footer);

        txViewModel.observerChainTxs().observe(getViewLifecycleOwner(), txs -> {
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

    @Override
    public void onStart() {
        super.onStart();
        txViewModel.getDeletedResult().observe(this, isSuccess -> {
            loadData(0);
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
                        dataChanged = true;
                    }
                }));
    }

    public void showOrHideLowLinkedView(boolean show) {
        if (null == binding) {
            return;
        }
        binding.llLowLinked.setVisibility(show ? View.VISIBLE : View.GONE);
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeAllDialog();
    }

    @Override
    public void onClick(View v) {
        closeAllDialog();
    }

    int getItemCount() {
        if (adapter != null) {
            return adapter.getItemCount();
        }
        return 0;
    }

    public void loadData(int pos) {
        currentPos = pos;
        txViewModel.loadSearchNewsData(keywords, pos, getItemCount());
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
        if (retweetDialog != null && retweetDialog.isShowing()) {
            retweetDialog.closeDialog();
        }
        retweetDialog = new PopUpDialog.Builder(activity)
                .addItems(R.mipmap.icon_retwitt, getString(R.string.common_retweet_other))
                .addItems(R.mipmap.icon_share_gray, getString(R.string.common_share_external))
                .setOnItemClickListener((dialog, name, code) -> {
                    dialog.cancel();
                    if (code == R.mipmap.icon_retwitt) {
                        Intent intent = new Intent();
                        intent.putExtra(IntentExtra.DATA, tx.memo);
                        intent.putExtra(IntentExtra.LINK, tx.link);
                        ActivityUtil.startActivity(intent, activity, NewsCreateActivity.class);
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
}
