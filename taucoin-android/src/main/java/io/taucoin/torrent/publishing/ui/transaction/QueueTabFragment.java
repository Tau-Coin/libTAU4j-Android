package io.taucoin.torrent.publishing.ui.transaction;

import android.content.Intent;

import org.libTAU4j.Account;

import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.TxQueueAndStatus;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.ui.community.QueueListAdapter;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;

/**
 * 交易队列Tab页
 */
public class QueueTabFragment extends CommunityTabFragment implements QueueListAdapter.ClickListener {

    private QueueListAdapter adapter;

    /**
     * 初始化视图
     */
    @Override
    public void initView() {
        super.initView();

        binding.refreshLayout.setRefreshing(false);
        binding.refreshLayout.setEnabled(false);

        adapter = new QueueListAdapter(this, isReadOnly);
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        binding.txList.setLayoutManager(layoutManager);
        binding.txList.setAdapter(adapter);
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
        intent.putExtra(IntentExtra.READ_ONLY, false);
        ActivityUtil.startActivity(intent, this, TransactionCreateActivity.class);
    }

    @Override
    public void onStart() {
        super.onStart();
        observeTxQueue();
    }
}
