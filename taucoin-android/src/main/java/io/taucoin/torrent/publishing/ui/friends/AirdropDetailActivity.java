package io.taucoin.torrent.publishing.ui.friends;

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.Menu;
import android.view.MenuItem;


import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.utils.UrlUtil;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.SpanUtils;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.databinding.ActivityAirdropDetailBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;

/**
 * Airdrop详情页
 */
public class AirdropDetailActivity extends BaseActivity {

    private ActivityAirdropDetailBinding binding;
    private CommunityViewModel communityViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private String chainID;
    private Disposable airdropDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_airdrop_detail);
        initLayout();
    }

    private void initLayout() {
        chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);

        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(ChainIDUtil.getName(chainID));
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        communityViewModel.getAirdropResult().observe(this, result -> {
            this.finish();
        });

        binding.ivLinkCopy.setOnClickListener(v -> {
            CopyManager.copyText(StringUtil.getText(binding.tauLink));
            ToastUtils.showShortToast(R.string.copy_link_successfully);
        });
    }

    private void updateAirdropDetail(Member member) {
        if (member != null) {
            if (airdropDisposable != null && !airdropDisposable.isDisposed()) {
                airdropDisposable.dispose();
            }
            airdropDisposable = communityViewModel.observeAirdropCountOnChain(member.chainID,
                    member.publicKey, member.airdropTime)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(count -> {
                        int blueColor = getResources().getColor(R.color.color_blue_dark);
                        SpannableStringBuilder progress = new SpanUtils()
                                .append(FmtMicrometer.fmtLong(count))
                                .setForegroundColor(blueColor)
                                .append("/")
                                .append(String.valueOf(member.airdropMembers))
                                .create();
                        binding.tvProgress.setRightText(progress);

                        SpannableStringBuilder coinsUsage = new SpanUtils()
                                .append(FmtMicrometer.fmtBalance(member.airdropCoins * count))
                                .setForegroundColor(blueColor)
                                .append("/")
                                .append(FmtMicrometer.fmtBalance(member.airdropCoins * member.airdropMembers))
                                .create();
                        binding.tvCoinsUsage.setRightText(coinsUsage);
                    });
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     *  创建右上角Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_delete, menu);
        return true;
    }

    /**
     * 右上角Menu选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_delete) {
            communityViewModel.deleteAirdropBot(chainID);
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        disposables.add(communityViewModel.observeCommunityAirdropDetail(chainID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateAirdropDetail));

        // 获取3个社区成员的公钥
        disposables.add(communityViewModel.getCommunityMembersLimit(chainID, Constants.AIRDROP_LINK_BS_LIMIT)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(list -> {
                    if (StringUtil.isNotEmpty(chainID)) {
                        String airdropPeer = MainApplication.getInstance().getPublicKey();
                        String airdropLink = UrlUtil.encodeAirdropUrl(airdropPeer, chainID, list);
                        binding.tauLink.setText(airdropLink);
                    }
                }));
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
        if (airdropDisposable != null && !airdropDisposable.isDisposed()) {
            airdropDisposable.dispose();
        }
    }
}