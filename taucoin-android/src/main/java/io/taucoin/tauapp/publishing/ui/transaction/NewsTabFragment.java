package io.taucoin.tauapp.publishing.ui.transaction;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import cn.bingoogolapple.refreshlayout.BGARefreshLayout;
import cn.bingoogolapple.refreshlayout.BGAStickinessRefreshViewHolder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.tauapp.publishing.MainApplication;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.model.data.OperationMenuItem;
import io.taucoin.tauapp.publishing.core.model.data.UserAndTx;
import io.taucoin.tauapp.publishing.core.utils.ActivityUtil;
import io.taucoin.tauapp.publishing.core.utils.CopyManager;
import io.taucoin.tauapp.publishing.core.utils.KeyboardUtils;
import io.taucoin.tauapp.publishing.core.utils.ObservableUtil;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.core.utils.ToastUtils;
import io.taucoin.tauapp.publishing.core.utils.UsersUtil;
import io.taucoin.tauapp.publishing.databinding.FragmentNewsTabBinding;
import io.taucoin.tauapp.publishing.ui.BaseActivity;
import io.taucoin.tauapp.publishing.ui.BaseFragment;
import io.taucoin.tauapp.publishing.ui.community.CommunityViewModel;
import io.taucoin.tauapp.publishing.ui.constant.IntentExtra;
import io.taucoin.tauapp.publishing.ui.constant.Page;
import io.taucoin.tauapp.publishing.ui.customviews.PopUpDialog;
import io.taucoin.tauapp.publishing.ui.main.MainActivity;
import io.taucoin.tauapp.publishing.ui.user.UserDetailActivity;
import io.taucoin.tauapp.publishing.ui.user.UserViewModel;

/**
 * 主页所有上链news或notes, 以及自己发的未上链news Tab页
 */
public class NewsTabFragment extends BaseFragment implements View.OnClickListener, NewsListAdapter.ClickListener,
        BGARefreshLayout.BGARefreshLayoutDelegate {

    protected static final Logger logger = LoggerFactory.getLogger("NewsTabFragment");
    private NewsListAdapter adapter;
    private FragmentNewsTabBinding binding;
    protected CompositeDisposable disposables = new CompositeDisposable();
    private boolean isVisibleToUser;
    public static final int TX_REQUEST_CODE = 0x1002;
    public static final int CHOOSE_REQUEST_CODE = 0x1003;
    protected BaseActivity activity;
    protected TxViewModel txViewModel;
    protected UserViewModel userViewModel;
    protected CommunityViewModel communityViewModel;
    private FloatMenu operationsMenu;
    private PopUpDialog retweetDialog;

    private int currentPos = 0;
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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_news_tab, container, false);
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
        initView();
        initRefreshLayout();
    }

    private void initRefreshLayout() {
        binding.refreshLayout.setDelegate(this);
        BGAStickinessRefreshViewHolder refreshViewHolder = new BGAStickinessRefreshViewHolder(activity.getApplicationContext(), true);
        refreshViewHolder.setRotateImage(R.mipmap.ic_launcher_foreground);
        refreshViewHolder.setStickinessColor(R.color.color_yellow);

        refreshViewHolder.setLoadingMoreText(getString(R.string.common_loading));
        binding.refreshLayout.setPullDownRefreshEnable(false);

        binding.refreshLayout.setRefreshViewHolder(refreshViewHolder);
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

        initFabSpeedDial();
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

    public RecyclerView getRecyclerView() {
        return binding.txList;
    }

    public BGARefreshLayout getRefreshLayout() {
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
            Intent intent = new Intent();
            ActivityUtil.startActivity(intent, activity, NewsCreateActivity.class);
        });

//        Intent intent = new Intent();
//        binding.fabButton.setOnActionSelectedListener(actionItem -> {
//            switch (actionItem.getId()) {
//                case R.id.community_create_sell:
//                    ActivityUtil.startActivityForResult(intent, activity, SellCreateActivity.class,
//                            TX_REQUEST_CODE);
//                    break;
//                case R.id.community_create_airdrop:
//                    ActivityUtil.startActivityForResult(intent, activity, AirdropCreateActivity.class,
//                            TX_REQUEST_CODE);
//                    break;
//                case R.id.community_create_invitation:
//                    ActivityUtil.startActivityForResult(intent, activity, AnnouncementCreateActivity.class,
//                            TX_REQUEST_CODE);
//                    break;
//            }
//            return false;
//        });
    }

    @Override
    public void onResume() {
        super.onResume();
        this.isVisibleToUser = true;
        if (communityViewModel != null && isVisibleToUser) {
            communityViewModel.clearNewsUnread();
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
            if (isVisibleToUser) {
                communityViewModel.clearNewsUnread();
            }
//            logger.debug("txs.size::{}", txs.size());
//            closeProgressDialog();\
//            TauNotifier.getInstance().cancelNotify(chainID);
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

        disposables.add(txViewModel.observeLatestPinnedMsg()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    boolean isHavePinnedMsg = list != null && list.size() > 0;
                    binding.llPinnedMessage.setVisibility(isHavePinnedMsg ? View.VISIBLE : View.GONE);
                    if (isHavePinnedMsg) {
                        binding.tvPinnedContent.setText(TxUtils.createTxSpan(list.get(0)));
                    }
                }));
    }

    @Override
    public void onClick(View v) {
        closeAllDialog();
        switch (v.getId()) {
            case R.id.ll_pinned_message:
                ActivityUtil.startActivity(activity, PinnedActivity.class);
                break;
        }
    }

    private int getItemCount() {
        if (adapter != null) {
            return adapter.getItemCount();
        }
        return 0;
    }

    private void loadData(int pos) {
        currentPos = pos;
        txViewModel.loadNewsData(pos, getItemCount());
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
        menuList.add(new OperationMenuItem(R.string.tx_operation_msg_hash));

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
                    KeyboardUtils.hideSoftInput(activity);
                    String showName = UsersUtil.getShowName(tx.sender, tx.senderPk);
                    userViewModel.showBanDialog(activity, tx.senderPk, showName);
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
    public void onRetweetClicked(UserAndTx tx) {
        if (retweetDialog != null && retweetDialog.isShowing()) {
            retweetDialog.closeDialog();
        }
        retweetDialog = new PopUpDialog.Builder(activity)
                .addItems(R.mipmap.icon_retwitt, getString(R.string.common_retweet))
                .addItems(R.mipmap.icon_share_gray, getString(R.string.common_share))
                .setOnItemClickListener((dialog, name, code) -> {
                    dialog.cancel();
                    if (code == R.mipmap.icon_retwitt) {
                        Intent intent = new Intent();
                        intent.putExtra(IntentExtra.DATA, TxUtils.createTxSpan(tx, CommunityTabFragment.TAB_NEWS));
                        intent.putExtra(IntentExtra.LINK, tx.link);
                        ActivityUtil.startActivityForResult(intent, activity, NewsCreateActivity.class,
                                CHOOSE_REQUEST_CODE);
                    } else if (code == R.mipmap.icon_share_gray) {
                        String text = tx.memo;
                        if (StringUtil.isNotEmpty(tx.link)) {
                            text = tx.memo + "\n" + tx.link;
                        }
                        ActivityUtil.shareText(activity, getString(R.string.app_share_news), text);
                    }
                })
                .create();
        retweetDialog.show();
    }

    @Override
    public void onReplyClicked(UserAndTx tx) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.CHAIN_ID, tx.chainID);
        intent.putExtra(IntentExtra.HASH, tx.txID);
        ActivityUtil.startActivityForResult(intent, activity, NewsCreateActivity.class,
                TX_REQUEST_CODE);
    }

    @Override
    public void onChatClicked(UserAndTx tx) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.CHAIN_ID, tx.chainID);
        intent.putExtra(IntentExtra.HASH, tx.txID);
        ActivityUtil.startActivity(intent, activity, CommunityChatActivity.class);
    }

    @Override
    public void onBanClicked(UserAndTx tx) {
        KeyboardUtils.hideSoftInput(activity);
        String showName = UsersUtil.getShowName(tx.sender, tx.senderPk);
        userViewModel.showBanDialog(activity, tx.senderPk, showName);
    }

    @Override
    public void onItemClicked(UserAndTx tx) {
        KeyboardUtils.hideSoftInput(activity);
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.ID, tx.txID);
        ActivityUtil.startActivity(intent, activity, NewsDetailActivity.class);
    }

    @Override
    public void onLinkClick(String link) {
        KeyboardUtils.hideSoftInput(activity);
        ActivityUtil.openUri(activity, link);
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
    public void onFragmentResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onFragmentResult(requestCode, resultCode, data);
        if (requestCode == TX_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            loadData(0);
        }
        if (resultCode == Activity.RESULT_OK && requestCode == CHOOSE_REQUEST_CODE) {
            if (data != null) {
                String chainID = data.getStringExtra(IntentExtra.CHAIN_ID);
                if (StringUtil.isNotEmpty(chainID)) {
                    Intent intent = new Intent();
                    intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(IntentExtra.TYPE, 0);
                    ActivityUtil.startActivity(intent, this, MainActivity.class);
                }
            }
        }
    }
}
