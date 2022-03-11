package io.taucoin.torrent.publishing.ui.transaction;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.OperationMenuItem;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.model.data.message.TxType;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.DialogTrustBinding;
import io.taucoin.torrent.publishing.databinding.FragmentTxsTabBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.BaseFragment;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.setting.FavoriteViewModel;
import io.taucoin.torrent.publishing.ui.user.UserDetailActivity;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

public abstract class CommunityTabFragment extends BaseFragment implements View.OnClickListener,
        CommunityListener {

    protected static final Logger logger = LoggerFactory.getLogger("CommunityTabFragment");
    public static final int TX_REQUEST_CODE = 0x1002;
    public static final int TAB_NOTES = 0;
    public static final int TAB_MARKET = 1;
    public static final int TAB_CHAIN = 2;
    protected BaseActivity activity;
    protected FragmentTxsTabBinding binding;
    protected TxViewModel txViewModel;
    protected UserViewModel userViewModel;
    private FavoriteViewModel favoriteViewModel;
    protected CommunityViewModel communityViewModel;
    protected CompositeDisposable disposables = new CompositeDisposable();
    private FloatMenu operationsMenu;
    private CommonDialog trustDialog;

    protected boolean isReadOnly;
    protected String chainID;
    int currentTab;
    int currentPos = 0;

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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_txs_tab, container, false);
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
        favoriteViewModel = provider.get(FavoriteViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        initParameter();
        initView();
        handleReadOnly(isReadOnly);
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if(getArguments() != null){
            chainID = getArguments().getString(IntentExtra.CHAIN_ID);
            isReadOnly = getArguments().getBoolean(IntentExtra.READ_ONLY, false);
        }
    }

    /**
     * 初始化视图
     */
    public void initView() {
        binding.refreshLayout.setOnRefreshListener(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
//        layoutManager.setStackFromEnd(true);
        binding.txList.setLayoutManager(layoutManager);
        binding.txList.setItemAnimator(null);
    }

    final Runnable handleUpdateAdapter = () -> {
        LinearLayoutManager layoutManager = (LinearLayoutManager) binding.txList.getLayoutManager();
        if (layoutManager != null) {
            int bottomPosition = getItemCount() - 1;
            // 滚动到底部
            logger.debug("handleUpdateAdapter scrollToPosition::{}", bottomPosition);
            layoutManager.scrollToPositionWithOffset(bottomPosition, Integer.MIN_VALUE);
        }
    };

    final Runnable handlePullAdapter = () -> {
        LinearLayoutManager layoutManager = (LinearLayoutManager) binding.txList.getLayoutManager();
        if (layoutManager != null) {
            int bottomPosition = getItemCount() - 1;
            int position = bottomPosition - currentPos;
            layoutManager.scrollToPositionWithOffset(position, 0);
        }
    };



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
        // 用户不能拉黑自己
        if(StringUtil.isNotEquals(tx.senderPk,
                MainApplication.getInstance().getPublicKey())){
            menuList.add(new OperationMenuItem(R.string.tx_operation_blacklist));
        }
        menuList.add(new OperationMenuItem(tx.pinned == 0 ? R.string.tx_operation_pin : R.string.tx_operation_unpin));
        menuList.add(new OperationMenuItem(R.string.tx_operation_msg_hash));

        operationsMenu = new FloatMenu(activity);
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
                    txViewModel.setMessagePinned(tx, false);
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
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.PUBLIC_KEY, senderPk);
        ActivityUtil.startActivity(intent, this, UserDetailActivity.class);
    }

    @Override
    public void onEditNameClicked(String senderPk){
        String userPk = MainApplication.getInstance().getPublicKey();
        if (StringUtil.isEquals(userPk, senderPk)) {
            userViewModel.showEditNameDialog(activity, senderPk);
        } else {
            userViewModel.showRemarkDialog(activity, senderPk);
        }
    }

    @Override
    public void onBanClicked(UserAndTx tx){
        String showName = UsersUtil.getShowName(tx.sender, tx.senderPk);
        userViewModel.showBanDialog(activity, tx.senderPk, showName);
    }

    @Override
    public void onTrustClicked(User user) {
        if (!isReadOnly) {
            showTrustDialog(user);
        }
    }

    @Override
    public void onItemClicked(UserAndTx tx) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.ID, tx.txID);
        intent.putExtra(IntentExtra.CHAIN_ID, tx.chainID);
        intent.putExtra(IntentExtra.PUBLIC_KEY, tx.senderPk);
        ActivityUtil.startActivity(intent, activity, SellDetailActivity.class);
    }

    @Override
    public void onLinkClick(String link) {
        ActivityUtil.openUri(activity, link);
    }

    /**
     * 显示信任的对话框
     */
    private void showTrustDialog(User user) {
        DialogTrustBinding binding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.dialog_trust, null, false);
        String showName = UsersUtil.getShowName(user);

        Spanned trustTip = Html.fromHtml(getString(R.string.tx_give_trust_tip, showName));
        binding.tvTrustTip.setText(trustTip);
        long txFee = txViewModel.getTxFee(chainID);
        String txFeeStr = FmtMicrometer.fmtFeeValue(txFee);

        String medianFree = getString(R.string.tx_median_fee, txFeeStr,
                ChainIDUtil.getCoinName(chainID));
        binding.tvTrustFee.setText(Html.fromHtml(medianFree));
        binding.tvTrustFee.setTag(txFeeStr);

        binding.ivClose.setOnClickListener(v -> trustDialog.closeDialog());
        binding.tvSubmit.setOnClickListener(v -> {
            int txType = TxType.TRUST_TX.getType();
            String fee = ViewUtils.getStringTag(binding.tvTrustFee);
            Tx tx = new Tx(chainID, user.publicKey, FmtMicrometer.fmtTxLongValue(fee), txType);
            txViewModel.addTransaction(tx);
            trustDialog.closeDialog();
        });
        trustDialog = new CommonDialog.Builder(activity)
                .setContentView(binding.getRoot())
                .setCanceledOnTouchOutside(false)
                .enableWarpWidth(true)
                .create();
        trustDialog.show();
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
            case R.id.favourite:
                String txID = ViewUtils.getStringTag(v);
                favoriteViewModel.addTxFavorite(txID);
                ToastUtils.showShortToast(R.string.favourite_successfully);
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
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
        if (trustDialog != null) {
            trustDialog.closeDialog();
        }
    }

    public void handleReadOnly(boolean isReadOnly) {
        if (null == binding) {
            return;
        }
        this.isReadOnly = isReadOnly;
    }

    @Override
    public void onRefresh() {
        loadData(getItemCount());
    }

    int getItemCount() {
        return 0;
    }

    protected void loadData(int pos) {
        currentPos = pos;
    }

    @Override
    public void onFragmentResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onFragmentResult(requestCode, resultCode, data);
        if (requestCode == TX_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            loadData(0);
        }
    }

    public void switchView(int spinnerItem) {
    }
}