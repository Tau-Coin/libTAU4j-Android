package io.taucoin.torrent.publishing.ui.transaction;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndMember;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.model.data.message.TxType;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.KeyboardUtils;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.ui.constant.Page;

/**
 * 交易Tab页
 */
public class NotesTabFragment extends CommunityTabFragment implements NotesListAdapter.ClickListener {

    private Handler handler = new Handler();
    private NotesListAdapter adapter;
    private boolean isUpdateFee = true;

    /**
     * 初始化视图
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void initView() {
        super.initView();
        currentTab = TAB_NOTES;
        binding.llOnChainCheckBox.setVisibility(View.VISIBLE);
        adapter = new NotesListAdapter(this, chainID, true);
        binding.txList.setAdapter(adapter);

        binding.etMessage.addTextChangedListener(textWatcher);

        binding.getRoot().setOnTouchListener((v, event) -> {
            KeyboardUtils.hideSoftInput(activity);
            return false;
        });

        binding.etMessage.setOnFocusChangeListener((v, hasFocus) -> {
            isScrollToBottom = true;
            handler.postDelayed(handleUpdateAdapter, 200);
        });

        binding.etMessage.setOnClickListener(v -> {
            boolean isVisible = KeyboardUtils.isSoftInputVisible(activity);
            logger.debug("onSoftInputChanged2::{}", isVisible);
            if (!isVisible) {
                isScrollToBottom = true;
                handler.postDelayed(handleUpdateAdapter, 200);
            }
        });

        binding.cbOnChain.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isScrollToBottom = true;
            loadData(0);
        });

        showBottomView();
    }

    private void showBottomView() {
        binding.llBottomInput.setVisibility(isJoined ? View.VISIBLE : View.GONE);
        binding.tvFeeTips.setVisibility(!onChain ? View.VISIBLE : View.GONE);
        binding.tvFee.setVisibility(onChain ? View.VISIBLE : View.GONE);

        if (onChain && isUpdateFee) {
            long txFee = txViewModel.getTxFee(chainID);
            String txFeeStr = FmtMicrometer.fmtFeeValue(txFee);
            binding.tvFee.setTag(R.id.median_fee, txFee);
            if (noBalance) {
                txFeeStr = "0";
            }
            String medianFree = getString(R.string.tx_median_fee, txFeeStr,
                    ChainIDUtil.getCoinName(chainID));
            binding.tvFee.setText(Html.fromHtml(medianFree));
            binding.tvFee.setTag(txFeeStr);
        }
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            boolean isEmpty = StringUtil.isEmpty(s);
            binding.tvSend.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
//            binding.ivAdd.setVisibility(!isEmpty ? View.GONE : View.VISIBLE);
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    @Override
    public void onClick(View v) {
        super.onClick(v);
        if (v.getId() == R.id.tv_send) {
            Tx tx = buildTx();
            if (txViewModel.validateTx(tx)) {
                isScrollToBottom = true;
                txViewModel.addTransaction(tx);
            }
        } else if (v.getId() == R.id.tv_fee) {
            KeyboardUtils.hideSoftInput(activity);
            txViewModel.showEditFeeDialog(activity, binding.tvFee, chainID);
        }
    }

    /**
     * 构建交易数据
     * @return Tx
     */
    private Tx buildTx() {
        int txType = TxType.NOTE_TX.getType();
        String fee = ViewUtils.getStringTag(binding.tvFee);
        String memo = ViewUtils.getText(binding.etMessage);
        return new Tx(chainID, FmtMicrometer.fmtTxLongValue(fee), txType, memo);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding.etMessage.removeTextChangedListener(textWatcher);
    }

    @Override
    public void onStart() {
        super.onStart();
        txViewModel.getAddState().observe(this, result -> {
            if (StringUtil.isNotEmpty(result)) {
                ToastUtils.showShortToast(result);
            } else {
                binding.cbOnChain.setChecked(false);
                binding.etMessage.getText().clear();
            }
        });

        txViewModel.observerChainTxs().observe(this, txs -> {
            List<UserAndTx> currentList = new ArrayList<>(txs);
            if (currentPos == 0) {
                initScrollToBottom();
                adapter.submitList(currentList, handleUpdateAdapter);
            } else {
                currentList.addAll(adapter.getCurrentList());
                adapter.submitList(currentList, handlePullAdapter);
            }
            binding.refreshLayout.setRefreshing(false);
            binding.refreshLayout.setEnabled(txs.size() != 0 && txs.size() % Page.PAGE_SIZE == 0);

            logger.debug("txs.size::{}", txs.size());
            closeProgressDialog();
        });
        showProgressDialog();

        loadData(0);

        disposables.add(txViewModel.observeLatestPinnedMsg(currentTab, chainID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    boolean isHavePinnedMsg = list != null && list.size() > 0;
                    binding.llPinnedMessage.setVisibility(isHavePinnedMsg ? View.VISIBLE : View.GONE);
                    if (isHavePinnedMsg) {
                        binding.tvPinnedContent.setText(TxUtils.createTxSpan(list.get(0)));
                    }
                }));

        disposables.add(txViewModel.observeDataSetChanged()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    // 跟当前用户有关系的才触发刷新
                    if (result != null && StringUtil.isNotEmpty(result.getMsg())) {
                        binding.refreshLayout.setRefreshing(false);
                        binding.refreshLayout.setEnabled(false);
                        // 立即执行刷新
                        loadData(0);
                    }
                }));

        userViewModel.getEditBlacklistResult().observe(this, result -> {
            if (result.isSuccess()) {
                ToastUtils.showShortToast(R.string.ban_successfully);
            } else {
                ToastUtils.showShortToast(R.string.ban_failed);
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        // 关闭键盘
        binding.etMessage.clearFocus();
    }

    @Override
    int getItemCount() {
        if (adapter != null) {
            return adapter.getItemCount();
        }
        return super.getItemCount();
    }

    @Override
    public void handleMember(CommunityAndMember member) {
        super.handleMember(member);
        showBottomView();
        isUpdateFee = false;
    }

    @Override
    public void loadData(int pos) {
        super.loadData(pos);
        boolean onChain = binding.cbOnChain.isChecked();
        txViewModel.loadNotesData(onChain, chainID, pos, getItemCount());
    }

    @Override
    public void hideView() {
        super.hideView();
        binding.llBottomInput.setVisibility(View.GONE);
    }
}
