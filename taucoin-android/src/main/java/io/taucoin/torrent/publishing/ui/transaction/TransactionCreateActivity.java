package io.taucoin.torrent.publishing.ui.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.InputFilter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.model.data.message.TxContent;
import io.taucoin.torrent.publishing.core.model.data.message.TxType;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.TxQueue;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.MoneyValueFilter;
import io.taucoin.torrent.publishing.ui.friends.FriendsActivity;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivityTransactionCreateBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;

/**
 * 交易创建页面页面
 */
public class TransactionCreateActivity extends BaseActivity implements View.OnClickListener {

    private static final int REQUEST_CODE = 0x01;
    private ActivityTransactionCreateBinding binding;

    private TxViewModel txViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private String chainID;
    private TxQueue txQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        txViewModel = provider.get(TxViewModel.class);
        txViewModel.observeNeedStartDaemon();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_transaction_create);
        binding.setListener(this);
        initParameter();
        initLayout();
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

        binding.etAmount.setFilters(new InputFilter[]{new MoneyValueFilter()});

        if (StringUtil.isNotEmpty(chainID)) {
            long txFee = 0;
            if (txQueue != null) {
                txFee = txQueue.fee;
                if (txQueue.content != null) {
                    TxContent txContent = new TxContent(txQueue.content);
                    binding.etMemo.setText(txContent.getMemo());
                }
                binding.etPublicKey.setText(txQueue.receiverPk);
                binding.etPublicKey.setEnabled(false);
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams)
                        binding.etPublicKey.getLayoutParams();
                layoutParams.rightMargin = getResources().getDimensionPixelSize(R.dimen.widget_size_20);
                binding.etPublicKey.setLayoutParams(layoutParams);
                binding.ivSelectPk.setVisibility(View.GONE);
                binding.etAmount.setText(FmtMicrometer.fmtBalance(txQueue.amount));
            }
            long mediaTxFee = txViewModel.getTxFee(chainID, TxType.WIRING_TX);
            String txFeeStr = FmtMicrometer.fmtFeeValue(txFee > 0 ? txFee : mediaTxFee);
            binding.tvFee.setTag(R.id.median_fee, mediaTxFee);

            String txFreeHtml = getString(R.string.tx_median_fee, txFeeStr,
                    ChainIDUtil.getCoinName(chainID));
            binding.tvFee.setText(Html.fromHtml(txFreeHtml));
            binding.tvFee.setTag(txFeeStr);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        txViewModel.getAddState().observe(this, result -> {
            if (StringUtil.isNotEmpty(result)){
                ToastUtils.showShortToast(result);
            } else {
                setResult(RESULT_OK);
                onBackPressed();
            }
        });
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
            if (txViewModel.validateTx(tx)) {
                txViewModel.addTransaction(tx, null == txQueue);
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
        }
        return tx;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_fee:
                txViewModel.showEditFeeDialog(this, binding.tvFee, chainID);
                break;
            case R.id.iv_select_pk:
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.TYPE, FriendsActivity.PAGE_SELECT_CONTACT);
                ActivityUtil.startActivityForResult(intent, this, FriendsActivity.class, REQUEST_CODE);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE && resultCode == RESULT_OK){
            if(data != null){
                String publicKey = data.getStringExtra(IntentExtra.PUBLIC_KEY);
                if(StringUtil.isNotEmpty(publicKey)){
                    binding.etPublicKey.setText(publicKey);
                }
            }
        }
    }
}
