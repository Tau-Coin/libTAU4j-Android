package io.taucoin.news.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.taucoin.news.publishing.MainApplication;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.utils.ActivityUtil;
import io.taucoin.news.publishing.core.utils.LinkUtil;
import io.taucoin.news.publishing.core.utils.StringUtil;
import io.taucoin.news.publishing.core.utils.ViewUtils;
import io.taucoin.news.publishing.databinding.ActivityCommunityJoinBinding;
import io.taucoin.news.publishing.databinding.ActivityPasteLinkBinding;
import io.taucoin.news.publishing.databinding.ExternalAirdropLinkDialogBinding;
import io.taucoin.news.publishing.databinding.ExternalErrorLinkDialogBinding;
import io.taucoin.news.publishing.ui.BaseActivity;
import io.taucoin.news.publishing.ui.constant.IntentExtra;
import io.taucoin.news.publishing.ui.customviews.CommonDialog;
import io.taucoin.news.publishing.ui.customviews.ConfirmDialog;
import io.taucoin.news.publishing.ui.main.MainActivity;
import io.taucoin.news.publishing.ui.user.UserViewModel;

/**
 * 粘贴link页面
 */
public class PasteLinkActivity extends BaseActivity {

    private ActivityPasteLinkBinding binding;
    private CommunityViewModel communityViewModel;
    private UserViewModel userViewModel;
    private CommonDialog linkDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        userViewModel = provider.get(UserViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_paste_link);
        initLayout();
        subscribeAddCommunity();
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.drawer_paste_link);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    /**
     *  创建右上角Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_done, menu);
        return true;
    }

    /**
     * 右上角Menu选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 添加新社区处理事件
        if (item.getItemId() == R.id.menu_done) {
            String link = ViewUtils.getText(binding.etLink);
            if (StringUtil.isNotEmpty(link)) {
                handlePasteLink(link);
            } else {
                showErrorLinkDialog(false);
            }
        }
        return true;
    }

    private void subscribeAddCommunity(){
        communityViewModel.getAddCommunityState().observe(this, result -> {
            if (result.isSuccess()) {
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, result.getMsg());
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(IntentExtra.TYPE, 0);
                ActivityUtil.startActivity(intent, this, MainActivity.class);
            }
        });
    }

    private void handlePasteLink(String link) {
        LinkUtil.Link decode = LinkUtil.decode(link);
        if (decode.isAirdropLink()) {
            if (StringUtil.isEquals(MainApplication.getInstance().getPublicKey(),
                    decode.getPeer())) {
                showErrorLinkDialog(true);
                return;
            }
            communityViewModel.showLongTimeCreateDialog(this, decode,
                new ConfirmDialog.ClickListener() {
                    @Override
                    public void proceed() {
                        String airdropPeer = decode.getPeer();
                        userViewModel.addAirdropFriend(airdropPeer, decode);
                        String chainID = decode.getData();
                        communityViewModel.addCommunity(chainID, decode);
                    }

                    @Override
                    public void close() {

                    }
                });
        } else if (decode.isChainLink()) {
            communityViewModel.showLongTimeCreateDialog(this, decode, new ConfirmDialog.ClickListener() {
                @Override
                public void proceed() {
                    // 加朋友
                    userViewModel.addFriend(decode.getPeer(), null);
                    String chainID = decode.getData();
                    communityViewModel.addCommunity(chainID, decode);
                }

                @Override
                public void close() {

                }
            });
        } else {
            showErrorLinkDialog(false);
        }
    }

    private void showErrorLinkDialog(boolean isMyself) {
        ExternalErrorLinkDialogBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.external_error_link_dialog, null, false);
        if (isMyself) {
            dialogBinding.tvTips.setText(R.string.main_error_link_yourself);
        }
        dialogBinding.tvClose.setOnClickListener(v -> {
            if (linkDialog != null) {
                linkDialog.closeDialog();
            }
        });
        linkDialog = new CommonDialog.Builder(this)
                .setContentView(dialogBinding.getRoot())
                .setCanceledOnTouchOutside(false)
                .create();
        linkDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(linkDialog != null){
            linkDialog.closeDialog();
        }
    }

}