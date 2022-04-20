package io.taucoin.torrent.publishing.ui.transaction;

import android.content.Intent;

import org.libTAU4j.Account;

import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.TxQueueAndStatus;
import io.taucoin.torrent.publishing.core.model.data.message.TxContent;
import io.taucoin.torrent.publishing.core.model.data.message.TxType;
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

        adapter = new QueueListAdapter(this);
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
                }, it -> {}));

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
        TxContent txContent = new TxContent(tx.content);
        int type = txContent.getType();
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.BEAN, tx);
        if (type == TxType.WIRING_TX.getType()) {
            ActivityUtil.startActivity(intent, this, TransactionCreateActivity.class);
        } else if (type == TxType.SELL_TX.getType()) {
            ActivityUtil.startActivity(intent, this, SellCreateActivity.class);
        } else if (type == TxType.AIRDROP_TX.getType()) {
            ActivityUtil.startActivity(intent, this, AirdropCreateActivity.class);
        } else if (type == TxType.ANNOUNCEMENT.getType()) {
            ActivityUtil.startActivity(intent, this, AnnouncementCreateActivity.class);
        } else if (type == TxType.TRUST_TX.getType()) {
            showTrustDialog(null, tx);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        observeTxQueue();
    }
}
