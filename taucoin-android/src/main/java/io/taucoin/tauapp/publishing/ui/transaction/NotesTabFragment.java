package io.taucoin.tauapp.publishing.ui.transaction;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.model.data.CommunityAndMember;
import io.taucoin.tauapp.publishing.core.model.data.UserAndTx;
import io.taucoin.tauapp.publishing.core.model.data.message.TxType;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.tauapp.publishing.core.utils.KeyboardUtils;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.core.utils.ToastUtils;
import io.taucoin.tauapp.publishing.core.utils.ViewUtils;
import io.taucoin.tauapp.publishing.databinding.FragmentTxsNotesTabBinding;
import io.taucoin.tauapp.publishing.ui.TauNotifier;
import io.taucoin.tauapp.publishing.ui.constant.Page;

/**
 * 交易Tab页
 */
public class NotesTabFragment extends CommunityTabFragment implements NotesListAdapter.ClickListener {

    private Handler handler = new Handler();
    private NotesListAdapter adapter;
    private FragmentTxsNotesTabBinding binding;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_txs_notes_tab, container, false);
        binding.setListener(this);
        return binding.getRoot();
    }

    @Override
    public RecyclerView getRecyclerView() {
        return binding.txList;
    }

    @Override
    public SwipeRefreshLayout getRefreshLayout() {
        return binding.refreshLayout;
    }

    /**
     * 初始化视图
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void initView() {
        super.initView();
        currentTab = TAB_NOTES;
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

        showBottomView();
    }

    private void showBottomView() {
        if (null == binding) {
            return;
        }
        binding.llBottomInput.setVisibility(isJoined ? View.VISIBLE : View.GONE);
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            boolean isEmpty = StringUtil.isEmpty(s);
            binding.tvSend.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
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
            if (txViewModel.validateNoteTx(tx)) {
                isScrollToBottom = true;
                txViewModel.addTransaction(tx);
            }
        }
    }

    /**
     * 构建交易数据
     * @return Tx
     */
    private Tx buildTx() {
        int txType = TxType.NOTE_TX.getType();
        String memo = ViewUtils.getText(binding.etMessage);
        return new Tx(chainID, 0L, txType, memo);
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
            communityViewModel.clearMsgUnread(chainID);
            closeProgressDialog();
            TauNotifier.getInstance().cancelNotify(chainID);
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
    }

    @Override
    public void loadData(int pos) {
        super.loadData(pos);
        txViewModel.loadNotesData(false, chainID, pos, getItemCount());
    }
}
