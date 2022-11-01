package io.taucoin.tauapp.publishing.ui.transaction;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
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
import io.taucoin.tauapp.publishing.core.model.data.message.AirdropTxContent;
import io.taucoin.tauapp.publishing.core.model.data.message.TxType;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.TxQueue;
import io.taucoin.tauapp.publishing.core.utils.ActivityUtil;
import io.taucoin.tauapp.publishing.core.utils.ChainIDUtil;
import io.taucoin.tauapp.publishing.core.utils.FmtMicrometer;
import io.taucoin.tauapp.publishing.core.utils.LinkUtil;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.core.utils.ToastUtils;
import io.taucoin.tauapp.publishing.core.utils.ViewUtils;
import io.taucoin.tauapp.publishing.databinding.ActivityAirdropCoinsBinding;
import io.taucoin.tauapp.publishing.ui.BaseActivity;
import io.taucoin.tauapp.publishing.ui.constant.IntentExtra;
import io.taucoin.tauapp.publishing.ui.customviews.FilterEditText;
import io.taucoin.tauapp.publishing.ui.friends.AirdropCommunityActivity;

/**
 * 发布Airdrop页面
 */
public class AirdropCreateActivity extends BaseActivity implements View.OnClickListener {

    private static final int CHOOSE_REQUEST_CODE = 0x01;
    private ActivityAirdropCoinsBinding binding;
    private TxViewModel txViewModel;
    private String chainID;
    private TxQueue txQueue;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private boolean isClickPaste = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        txViewModel = provider.get(TxViewModel.class);
        txViewModel.observeNeedStartDaemon();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_airdrop_coins);
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
        binding.toolbarInclude.toolbar.setTitle(R.string.community_airdrop);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        if (StringUtil.isNotEmpty(chainID)) {
            if (txQueue != null) {
                AirdropTxContent txContent = new AirdropTxContent(txQueue.content);
                binding.etLink.setText(txContent.getLink());
                binding.etDescription.setText(txContent.getMemo());
            }
        }

        binding.etLink.setOnPasteCallback(new FilterEditText.OnPasteCallback() {
            @Override
            public void onPaste() {
                isClickPaste = true;
            }
        });

        binding.etLink.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isClickPaste) {
                    isClickPaste = false;
                    String content = binding.etLink.getEditableText().toString();
                    if (StringUtil.isNotEmpty(content)) {
                        LinkUtil.Link link = LinkUtil.decode(content);
                        if (link.isAirdropLink()) {
                            binding.etLink.setText(content);
                        }
                    }
                }
            }
        });
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

        disposables.add(txViewModel.observeAverageTxFee(chainID, TxType.AIRDROP_TX)
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
        String link = ViewUtils.getText(binding.etLink);
        String description = ViewUtils.getText(binding.etDescription);
        AirdropTxContent content = new AirdropTxContent(link, description);
        TxQueue tx = new TxQueue(chainID, senderPk, senderPk, 0L,
                FmtMicrometer.fmtTxLongValue(fee), TxType.AIRDROP_TX, content.getEncoded());
        if (txQueue != null) {
            tx.queueID = txQueue.queueID;
            tx.queueTime = txQueue.queueTime;
        }
        return tx;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.iv_links:
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.LINKS_SELECTOR, true);
                ActivityUtil.startActivityForResult(intent, this, AirdropCommunityActivity.class,
                        CHOOSE_REQUEST_CODE);
                break;
            case R.id.tv_fee:
                txViewModel.showEditFeeDialog(this, binding.tvFee, chainID);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == CHOOSE_REQUEST_CODE) {
            if (data != null) {
                String airdropLink = data.getStringExtra(IntentExtra.AIRDROP_LINK);
                if (StringUtil.isNotEmpty(airdropLink)) {
                    binding.etLink.setText(airdropLink);
                    binding.etLink.setSelection(airdropLink.length());
                }
            }
        }
    }
}
