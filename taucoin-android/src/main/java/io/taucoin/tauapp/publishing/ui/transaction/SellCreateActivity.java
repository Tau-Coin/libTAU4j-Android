package io.taucoin.tauapp.publishing.ui.transaction;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.tauapp.publishing.MainApplication;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.model.data.message.SellTxContent;
import io.taucoin.tauapp.publishing.core.model.data.message.TxType;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.TxQueue;
import io.taucoin.tauapp.publishing.core.utils.ActivityUtil;
import io.taucoin.tauapp.publishing.core.utils.ChainIDUtil;
import io.taucoin.tauapp.publishing.core.utils.FmtMicrometer;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.core.utils.ToastUtils;
import io.taucoin.tauapp.publishing.core.utils.ViewUtils;
import io.taucoin.tauapp.publishing.databinding.ActivitySellBinding;
import io.taucoin.tauapp.publishing.ui.BaseActivity;
import io.taucoin.tauapp.publishing.ui.constant.IntentExtra;

/**
 * 发布Sell页面
 */
public class SellCreateActivity extends BaseActivity implements View.OnClickListener {

    private static final int CHOOSE_REQUEST_CODE = 0x01;
    private ActivitySellBinding binding;
    private TxViewModel txViewModel;
    private String chainID;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private TxQueue txQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        txViewModel = provider.get(TxViewModel.class);
        txViewModel.observeNeedStartDaemon();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sell);
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
    @SuppressLint("ClickableViewAccessibility")
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.community_sell_coins);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

//        binding.etQuantity.setFilters(new InputFilter[]{new MoneyValueFilter()});
        binding.etQuantity.setInputType(InputType.TYPE_CLASS_NUMBER);

        if (StringUtil.isNotEmpty(chainID)) {
            if (txQueue != null) {
                SellTxContent txContent = new SellTxContent(txQueue.content);
                binding.etItemName.setText(txContent.getCoinName());
                binding.etLink.setText(txContent.getLink());
                binding.etQuantity.setText(String.valueOf(txContent.getQuantity()));
                binding.etLocation.setText(txContent.getLocation());
                binding.etDescription.setText(txContent.getMemo());
            }
        }
        String coinName = ChainIDUtil.getCoinName(chainID);
        String[] items = getResources().getStringArray(R.array.coin_name);
        items[1] = coinName;

        binding.rlCommunity.setVisibility(StringUtil.isEmpty(chainID) ? View.VISIBLE : View.GONE);
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

    @Override
    public void onStart() {
        super.onStart();
        txViewModel.getAddState().observe(this, result -> {
            if (StringUtil.isNotEmpty(result)) {
                ToastUtils.showShortToast(result);
            } else {
                setResult(RESULT_OK);
                onBackPressed();
            }
        });

        disposables.add(txViewModel.observeAverageTxFee(chainID, TxType.SELL_TX)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::loadFeeView));
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
        if (item.getItemId() == R.id.menu_done) {
            TxQueue tx = buildTx();
            if (txViewModel.validateTx(tx)) {
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
        String fee = ViewUtils.getStringTag(binding.tvFee);
        String coinName = ViewUtils.getText(binding.etItemName);
        long quantity = ViewUtils.getIntTag(binding.etQuantity);
        String link = ViewUtils.getText(binding.etLink);
        String location = ViewUtils.getText(binding.etLocation);
        String description = ViewUtils.getText(binding.etDescription);

        SellTxContent content = new SellTxContent(coinName, quantity, link, location, description);
        TxQueue tx = new TxQueue(chainID, senderPk, senderPk, 0L,
                FmtMicrometer.fmtTxLongValue(fee), TxType.SELL_TX, content.getEncoded());
        if (txQueue != null) {
            tx.queueID = txQueue.queueID;
            tx.queueTime = txQueue.queueTime;
        }
        if (StringUtil.isEmpty(chainID)) {
            tx.chainID = ViewUtils.getStringTag(binding.etCommunity);
        }
        return tx;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_fee:
                txViewModel.showEditFeeDialog(this, binding.tvFee, chainID);
                break;
            case R.id.iv_community:
                ActivityUtil.startActivityForResult(this, NewsCreateActivity.class,
                        CHOOSE_REQUEST_CODE);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == CHOOSE_REQUEST_CODE) {
            if (data != null) {
                String chainID = data.getStringExtra(IntentExtra.CHAIN_ID);
                if (StringUtil.isNotEmpty(chainID)) {
                    String communityName = ChainIDUtil.getName(chainID);
                    String communityCode = ChainIDUtil.getCode(chainID);
                    binding.etCommunity.setText(getString(R.string.main_community_name, communityName, communityCode));
                    binding.etCommunity.setTag(chainID);
                } else {
                    binding.etCommunity.getText().clear();
                }
            }
        }
    }
}
