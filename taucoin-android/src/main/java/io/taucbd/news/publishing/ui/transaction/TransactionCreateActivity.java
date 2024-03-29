package io.taucbd.news.publishing.ui.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import cn.bingoogolapple.refreshlayout.BGARefreshLayout;
import cn.bingoogolapple.refreshlayout.BGAStickinessRefreshViewHolder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucbd.news.publishing.MainApplication;
import io.taucbd.news.publishing.core.model.data.UserAndFriend;
import io.taucbd.news.publishing.core.model.data.message.TxContent;
import io.taucbd.news.publishing.core.model.data.message.TxType;
import io.taucbd.news.publishing.core.storage.sqlite.entity.TxQueue;
import io.taucbd.news.publishing.core.utils.ActivityUtil;
import io.taucbd.news.publishing.core.utils.ChainIDUtil;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.utils.FmtMicrometer;
import io.taucbd.news.publishing.core.utils.LinkUtil;
import io.taucbd.news.publishing.core.utils.ObservableUtil;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.core.utils.ToastUtils;
import io.taucbd.news.publishing.core.utils.ViewUtils;
import io.taucbd.news.publishing.databinding.ActivityTransactionCreateBinding;
import io.taucbd.news.publishing.ui.ScanTriggerActivity;
import io.taucbd.news.publishing.ui.community.ChainExplorerActivity;
import io.taucbd.news.publishing.ui.community.CommunityViewModel;
import io.taucbd.news.publishing.ui.constant.IntentExtra;
import io.taucbd.news.publishing.ui.constant.Page;
import io.taucbd.news.publishing.ui.user.UserViewModel;

/**
 * 交易创建页面页面
 */
public class TransactionCreateActivity extends ScanTriggerActivity implements View.OnClickListener,
        MembersAdapter.ClickListener, BGARefreshLayout.BGARefreshLayoutDelegate{

    private ActivityTransactionCreateBinding binding;

    private UserViewModel userViewModel;
    private TxViewModel txViewModel;
    private CommunityViewModel communityViewModel;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private String chainID;
    private TxQueue txQueue;
    private MembersAdapter adapter;
    private boolean dataChanged = false;
    private int currentPos = 0;
    private boolean isLoadMore = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        txViewModel = provider.get(TxViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        txViewModel.observeNeedStartDaemon();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_transaction_create);
        binding.setListener(this);
        initParameter();
        initLayout();
        initRefreshLayout();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if (getIntent() != null) {
            chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
            txQueue = getIntent().getParcelableExtra(IntentExtra.BEAN);
            if (txQueue != null) {
                chainID = txQueue.chainID;
            }
        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.community_transaction);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

//        binding.etAmount.setFilters(new InputFilter[]{new MoneyValueFilter()});
        binding.etAmount.setInputType(InputType.TYPE_CLASS_NUMBER);

        if (StringUtil.isNotEmpty(chainID)) {
            if (txQueue != null) {
                if (txQueue.content != null) {
                    TxContent txContent = new TxContent(txQueue.content);
                    binding.etMemo.setText(txContent.getMemo());
                } else {
                    binding.etMemo.setText(txQueue.memo);
                }
                binding.etPublicKey.setText(txQueue.receiverPk);
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams)
                        binding.etPublicKey.getLayoutParams();
                layoutParams.rightMargin = getResources().getDimensionPixelSize(R.dimen.widget_size_20);
                binding.etPublicKey.setLayoutParams(layoutParams);
                binding.ivScan.setVisibility(View.GONE);
                binding.etAmount.setText(FmtMicrometer.fmtBalance(txQueue.amount));
				//不可修改有3项
                binding.etPublicKey.setEnabled(false);
                binding.etAmount.setEnabled(false);
                binding.etMemo.setEnabled(false);
            }
        }

        if (null == txQueue) {
            adapter = new MembersAdapter(this);
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            binding.recyclerList.setLayoutManager(layoutManager);
            binding.recyclerList.setItemAnimator(null);
            binding.recyclerList.setAdapter(adapter);

            userViewModel.getUserList().observe(this, friends -> {
                if (friends != null) {
                    if (currentPos == 0) {
                        adapter.submitList(friends);
                    } else {
                        List<UserAndFriend> currentList = new ArrayList<>();
                        currentList.addAll(adapter.getCurrentList());
                        currentList.addAll(friends);
                        adapter.submitList(currentList);
                    }
                    int size = friends.size();
                    isLoadMore = size != 0 && size % Page.PAGE_SIZE == 0;
                    binding.refreshLayout.endLoadingMore();
                }
            });
        } else {
            binding.llMembersSelect.setVisibility(View.GONE);
        }

        txViewModel.getAddState().observe(this, result -> {
            if (StringUtil.isNotEmpty(result)){
                ToastUtils.showShortToast(result);
            } else {
                setResult(RESULT_OK);
                if (txQueue != null) {
                    onBackPressed();
                } else {
                    onBackPressed();
                    Intent intent = new Intent();
                    intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                    intent.putExtra(IntentExtra.IS_JOINED, true);
                    ActivityUtil.startActivity(intent, this, ChainExplorerActivity.class);
                }
            }
        });
        loadPaymentBalanceView(0);
    }

    private void loadFeeView(long averageTxFee) {
        long txFee = 0L;
        if (txQueue != null) {
            txFee = txQueue.fee;
        }
        String txFeeStr = FmtMicrometer.fmtFeeValue(txFee > 0 ? txFee : averageTxFee);
        binding.tvFee.setTag(R.id.median_fee, averageTxFee);

        String txFreeHtml = getString(R.string.tx_median_fee, txFeeStr,
                ChainIDUtil.getCoinName(chainID));
        binding.tvFee.setText(Html.fromHtml(txFreeHtml));
        binding.tvFee.setTag(txFeeStr);
    }

    private void initRefreshLayout() {
        binding.refreshLayout.setDelegate(this);
        BGAStickinessRefreshViewHolder refreshViewHolder = new BGAStickinessRefreshViewHolder(this, true);
        refreshViewHolder.setRotateImage(R.mipmap.ic_launcher_foreground);
        refreshViewHolder.setStickinessColor(R.color.color_yellow);

        refreshViewHolder.setLoadingMoreText(getString(R.string.common_loading));
        binding.refreshLayout.setPullDownRefreshEnable(false);

        binding.refreshLayout.setRefreshViewHolder(refreshViewHolder);
    }

    @Override
    public void onSelectClicked(String publicKey) {
        binding.etPublicKey.setText(publicKey);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (null == txQueue) {
            loadData(0);
            disposables.add(userViewModel.observeUsersChanged()
                    .subscribeOn(Schedulers.io())
                    .subscribe(o -> dataChanged = true));

            disposables.add(ObservableUtil.interval(500)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(o -> {
                        if (dataChanged) {
                            loadData(0);
                            dataChanged = false;
                        }
                    }));
        }
        disposables.add(txViewModel.observeAverageTxFee(chainID, TxType.WIRING_TX)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::loadFeeView));

        disposables.add(communityViewModel.observerCurrentMember(chainID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(member -> {
                    long balance = ViewUtils.getLongTag(binding.tvPaymentBalance);
                    if (member != null && member.getPaymentBalance() != balance) {
                        loadPaymentBalanceView(member.getPaymentBalance());
                    }
                }, it -> {
                    loadPaymentBalanceView(0);
                }));
    }

    private void loadPaymentBalanceView(long balance) {
        long showBalance = balance >= 0 ? balance : 0;
        binding.tvPaymentBalance.setText(getString(R.string.tx_payment_balance,
                FmtMicrometer.fmtLong(showBalance),
                ChainIDUtil.getCoinName(chainID)));
        binding.tvPaymentBalance.setTag(balance);
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    /**
     *  创建右上角Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_done, menu);
        return true;
    }

    /**
     * 右上角Menu选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 交易创建事件
        if (item.getItemId() == R.id.menu_done) {
            TxQueue tx = buildTx();
            long paymentBalance = ViewUtils.getLongTag(binding.tvPaymentBalance);
            if (txViewModel.validateTx(tx, paymentBalance)) {
                txViewModel.addTransaction(tx, txQueue);
            }
        }
        return true;
    }

    /**
     * 构建交易数据
     * @return Tx
     */
    private TxQueue buildTx() {
        String senderPk = MainApplication.getInstance().getPublicKey();
        String receiverPk = ViewUtils.getText(binding.etPublicKey);
        String amount = ViewUtils.getText(binding.etAmount);
        String fee = ViewUtils.getStringTag(binding.tvFee);
        String memo = ViewUtils.getText(binding.etMemo);
        TxContent txContent = new TxContent(TxType.WIRING_TX.getType(), memo);
        TxQueue tx = new TxQueue(chainID, senderPk, receiverPk, FmtMicrometer.fmtTxLongValue(amount),
                    FmtMicrometer.fmtTxLongValue(fee), TxType.WIRING_TX, txContent.getEncoded());
        if (txQueue != null) {
            tx.queueID = txQueue.queueID;
            tx.queueTime = txQueue.queueTime;
        }
        return tx;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_fee:
                txViewModel.showEditFeeDialog(this, binding.tvFee, chainID);
                break;
            case R.id.iv_scan:
                openScanQRActivityForResult();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == ScanTriggerActivity.SCAN_CODE && resultCode == RESULT_OK){
            if(data != null){
                String link = data.getStringExtra(IntentExtra.DATA);
                LinkUtil.Link decode = LinkUtil.decode(link);
                if(decode != null && decode.isFriendLink()){
                    binding.etPublicKey.setText(decode.getPeer());
                }
            }
        }
    }

    private int getItemCount() {
        int count = 0;
        if (adapter != null) {
            count = adapter.getItemCount();
        }
        return count;
    }

    protected void loadData(int pos) {
        this.currentPos = pos;
        userViewModel.loadUsersList(pos, getItemCount());
    }

    @Override
    public void onBGARefreshLayoutBeginRefreshing(BGARefreshLayout refreshLayout) {
    }

    @Override
    public boolean onBGARefreshLayoutBeginLoadingMore(BGARefreshLayout refreshLayout) {
        if (isLoadMore) {
            loadData(getItemCount());
            return true;
        } else {
            refreshLayout.endLoadingMore();
            return false;
        }
    }
}
