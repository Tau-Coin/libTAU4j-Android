package io.taucoin.news.publishing.ui.transaction;

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
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.news.publishing.MainApplication;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.model.data.OperationMenuItem;
import io.taucoin.news.publishing.core.model.data.UserAndTx;
import io.taucoin.news.publishing.core.model.data.UserAndTxReply;
import io.taucoin.news.publishing.core.utils.ActivityUtil;
import io.taucoin.news.publishing.core.utils.CopyManager;
import io.taucoin.news.publishing.core.utils.KeyboardUtils;
import io.taucoin.news.publishing.core.utils.LinkUtil;
import io.taucoin.news.publishing.core.utils.ObservableUtil;
import io.taucoin.news.publishing.core.utils.StringUtil;
import io.taucoin.news.publishing.core.utils.ToastUtils;
import io.taucoin.news.publishing.core.utils.UsersUtil;
import io.taucoin.news.publishing.core.utils.media.MediaUtil;
import io.taucoin.news.publishing.databinding.FragmentNewsTabBinding;
import io.taucoin.news.publishing.databinding.ItemHomeNewsBinding;
import io.taucoin.news.publishing.ui.BaseActivity;
import io.taucoin.news.publishing.ui.BaseFragment;
import io.taucoin.news.publishing.ui.community.CommunityViewModel;
import io.taucoin.news.publishing.ui.constant.IntentExtra;
import io.taucoin.news.publishing.ui.constant.Page;
import io.taucoin.news.publishing.ui.customviews.CommonDialog;
import io.taucoin.news.publishing.ui.customviews.CustomXRefreshViewFooter;
import io.taucoin.news.publishing.ui.customviews.PopUpDialog;
import io.taucoin.news.publishing.ui.main.MainActivity;
import io.taucoin.news.publishing.ui.user.UserDetailActivity;
import io.taucoin.news.publishing.ui.user.UserViewModel;

/**
 * 主页所有上链news或notes, 以及自己发的未上链news Tab页
 */
public class NewsTabFragment extends BaseFragment implements View.OnClickListener, HomeListAdapter.ClickListener,
        XRefreshView.XRefreshViewListener {

    protected static final Logger logger = LoggerFactory.getLogger("NewsTabFragment");
    private HomeListAdapter adapter;
    private FragmentNewsTabBinding binding;
    protected CompositeDisposable disposables = new CompositeDisposable();
    private boolean isVisibleToUser;
    public static final int TX_REQUEST_CODE = 0x1002;
    public static final int CHOOSE_REQUEST_CODE = 0x1003;
    private MainActivity activity;
    protected TxViewModel txViewModel;
    protected UserViewModel userViewModel;
    protected CommunityViewModel communityViewModel;
    private FloatMenu operationsMenu;
    private PopUpDialog retweetDialog;
    private CommonDialog banCommunityDialog;
    private ItemHomeNewsBinding headerBinding;
    private UserAndTx headerData;

    private int currentPos = 0;
    private boolean isScrollToTop = true;
    private boolean isLoadMore = false;
    private boolean dataChanged = false;
    private int dataTimeCount = 0;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof BaseActivity)
            activity = (MainActivity) context;
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
        activity = (MainActivity) getActivity();
        assert activity != null;
        ViewModelProvider provider = new ViewModelProvider(this);
        txViewModel = provider.get(TxViewModel.class);
        userViewModel = provider.get(UserViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        initView();
    }

    /**
     * 初始化视图
     */
    public void initView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
//      layoutManager.setStackFromEnd(true);
        binding.txList.setLayoutManager(layoutManager);
        binding.txList.setItemAnimator(null);
        adapter = new HomeListAdapter(this);
        binding.txList.setAdapter(adapter);

        initFabSpeedDial();
        initRefreshLayout();

        txViewModel.observerTxsReply().observe(getViewLifecycleOwner(), txs -> {
            List<UserAndTxReply> currentList = new ArrayList<>(txs);
            int size;
            if (currentPos == 0) {
                size = currentList.size();
                if (size <= Page.PAGE_SIZE) {
                    isLoadMore = size != 0 && size % Page.PAGE_SIZE == 0;
                }
                int headerDataIndex = currentList.indexOf(headerData);
                logger.debug("headerDataIndex::{}", headerDataIndex);
                if (headerDataIndex >= 0) {
                    currentList.remove(headerDataIndex);
                }
                adapter.submitList(currentList, handleUpdateAdapter);
            } else {
                currentList.addAll(0, adapter.getCurrentList());
                isLoadMore = txs.size() != 0 && txs.size() % Page.PAGE_SIZE == 0;

                int headerDataIndex = currentList.indexOf(headerData);
                logger.debug("headerDataIndex::{}", headerDataIndex);
                if (headerDataIndex >= 0) {
                    currentList.remove(headerDataIndex);
                }
                adapter.submitList(currentList, handlePullAdapter);
            }

            binding.refreshLayout.setLoadComplete(!isLoadMore);
            binding.refreshLayout.stopLoadMore();
            if (isVisibleToUser) {
                communityViewModel.clearNewsUnread();
            }
//            logger.debug("txs.size::{}", txs.size());
//            closeProgressDialog();\
//            TauNotifier.getInstance().cancelNotify(chainID);
        });
    }

    private void initRefreshLayout() {
        binding.refreshLayout.setXRefreshViewListener(this);
        binding.refreshLayout.setPullRefreshEnable(false);
        binding.refreshLayout.setPullLoadEnable(true);
        binding.refreshLayout.setAutoLoadMore(true);
        binding.refreshLayout.setMoveForHorizontal(true);
        binding.refreshLayout.enableRecyclerViewPullUp(true);

        CustomXRefreshViewFooter footer = new CustomXRefreshViewFooter(getContext());
        binding.refreshLayout.setCustomFooterView(footer);
    }

    private final Runnable handleUpdateAdapter = () -> {
        LinearLayoutManager layoutManager = (LinearLayoutManager) binding.txList.getLayoutManager();
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
        int dx = binding.txList.getScrollX();
        int dy = binding.txList.getScrollY();
        int offset = getResources().getDimensionPixelSize(R.dimen.widget_size_100);
        binding.txList.smoothScrollBy(dx, dy + offset);
    };

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
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        this.isVisibleToUser = isVisibleToUser;
        logger.debug("setUserVisibleHint1::{}", isVisibleToUser);
        if (communityViewModel != null && isVisibleToUser) {
            communityViewModel.clearNewsUnread();
        }
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
        communityViewModel.getSetBlacklistState().observe(this, isSuccess -> {
            if (isSuccess) {
                loadData(0);
            }
        });
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
                .subscribe(result -> {
                    if (result != null) {
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
                        binding.tvPinnedContent.setText(list.get(0).memo);
                    }
                }));
        disposables.add(txViewModel.observeMaxChatNumNews()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::loadHomeHeaderView));
    }

    private void loadHomeHeaderView(UserAndTxReply tx) {
        if (null == headerBinding) {
            headerBinding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                    R.layout.item_home_news, null, false);
            binding.txList.addHeaderView(headerBinding.getRoot());
        }
        HomeListAdapter.ViewHolder viewHolder = new HomeListAdapter.ViewHolder(headerBinding, this);
        viewHolder.bind(tx);
        this.headerData = tx;
        int headerDataIndex = adapter.getCurrentList().indexOf(tx);
        logger.debug("headerDataIndex::{}", headerDataIndex);
        if (headerDataIndex >= 0) {
            List<UserAndTxReply> list = new ArrayList<>(adapter.getCurrentList());
            list.remove(headerDataIndex);
            adapter.submitList(list);
        }
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
        if (StringUtil.isNotEmpty(tx.link)) {
            menuList.add(new OperationMenuItem(R.string.tx_operation_copy_link));
        }
        // 用户不能拉黑自己
        if(StringUtil.isNotEquals(tx.senderPk,
                MainApplication.getInstance().getPublicKey())){
            menuList.add(new OperationMenuItem(R.string.tx_operation_blacklist));
        }
        menuList.add(new OperationMenuItem(R.string.tx_operation_ban_community));
        menuList.add(new OperationMenuItem(R.string.tx_operation_enter_community));
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
                    KeyboardUtils.hideSoftInput(activity);
                    String showName = UsersUtil.getShowName(tx.sender, tx.senderPk);
                    userViewModel.showBanDialog(activity, tx.senderPk, showName);
                    break;
                case R.string.tx_operation_ban_community:
                    banCommunityDialog = communityViewModel.showBanCommunityTipsDialog(activity, tx.chainID);
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
                case R.string.tx_operation_enter_community:
                    Bundle bundle = new Bundle();
                    bundle.putInt(IntentExtra.TYPE, 0);
                    bundle.putString(IntentExtra.ID, tx.chainID);
                    activity.updateMainRightFragment(bundle);
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
                        intent.putExtra(IntentExtra.PICTURE_PATH, tx.picturePath);
                        ActivityUtil.startActivityForResult(intent, activity, NewsCreateActivity.class,
                                CHOOSE_REQUEST_CODE);
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
    public void onDeleteClicked(UserAndTx tx) {
        txViewModel.deleteThisNews(activity, tx.txID);
    }

    @Override
    public void onBanClicked(UserAndTx tx) {
        KeyboardUtils.hideSoftInput(activity);
        String showName = UsersUtil.getShowName(tx.sender, tx.senderPk);
        userViewModel.showBanDialog(activity, tx.senderPk, showName);
    }

    @Override
    public void onItemClicked(String txID) {
        KeyboardUtils.hideSoftInput(activity);
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.HASH, txID);
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
        if (banCommunityDialog != null && banCommunityDialog.isShowing()) {
            banCommunityDialog.closeDialog();
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

    public void scrollToTop() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) binding.txList.getLayoutManager();
        if (layoutManager != null) {
            logger.debug("handleUpdateAdapter scrollToPosition::{}", 0);
            layoutManager.scrollToPositionWithOffset(0, Integer.MIN_VALUE);
            dataChanged = true;
        }
    }
}
