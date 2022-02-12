package io.taucoin.torrent.publishing.ui.transaction;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.libTAU4j.Account;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.TxQueueAndStatus;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.rlp.ByteUtil;
import io.taucoin.torrent.publishing.databinding.FragmentTxsTabBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.CommunityTabFragment;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.community.QueueListAdapter;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 交易Tab页
 */
public class QueueTabFragment extends CommunityTabFragment implements QueueListAdapter.ClickListener {

    private BaseActivity activity;
    private FragmentTxsTabBinding binding;
    private TxViewModel txViewModel;
    private CommunityViewModel communityViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private QueueListAdapter adapter;

    private String chainID;
    private boolean isReadOnly;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_txs_tab, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activity = (BaseActivity) getActivity();
        assert activity != null;
        ViewModelProvider provider = new ViewModelProvider(this);
        txViewModel = provider.get(TxViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        initParameter();
        initView();
        handleReadOnly(isReadOnly);

    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if(getArguments() != null){
            chainID = getArguments().getString(IntentExtra.CHAIN_ID);
            isReadOnly = getArguments().getBoolean(IntentExtra.READ_ONLY, false);
        }
    }

    /**
     * 初始化视图
     */
    private void initView() {
        binding.fabButton.setVisibility(View.GONE);
        binding.refreshLayout.setRefreshing(false);
        binding.refreshLayout.setEnabled(false);

        adapter = new QueueListAdapter(this, isReadOnly);
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        binding.txList.setLayoutManager(layoutManager);
        binding.txList.setAdapter(adapter);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof BaseActivity)
            activity = (BaseActivity)context;
    }

    @Override
    public void onStart() {
        super.onStart();
        observeTxQueue();
    }

    @Override
    public void onStop() {
        super.onStop();
        disposables.clear();
    }

    /**
     * 观察加入的社区列表
     */
    private void observeTxQueue() {
        disposables.add(communityViewModel.observerCurrentMember(chainID)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(member -> {
                    loadAccountInfo();
                }));

        disposables.add(txViewModel.observeCommunityTxQueue(chainID)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(list -> {
                    if (adapter != null) {
                        adapter.submitList(list);
                    }
                }));

    }

    private void loadAccountInfo() {
        TauDaemon daemon = TauDaemon.getInstance(activity.getApplicationContext());
        String userPk = MainApplication.getInstance().getPublicKey();
        Account account = daemon.getAccountInfo(ChainIDUtil.encode(chainID), userPk);
        if (account != null) {
            adapter.setAccount(account);
        }
    }

    @Override
    public void onDeleteClicked(TxQueueAndStatus tx) {
        txViewModel.deleteTxQueue(tx);
    }

    @Override
    public void onEditClicked(TxQueueAndStatus tx) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.BEAN, tx);
        ActivityUtil.startActivity(intent, this, TransactionCreateActivity.class);
    }

    @Override
    public void handleReadOnly(boolean isReadOnly) {
        if (null == binding) {
            return;
        }
        adapter.setReadOnly(isReadOnly);
    }
}
