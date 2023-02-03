package io.taucbd.news.publishing.ui.transaction;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.disposables.CompositeDisposable;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.model.data.message.TxType;
import io.taucbd.news.publishing.core.storage.sqlite.entity.Tx;
import io.taucbd.news.publishing.core.utils.ActivityUtil;
import io.taucbd.news.publishing.core.utils.ChainIDUtil;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.core.utils.ToastUtils;
import io.taucbd.news.publishing.core.utils.ViewUtils;
import io.taucbd.news.publishing.databinding.ActivityNoteBinding;
import io.taucbd.news.publishing.ui.BaseActivity;
import io.taucbd.news.publishing.ui.constant.IntentExtra;
import io.taucbd.news.publishing.ui.main.MainActivity;

/**
 * 发布Note页面
 */
public class NoteCreateActivity extends BaseActivity implements View.OnClickListener {

    private static final int CHOOSE_REQUEST_CODE = 0x01;
    private ActivityNoteBinding binding;
    private TxViewModel txViewModel;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        txViewModel = provider.get(TxViewModel.class);
        txViewModel.observeNeedStartDaemon();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_note);
        binding.setListener(this);
        initLayout();
    }

    /**
     * 初始化布局
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.common_retweet);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        if (getIntent() != null) {
            CharSequence msg = getIntent().getCharSequenceExtra(IntentExtra.DATA);
            binding.etMessage.setText(msg.toString());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        txViewModel.getAddState().observe(this, result -> {
            if (StringUtil.isNotEmpty(result)) {
                ToastUtils.showShortToast(result);
            } else {
                String chainID = ViewUtils.getStringTag(binding.etCommunity);
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(IntentExtra.TYPE, 0);
                ActivityUtil.startActivity(intent, this, MainActivity.class);
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
            if (txViewModel.validateNoteTx(tx)) {
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
        int txType = TxType.NOTE_TX.getType();
        String memo = ViewUtils.getText(binding.etMessage);
        String chainID = ViewUtils.getStringTag(binding.etCommunity);
		//TODO: link, repliedHash
		String link = "https://taucoin.io/test";
		String repliedHash = "b0cff312c8fc5e74f31940ba3b83646437461a889bece82265039c9080ac6161";
        return new Tx(chainID, 0L, txType, memo, link, repliedHash);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_more:

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
