package io.taucoin.tauapp.publishing.ui.friends;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.tauapp.publishing.MainApplication;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.tauapp.publishing.core.utils.ActivityUtil;
import io.taucoin.tauapp.publishing.core.utils.LinkUtil;
import io.taucoin.tauapp.publishing.core.utils.ChainIDUtil;
import io.taucoin.tauapp.publishing.core.utils.CopyManager;
import io.taucoin.tauapp.publishing.core.utils.FmtMicrometer;
import io.taucoin.tauapp.publishing.core.utils.SpanUtils;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.core.utils.ToastUtils;
import io.taucoin.tauapp.publishing.databinding.ActivityAirdropDetailBinding;
import io.taucoin.tauapp.publishing.ui.BaseActivity;
import io.taucoin.tauapp.publishing.ui.community.CommunityViewModel;
import io.taucoin.tauapp.publishing.ui.constant.IntentExtra;

/**
 * Airdrop详情页
 */
public class AirdropDetailActivity extends BaseActivity implements View.OnClickListener {

    private ActivityAirdropDetailBinding binding;
    private CommunityViewModel communityViewModel;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private String chainID;
    private Member member;
    private Disposable airdropDisposable;
    private String airdropLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_airdrop_detail);
        binding.setListener(this);
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

        String airdropPeer = MainApplication.getInstance().getPublicKey();
        airdropLink = LinkUtil.encodeAirdrop(airdropPeer, chainID);
        binding.tauLink.setText(airdropLink);
    }

    private void updateAirdropDetail(Member member) {
        this.member = member;
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
        getMenuInflater().inflate(R.menu.menu_airdrop, menu);
        return true;
    }

    /**
     * 右上角Menu选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_delete) {
            communityViewModel.deleteAirdropBot(chainID);
        } else if (item.getItemId() == R.id.menu_history) {
            if (member != null) {
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, member.chainID);
                intent.putExtra(IntentExtra.PUBLIC_KEY, member.publicKey);
                intent.putExtra(IntentExtra.TIMESTAMP, member.airdropTime);
                ActivityUtil.startActivity(intent, this, AirdropHistoryActivity.class);
            }
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

    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
        if (airdropDisposable != null && !airdropDisposable.isDisposed()) {
            airdropDisposable.dispose();
        }
    }

    @Override
    public void onClick(View v) {
        if (StringUtil.isEmpty(airdropLink)) {
            return;
        }
        switch (v.getId()) {
            case R.id.iv_link_copy:
                CopyManager.copyText(airdropLink);
                ToastUtils.showShortToast(R.string.copy_link_successfully);
                break;
            case R.id.ll_share:
                if (StringUtil.isEmpty(airdropLink)) {
                    return;
                }
                String communityName = ChainIDUtil.getName(chainID);
                String shareTitle = getString(R.string.bot_share_airdrop_link_title);
                String text = getString(R.string.bot_share_airdrop_link_content, communityName, airdropLink);
                ActivityUtil.shareText(this, shareTitle, text);
                break;
        }
    }
}
