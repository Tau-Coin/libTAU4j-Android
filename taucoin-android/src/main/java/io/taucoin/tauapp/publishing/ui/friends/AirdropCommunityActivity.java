package io.taucoin.tauapp.publishing.ui.friends;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.tauapp.publishing.MainApplication;
import io.taucoin.tauapp.publishing.R;
import io.taucoin.tauapp.publishing.core.model.data.message.AirdropStatus;
import io.taucoin.tauapp.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.tauapp.publishing.core.utils.ActivityUtil;
import io.taucoin.tauapp.publishing.core.utils.ChainIDUtil;
import io.taucoin.tauapp.publishing.core.utils.FmtMicrometer;
import io.taucoin.tauapp.publishing.core.utils.LinkUtil;
import io.taucoin.tauapp.publishing.core.utils.CopyManager;
import io.taucoin.tauapp.publishing.core.utils.StringUtil;
import io.taucoin.tauapp.publishing.core.utils.ToastUtils;
import io.taucoin.tauapp.publishing.core.utils.UsersUtil;
import io.taucoin.tauapp.publishing.databinding.ActivityAirdropCommunityBinding;
import io.taucoin.tauapp.publishing.databinding.ExternalAirdropLinkDialogBinding;
import io.taucoin.tauapp.publishing.databinding.ExternalErrorLinkDialogBinding;
import io.taucoin.tauapp.publishing.ui.BaseActivity;
import io.taucoin.tauapp.publishing.ui.community.CommunityViewModel;
import io.taucoin.tauapp.publishing.ui.constant.IntentExtra;
import io.taucoin.tauapp.publishing.ui.customviews.CommonDialog;
import io.taucoin.tauapp.publishing.ui.customviews.ConfirmDialog;
import io.taucoin.tauapp.publishing.ui.main.MainActivity;
import io.taucoin.tauapp.publishing.ui.user.UserViewModel;

/**
 * 发币社区选择页面
 */
public class AirdropCommunityActivity extends BaseActivity implements
        AirdropListAdapter.OnClickListener {

    private ActivityAirdropCommunityBinding binding;
    private CommunityViewModel communityViewModel;
    private UserViewModel userViewModel;
    private AirdropListAdapter adapter;
    private Dialog linkDialog;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private boolean linksSelector = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        userViewModel = provider.get(UserViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_airdrop_community);
        initParam();
        initLayout();
        subscribeAddCommunity();
    }

    private void initParam() {
        if (getIntent() != null) {
            linksSelector = getIntent().getBooleanExtra(IntentExtra.LINKS_SELECTOR, false);
        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.drawer_airdrop_links);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());
        if (linksSelector) {
            binding.pasteLink.setVisibility(View.GONE);
        }

        adapter = new AirdropListAdapter(this, linksSelector);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.joinedList.setLayoutManager(layoutManager);
        binding.joinedList.setOnItemClickListener((view, adapterPosition) -> {
            // 选择社区退出返回数据
            Member member = adapter.getCurrentList().get(adapterPosition);
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.BALANCE, member.balance);
            intent.putExtra(IntentExtra.CHAIN_ID, member.chainID);
            if (member.airdropStatus == AirdropStatus.ON.getStatus()) {
                ActivityUtil.startActivity(intent, this, AirdropDetailActivity.class);
            } else {
                ActivityUtil.startActivity(intent, this, AirdropSetupActivity.class);
            }
        });
        binding.joinedList.setAdapter(adapter);

        communityViewModel.getJoinedUnexpiredList().observe(this, members -> {
            if (adapter != null) {
                adapter.submitList(members);
            }
        });

        binding.pasteLink.setOnClickListener(v -> {
            handleClipboardContent();
        });
    }

    /**
     *  创建右上角Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_done, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.menu_done);
        menuItem.setVisible(linksSelector);
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * 右上角Menu选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_done) {
            Member member = adapter.getMember();
            if (member != null) {
                AirdropStatus status = AirdropStatus.valueOf(member.airdropStatus);
                if (status == AirdropStatus.ON) {
                    String chainID = member.chainID;
                    String airdropPeer = MainApplication.getInstance().getPublicKey();
                    long airdropTime = member.airdropTime / 60 / 1000;
                    String airdropLink = LinkUtil.encodeAirdrop(airdropPeer, chainID, member.airdropCoins, airdropTime);
                    Intent intent = new Intent();
                    intent.putExtra(IntentExtra.AIRDROP_LINK, airdropLink);
                    setResult(RESULT_OK, intent);
                    this.finish();
                } else {
                    ToastUtils.showShortToast(R.string.tx_airdrop_setup);
                }
            } else {
                ToastUtils.showShortToast(R.string.tx_airdrop_select);
            }
        }
        return true;
    }

    /**
     * 订阅是否需要启动TauDaemon
     */
    private void subscribeAddCommunity(){
        communityViewModel.getAddCommunityState().observe(this, result -> {
            if(result.isSuccess()){
                openCommunityActivity(result.getMsg());
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        String userPk = MainApplication.getInstance().getPublicKey();
        communityViewModel.getJoinedUnexpiredCommunityList(userPk);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
        if(linkDialog != null){
            linkDialog.dismiss();
        }
    }

    @Override
    public void onShare(String chainID, long airdropCoins, long airdropTime) {
        if (StringUtil.isNotEmpty(chainID)) {
            String airdropPeer = MainApplication.getInstance().getPublicKey();
            String airdropLink = LinkUtil.encodeAirdrop(airdropPeer, chainID, airdropCoins, airdropTime);
            shareAirdropLink(chainID, airdropLink, airdropCoins);
        }
    }

    private void shareAirdropLink(String chainID, String airdropLink, long airdropCoins) {
        String airdropCoinsStr = FmtMicrometer.fmtLong(airdropCoins);
        String communityName = ChainIDUtil.getName(chainID);
        String shareTitle = getString(R.string.bot_share_airdrop_link_title);
        String text = getString(R.string.bot_share_airdrop_link_content, communityName, airdropCoinsStr,
                communityName, airdropLink);
        ActivityUtil.shareText(this, shareTitle, text);
    }

    private void handleClipboardContent() {
        String content = CopyManager.getClipboardContent(this);
        if (StringUtil.isNotEmpty(content)) {
            showOpenExternalLinkDialog(content);
        } else {
            showErrorLinkDialog(false);
        }
    }

    /**
     * 显示打开外部chain url的对话框（来自剪切板或外部链接）
     */
    private boolean showOpenExternalLinkDialog(String url) {
        LinkUtil.Link link = LinkUtil.decodeAirdropLink(url);
        if (link.isAirdropLink()) {
            if (StringUtil.isEquals(MainApplication.getInstance().getPublicKey(),
                    link.getPeer())) {
                showErrorLinkDialog(true);
                return false;
            }
            communityViewModel.showLongTimeCreateDialog(this, link,
                    new ConfirmDialog.ClickListener() {
                @Override
                public void proceed() {
                    showJoinDialog(link);
                }

                @Override
                public void close() {

                }
            });
            return true;
        }
        showErrorLinkDialog(false);
        return false;
    }

    private void showJoinDialog(LinkUtil.Link link) {
        ExternalAirdropLinkDialogBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.external_airdrop_link_dialog, null, false);
        String airdropPeerName = UsersUtil.getShowName(null, link.getPeer());
        String airdropPeerTip = getString(R.string.main_airdrop_link_peer, airdropPeerName);
        dialogBinding.tvPeer.setText(Html.fromHtml(airdropPeerTip));
        dialogBinding.ivSkip.setOnClickListener(v -> {
            if (linkDialog != null) {
                linkDialog.dismiss();
            }
        });
        dialogBinding.tvJoin.setOnClickListener(v -> {
            if (linkDialog != null) {
                linkDialog.dismiss();
            }
            openExternalAirdropLink(link);
        });
        linkDialog = new ConfirmDialog.Builder(this)
                .setContentView(dialogBinding.getRoot())
                .setCanceledOnTouchOutside(false)
                .create();
        linkDialog.show();
    }

    private void showErrorLinkDialog(boolean isMyself) {
        ExternalErrorLinkDialogBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.external_error_link_dialog, null, false);
        if (isMyself) {
            dialogBinding.tvTips.setText(R.string.main_error_link_yourself);
        }
        dialogBinding.tvClose.setOnClickListener(v -> {
            if(linkDialog != null){
                linkDialog.dismiss();
            }
        });
        linkDialog = new CommonDialog.Builder(this)
                .setContentView(dialogBinding.getRoot())
                .setCanceledOnTouchOutside(false)
                .create();
        linkDialog.show();
    }

    /**
     * 打开外部Airdrop link
     * @param link
     */
    private void openExternalAirdropLink(LinkUtil.Link link) {
        if (link.isAirdropLink()) {
            // 加朋友
            String airdropPeer = link.getPeer();
            userViewModel.addAirdropFriend(airdropPeer, link);
            openExternalChainLink(link);
        }
    }

    /**
     * 打开外部chain link
     * @param link
     */
    private void openExternalChainLink(LinkUtil.Link link) {
        String chainID = link.getData();
        communityViewModel.addCommunity(chainID, link);
    }

    /**
     * 打开社区页面
     * @param chainID
     */
    private void openCommunityActivity(String chainID){
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.CHAIN_ID, chainID);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(IntentExtra.TYPE, 0);
        ActivityUtil.startActivity(intent, this, MainActivity.class);
    }
}
