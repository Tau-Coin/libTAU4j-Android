package io.taucoin.torrent.publishing.ui.friends;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;

import org.libTAU4j.ChainURL;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.data.AirdropUrl;
import io.taucoin.torrent.publishing.core.model.data.message.AirdropStatus;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.ChainUrlUtil;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UrlUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.databinding.ActivityAirdropCommunityBinding;
import io.taucoin.torrent.publishing.databinding.ExternalAirdropLinkDialogBinding;
import io.taucoin.torrent.publishing.databinding.ExternalErrorLinkDialogBinding;
import io.taucoin.torrent.publishing.databinding.PromptDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.main.MainActivity;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 发币社区选择页面
 */
public class AirdropCommunityActivity extends BaseActivity implements
        AirdropListAdapter.OnClickListener {

    private ActivityAirdropCommunityBinding binding;
    private CommunityViewModel communityViewModel;
    private UserViewModel userViewModel;
    private AirdropListAdapter adapter;
    private Disposable linkDisposable;
    private CommonDialog linkDialog;
    private CommonDialog joinDialog;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        userViewModel = provider.get(UserViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_airdrop_community);
        initLayout();
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.drawer_airdrop_links);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new AirdropListAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.joinedList.setLayoutManager(layoutManager);
        binding.joinedList.setOnItemClickListener((view, adapterPosition) -> {
            // 选择社区退出返回数据
            Member member = adapter.getCurrentList().get(adapterPosition);
            Intent intent = new Intent();
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

    @Override
    protected void onStart() {
        super.onStart();
        String userPk = MainApplication.getInstance().getPublicKey();
        communityViewModel.getJoinedUnexpiredCommunityList(userPk);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (linkDisposable != null && !linkDisposable.isDisposed()) {
            linkDisposable.dispose();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
        if(linkDialog != null){
            linkDialog.closeDialog();
        }

        if(joinDialog != null){
            joinDialog.closeDialog();
        }
    }

    @Override
    public void onShare(String chainID) {
        if (linkDisposable != null && !linkDisposable.isDisposed()) {
            linkDisposable.dispose();
        }
        linkDisposable = communityViewModel.getCommunityMembersLimit(chainID, Constants.AIRDROP_LINK_BS_LIMIT)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(list -> {
                    if (StringUtil.isNotEmpty(chainID)) {
                        String airdropPeer = MainApplication.getInstance().getPublicKey();
                        String airdropLink = UrlUtil.encodeAirdropUrl(airdropPeer, chainID, list);
                        shareAirdropLink(airdropLink);
                    }
                });
    }

    private void shareAirdropLink(String airdropLink) {
        String shareTitle = getString(R.string.bot_share_airdrop_link_title);
        String text = getString(R.string.bot_share_airdrop_link_content,
                Constants.APP_SHARE_URL, airdropLink);
        ActivityUtil.shareText(this, shareTitle, text);
    }

    private void handleClipboardContent() {
        String content = CopyManager.getClipboardContent(this);
        if (StringUtil.isNotEmpty(content)) {
            boolean isShowLinkDialog = showOpenExternalLinkDialog(content);
            if (isShowLinkDialog) {
                CopyManager.clearClipboardContent();
            }
        } else {
            showErrorLinkDialog(false);
        }
    }

    /**
     * 显示打开外部chain url的对话框（来自剪切板或外部链接）
     */
    private boolean showOpenExternalLinkDialog(String url) {
        AirdropUrl airdropUrl = UrlUtil.decodeAirdropUrl(url);
        if (airdropUrl != null) {
            if (StringUtil.isEquals(MainApplication.getInstance().getPublicKey(),
                    airdropUrl.getAirdropPeer())) {
                showErrorLinkDialog(true);
                return false;
            }
            ExternalAirdropLinkDialogBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                    R.layout.external_airdrop_link_dialog, null, false);
            String airdropPeerName = UsersUtil.getShowName(null, airdropUrl.getAirdropPeer());
            String airdropPeerTip = getString(R.string.main_airdrop_link_peer, airdropPeerName);
            dialogBinding.tvPeer.setText(Html.fromHtml(airdropPeerTip));
            dialogBinding.tvSkip.setOnClickListener(v -> {
                if (linkDialog != null) {
                    linkDialog.closeDialog();
                }
            });
            dialogBinding.tvJoin.setOnClickListener(v -> {
                if (linkDialog != null) {
                    linkDialog.closeDialog();
                }
                openExternalAirdropLink(airdropUrl.getAirdropUrl());
            });
            linkDialog = new CommonDialog.Builder(this)
                    .setContentView(dialogBinding.getRoot())
                    .setCanceledOnTouchOutside(false)
                    .create();
            linkDialog.show();
            return true;
        }
        showErrorLinkDialog(false);
        return false;
    }

    private void showErrorLinkDialog(boolean isMyself) {
        ExternalErrorLinkDialogBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.external_error_link_dialog, null, false);
        if (isMyself) {
            dialogBinding.tvTips.setText(R.string.main_error_link_yourself);
        }
        dialogBinding.tvClose.setOnClickListener(v -> {
            if(linkDialog != null){
                linkDialog.closeDialog();
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
     * @param link Airdrop link
     */
    private void openExternalAirdropLink(String link) {
        AirdropUrl decode = UrlUtil.decodeAirdropUrl(link);
        if (decode != null) {
            // 加朋友
            String airdropPeer = decode.getAirdropPeer();
            userViewModel.addAirdropFriend(airdropPeer, decode.getChainID());
            // 加入社区
            String chainUrl = decode.getChainUrl();
            initJoinSuccessDialog(decode.getAirdropPeer());
            openExternalChainLink(chainUrl);
        }
    }

    /**
     * 打开外部chain link
     * @param link chain link
     */
    private void openExternalChainLink(String link) {
        ChainURL decode = ChainUrlUtil.decode(link);
        if (decode != null) {
            String chainID = decode.getChainID();
            disposables.add(communityViewModel.getCommunityByChainIDSingle(chainID)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(community -> {
                        openCommunityActivity(chainID);
                        if (joinDialog != null) {
                            joinDialog.closeDialog();
                        }
                    }, it -> {
                        communityViewModel.addCommunity(chainID, link);
                    }));
        }
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

    private void initJoinSuccessDialog(String airdropPeer) {
        PromptDialogBinding joinBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.prompt_dialog, null, false);
        String airdropPeerName = UsersUtil.getShowName(null, airdropPeer);
        String joinSuccess = getString(R.string.main_chain_join_success, airdropPeerName);
        joinBinding.tvTitle.setText(Html.fromHtml(joinSuccess));
        joinDialog = new CommonDialog.Builder(this)
                .setContentView(joinBinding.getRoot())
                .setCanceledOnTouchOutside(true)
                .create();
        joinDialog.setOnCancelListener(l -> joinDialog = null);
    }
}
