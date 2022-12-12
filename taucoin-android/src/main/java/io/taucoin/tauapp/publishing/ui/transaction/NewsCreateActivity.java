package io.taucoin.tauapp.publishing.ui.transaction;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.View;

import org.slf4j.LoggerFactory;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.tauapp.publishing.MainApplication;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.Constants;
import io.taucoin.tauapp.publishing.core.model.data.message.NewsContent;
import io.taucoin.tauapp.publishing.core.model.data.message.TxType;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.TxQueue;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.User;
import io.taucoin.tauapp.publishing.core.utils.ChainIDUtil;
import io.taucoin.tauapp.publishing.core.utils.FmtMicrometer;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.core.utils.ToastUtils;
import io.taucoin.tauapp.publishing.core.utils.UsersUtil;
import io.taucoin.tauapp.publishing.core.utils.Utils;
import io.taucoin.tauapp.publishing.core.utils.ViewUtils;
import io.taucoin.tauapp.publishing.databinding.ActivityNewsBinding;
import io.taucoin.tauapp.publishing.ui.BaseActivity;
import io.taucoin.tauapp.publishing.ui.community.CommunityViewModel;
import io.taucoin.tauapp.publishing.ui.constant.IntentExtra;

/**
 * 发布News页面
 */
public class NewsCreateActivity extends BaseActivity implements View.OnClickListener {

    private ActivityNewsBinding binding;
    private TxViewModel txViewModel;
    private CommunityViewModel communityViewModel;
    private String chainID;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private TxQueue txQueue;
    private CharSequence msg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        txViewModel = provider.get(TxViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_news);
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
            msg = getIntent().getCharSequenceExtra(IntentExtra.DATA);
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
        binding.tvBytesCount.setText("0/" + Constants.NEWS_TX_MAX_BYTE_SIZE);
        binding.tvBytesCount.setTextColor(getResources().getColor(R.color.color_gray_dark));
        binding.etNews.addTextChangedListener(textWatcher);
        binding.etNews.setMaxBytesLimit(Constants.NEWS_TX_MAX_BYTE_SIZE);

        binding.ivCancel.setOnClickListener(v -> onBackPressed());
        if (StringUtil.isNotEmpty(chainID)) {
            if (txQueue != null) {
                NewsContent txContent = new NewsContent(txQueue.content);
                binding.etNews.setText(txContent.getMemo());
                binding.etNews.setSelection(binding.etNews.getText().length());
                binding.etNews.setEnabled(false);
            } else if (StringUtil.isNotEmpty(msg)) {
                binding.etNews.setText(msg.toString());
                binding.etNews.setSelection(binding.etNews.getText().length());
//                binding.etNews.setEnabled(false);
                binding.tvPost.setText(R.string.common_retweet);
            }
        }
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            boolean isEmpty = StringUtil.isEmpty(s);
            binding.tvPost.setEnabled(!isEmpty);
            byte[] bytes = Utils.textStringToBytes(s.toString());
            int bytesCount = bytes != null ? bytes.length : 0;
            binding.tvBytesCount.setText(bytesCount + "/" + Constants.NEWS_TX_MAX_BYTE_SIZE);
            binding.tvBytesCount.setTextColor(getResources().getColor(bytesCount > 0 ? R.color.color_black :
                    R.color.color_gray_dark));
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

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

        disposables.add(txViewModel.observeAverageTxFee(chainID, TxType.NEWS_TX)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::loadFeeView));

        disposables.add(communityViewModel.observerCurrentMember(chainID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(member -> {
                    long balance = ViewUtils.getLongTag(binding.tvInterimBalance);
                    if (member != null && member.getDisplayBalance() != balance) {
                        loadInterimBalanceView(member.getDisplayBalance());
                    }
                }, it -> {
                    loadInterimBalanceView(0);
                }));
    }

    private void loadInterimBalanceView(long showBalance) {
        binding.tvInterimBalance.setText(getString(R.string.tx_interim_balance,
                FmtMicrometer.fmtFeeValue(showBalance),
                ChainIDUtil.getCoinName(chainID)));
        binding.tvInterimBalance.setTag(showBalance);
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.etNews.removeTextChangedListener(textWatcher);
    }

    /**
     * 构建交易数据
     * @return Tx
     */
    private TxQueue buildTx() {
        String senderPk = MainApplication.getInstance().getPublicKey();
        String fee = ViewUtils.getStringTag(binding.tvFee);
        String news = ViewUtils.getText(binding.etNews);
		//TODO: link, repliedHash, repliedKey
		String link = null;
		String repliedHash = null;
		String repliedKey = null;
        NewsContent content = new NewsContent(news, link, repliedHash, repliedKey);
        TxQueue tx = new TxQueue(chainID, senderPk, senderPk, 0L,
                FmtMicrometer.fmtTxLongValue(fee), TxType.NEWS_TX, content.getEncoded());
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
            case R.id.tv_post:
                TxQueue tx = buildTx();
                if (txViewModel.validateTx(tx)) {
                    txViewModel.addTransaction(tx, txQueue);
                }
                break;
        }
    }
}
