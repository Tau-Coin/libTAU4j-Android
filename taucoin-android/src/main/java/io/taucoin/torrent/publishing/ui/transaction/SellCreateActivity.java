package io.taucoin.torrent.publishing.ui.transaction;

import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.message.TxType;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.KeyboardUtils;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivitySellBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;

/**
 * 发布Sell页面
 */
public class SellCreateActivity extends BaseActivity implements View.OnClickListener {

    private ActivitySellBinding binding;
    private TxViewModel txViewModel;
    private String chainID;
    private CompositeDisposable disposables = new CompositeDisposable();

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
        if(getIntent() != null){
            chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.community_sell_coins);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        if (StringUtil.isNotEmpty(chainID)) {

            long txFee = txViewModel.getTxFee(chainID);
            String txFeeStr = FmtMicrometer.fmtFeeValue(txFee);
            binding.tvFee.setTag(R.id.median_fee, txFee);

            String medianFree = getString(R.string.tx_median_fee, txFeeStr,
                    ChainIDUtil.getCoinName(chainID));
            binding.tvFee.setText(Html.fromHtml(medianFree));
            binding.tvFee.setTag(txFeeStr);
        }
        String coinName = ChainIDUtil.getCoinName(chainID);
        String[] items = getResources().getStringArray(R.array.coin_name);
        items[1] = coinName;

        SpinnerAdapter adapter = new SpinnerAdapter(this, items);
        binding.coinSpinner.setAdapter(adapter);
        binding.coinSpinner.setDropDownVerticalOffset(getResources().getDimensionPixelSize(R.dimen.widget_size_45));
        binding.coinSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                binding.etCoinName.setText(items[position]);
                adapter.setSelectPos(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        binding.coinSpinner.setOnTouchListener((v, event) -> {
            v.performClick();
            KeyboardUtils.hideSoftInput(SellCreateActivity.this);
            return false;
        });
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
        int txType = TxType.SELL_TX.getType();
        String fee = ViewUtils.getStringTag(binding.tvFee);
        String coinName = ViewUtils.getText(binding.etCoinName);
        String link = ViewUtils.getText(binding.etLink);
        String location = ViewUtils.getText(binding.etLocation);
        String description = ViewUtils.getText(binding.etDescription);
        return new Tx(chainID, FmtMicrometer.fmtTxLongValue(fee), txType, coinName, link,
                location, description);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_fee:
                txViewModel.showEditFeeDialog(this, binding.tvFee, chainID);
                break;
        }
    }
}