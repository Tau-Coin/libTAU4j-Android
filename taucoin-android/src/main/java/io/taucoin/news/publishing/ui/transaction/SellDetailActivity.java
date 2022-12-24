package io.taucoin.news.publishing.ui.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.news.publishing.MainApplication;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.model.data.FriendStatus;
import io.taucoin.news.publishing.core.model.data.UserAndFriend;
import io.taucoin.news.publishing.core.model.data.UserAndTx;
import io.taucoin.news.publishing.core.storage.sqlite.entity.User;
import io.taucoin.news.publishing.core.utils.ActivityUtil;
import io.taucoin.news.publishing.core.utils.BitmapUtil;
import io.taucoin.news.publishing.core.utils.GeoUtils;
import io.taucoin.news.publishing.core.utils.StringUtil;
import io.taucoin.news.publishing.core.utils.ToastUtils;
import io.taucoin.news.publishing.core.utils.UsersUtil;
import io.taucoin.news.publishing.databinding.ActivitySellDetailBinding;
import io.taucoin.news.publishing.ui.BaseActivity;
import io.taucoin.news.publishing.ui.constant.IntentExtra;
import io.taucoin.news.publishing.ui.main.MainActivity;
import io.taucoin.news.publishing.ui.user.UserViewModel;

/**
 * Sell detail
 */
@Deprecated
public class SellDetailActivity extends BaseActivity implements View.OnClickListener {
    private ActivitySellDetailBinding binding;
    private TxViewModel txViewModel;
    private UserViewModel userViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private TrustListAdapter adapter;
    private String chainID;
    private String txID;
    private String sellerPk;
    private UserAndFriend user;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        txViewModel = provider.get(TxViewModel.class);
        txViewModel.observeNeedStartDaemon();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sell_detail);
        binding.setListener(this);
        initParam();
        initView();
    }

    /**
     * 初始化参数
     */
    private void initParam() {
        if (getIntent() != null) {
            chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
            txID = getIntent().getStringExtra(IntentExtra.ID);
            sellerPk = getIntent().getStringExtra(IntentExtra.PUBLIC_KEY);
        }
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.community_sell_detail);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new TrustListAdapter();

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerList.setLayoutManager(layoutManager);
        binding.recyclerList.setItemAnimator(null);
        binding.recyclerList.setAdapter(adapter);

        // 获取用户详情数据
        userViewModel.getUserDetail(sellerPk);

//        txViewModel.observerTrustTxs().observe(this, txs -> {
//            adapter.submitList(txs);
//        });

        userViewModel.getUserDetail().observe(this, this::showUserInfo);

        userViewModel.getAddFriendResult().observe(this, result -> {
            if (result.isSuccess()) {
                ToastUtils.showShortToast(result.getMsg());
            } else {
                binding.tvDirectTalk.setText(R.string.tx_sell_direct_talk);
            }
        });

        userViewModel.getEditBlacklistResult().observe(this, result -> {
            if (result.isSuccess()) {
                ToastUtils.showShortToast(R.string.blacklist_successfully);
                onBackPressed();
            } else {
                ToastUtils.showShortToast(R.string.blacklist_failed);
            }
        });
    }

    private void showSellInfo(UserAndTx tx) {
//        String trusts = FmtMicrometer.fmtLong(tx.trusts);
//        trusts = getString(R.string.tx_sell_detail_trust, trusts);
//        binding.tvTrusts.setText(trusts);
//
//        binding.tvSellInfo.setText(TxUtils.createTxSpan(tx));
//
//        binding.llTrustMore.setVisibility(tx.trusts > 3 ? View.VISIBLE : View.GONE);
//        binding.llTrustHash.setVisibility(tx.trusts > 0 ? View.VISIBLE : View.GONE);
//        if (tx.trusts > 0) {
//            txViewModel.loadTrustTxsData(chainID, sellerPk, 0, 3);
//        }
    }

    private void showUserInfo(UserAndFriend user) {
        this.user = user;
        binding.leftView.ivHeadPic.setImageBitmap(UsersUtil.getHeadPic(user));
        boolean isMine = StringUtil.isEquals(sellerPk, MainApplication.getInstance().getPublicKey());
        binding.tvDirectTalk.setVisibility(isMine ? View.GONE : View.VISIBLE);
        binding.leftView.tvBlacklist.setVisibility(isMine ? View.GONE : View.VISIBLE);
        String nickName;
        if (isMine) {
            nickName = UsersUtil.getCurrentUserName(user);
        } else {
            nickName = UsersUtil.getShowName(user);
            binding.leftView.tvBlacklist.setOnClickListener(v -> {
                userViewModel.showBanDialog(this, sellerPk, nickName);
            });
            binding.tvDirectTalk.setText(user.status != FriendStatus.DISCOVERED.getStatus() ?
                    R.string.tx_sell_direct_talk : R.string.tx_sell_add_friend);
        }
        binding.tvName.setText(nickName);

        User currentUser = MainApplication.getInstance().getCurrentUser();
        String distance = null;
        if (currentUser != null) {
            if (user.longitude != 0 && user.latitude != 0 &&
                    currentUser.longitude != 0 && currentUser.latitude != 0) {
                distance = GeoUtils.getDistanceStr(user.longitude, user.latitude,
                        currentUser.longitude, currentUser.latitude);
            }
        }
        if (StringUtil.isNotEmpty(distance)) {
            binding.tvDistance.setText(distance);
            binding.tvDistance.setVisibility(View.VISIBLE);
        } else {
            binding.tvDistance.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.ll_trust_more:
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                intent.putExtra(IntentExtra.PUBLIC_KEY, sellerPk);
                ActivityUtil.startActivity(intent, this, TrustMoreActivity.class);
                break;
            case R.id.tv_direct_talk:
                if (null == user) {
                    return;
                }
                if (user.status == FriendStatus.DISCOVERED.getStatus()) {
                    userViewModel.addFriend(sellerPk);
                } else {
                    onBackPressed();
                    openChatActivity(sellerPk);
                }
                break;
            case R.id.tv_escrow_now:
                if (null == user) {
                    return;
                }
                intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                ActivityUtil.startActivity(intent, this, EscrowServiceActivity.class);
                break;
        }
    }

    /**
     * 打开聊天页面
     * @param chainID
     */
    private void openChatActivity(String chainID) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.CHAIN_ID, chainID);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(IntentExtra.TYPE, 1);
        intent.putExtra(IntentExtra.BEAN, user);
        ActivityUtil.startActivity(intent, this, MainActivity.class);
    }


    @Override
    protected void onStart() {
        super.onStart();
//        Disposable disposable = txViewModel.observeSellTxDetail(chainID, txID)
//            .subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe(this::showSellInfo);
//        disposables.add(disposable);
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BitmapUtil.recycleImageView(binding.leftView.ivHeadPic);
    }
}