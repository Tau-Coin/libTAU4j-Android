package io.taucoin.torrent.publishing.ui.transaction;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.InputFilter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.data.message.TxType;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivityLeaderInvitationBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;

/**
 * 发布Sell页面
 */
public class LeaderInvitationCreateActivity extends BaseActivity implements View.OnClickListener {

    private static final int CHOOSE_REQUEST_CODE = 0x01;
    private ActivityLeaderInvitationBinding binding;
    private TxViewModel txViewModel;
    private String chainID;
    private boolean noBalance;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        txViewModel = provider.get(TxViewModel.class);
        txViewModel.observeNeedStartDaemon();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_leader_invitation);
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
            noBalance = getIntent().getBooleanExtra(IntentExtra.NO_BALANCE, true);
        }
    }

    /**
     * 初始化布局
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.community_leader_invitation);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        if (StringUtil.isNotEmpty(chainID)) {

            long txFee = txViewModel.getTxFee(chainID);
            String txFeeStr = FmtMicrometer.fmtFeeValue(Constants.COIN.longValue());
            binding.tvFee.setTag(R.id.median_fee, txFee);

            if (noBalance) {
                txFeeStr = "0";
            }
            String medianFree = getString(R.string.tx_median_fee, txFeeStr,
                    ChainIDUtil.getCoinName(chainID));
            binding.tvFee.setText(Html.fromHtml(medianFree));
            binding.tvFee.setTag(txFeeStr);
        }
        String coinName = ChainIDUtil.getCoinName(chainID);
        String[] items = getResources().getStringArray(R.array.coin_name);
        items[1] = coinName;
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
            Tx tx = buildTx();
            if (txViewModel.validateTx(tx)) {
                txViewModel.addTransaction(tx);
            }
        }
        return true;
    }

    /**
     * 构建交易数据
     * @return Tx
     */
    private Tx buildTx() {
        int txType = TxType.LEADER_INVITATION.getType();
        String fee = ViewUtils.getStringTag(binding.tvFee);
        String coinName = ViewUtils.getText(binding.etCoinName);
        String description = ViewUtils.getText(binding.etDescription);
        return new Tx(chainID, FmtMicrometer.fmtTxLongValue(fee), txType, coinName, description);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.iv_coins:
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.COIN_NAME, ViewUtils.getText(binding.etCoinName));
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                ActivityUtil.startActivityForResult(intent, this, CoinsChooseActivity.class,
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
                String coinName = data.getStringExtra(IntentExtra.COIN_NAME);
                if (StringUtil.isNotEmpty(coinName)) {
                    binding.etCoinName.setText(coinName);
                    binding.etCoinName.setSelection(coinName.length());
                } else {
                    binding.etCoinName.getText().clear();
                }
            }
        }
    }
}
