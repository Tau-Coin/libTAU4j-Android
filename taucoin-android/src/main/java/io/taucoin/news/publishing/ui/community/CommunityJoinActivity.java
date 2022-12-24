package io.taucoin.news.publishing.ui.community;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.utils.LinkUtil;
import io.taucoin.news.publishing.core.utils.StringUtil;
import io.taucoin.news.publishing.core.utils.ToastUtils;
import io.taucoin.news.publishing.core.utils.ViewUtils;
import io.taucoin.news.publishing.databinding.ActivityCommunityJoinBinding;
import io.taucoin.news.publishing.databinding.ExternalErrorLinkDialogBinding;
import io.taucoin.news.publishing.ui.BaseActivity;
import io.taucoin.news.publishing.ui.customviews.CommonDialog;
import io.taucoin.news.publishing.ui.customviews.ConfirmDialog;
import io.taucoin.news.publishing.ui.user.UserViewModel;

/**
 * 社区加入页面
 */
@Deprecated
public class CommunityJoinActivity extends BaseActivity {

    private ActivityCommunityJoinBinding binding;
    private CommunityViewModel communityViewModel;
    private UserViewModel userViewModel;
    private CommonDialog linkDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        userViewModel = provider.get(UserViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_community_join);
        initLayout();
        subscribeAddCommunity();
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.community_join_title);
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
            String communityLink = ViewUtils.getText(binding.etLink);
            if (StringUtil.isNotEmpty(communityLink)) {
                handleCommunityLink(communityLink);
            } else {
                showErrorLinkDialog();
            }
        }
        return true;
    }

    private void subscribeAddCommunity(){
        communityViewModel.getAddCommunityState().observe(this, result -> {
            if (result.isSuccess()) {
                ToastUtils.showShortToast(R.string.community_join_successfully);
                this.finish();
            } else {
                ToastUtils.showShortToast(R.string.community_join_failed);
            }
        });
    }

    private void handleCommunityLink(String url) {
        LinkUtil.Link decode = LinkUtil.decode(url);
        if (decode.isChainLink()) {
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
            showErrorLinkDialog();
        }
    }

    private void showErrorLinkDialog() {
        ExternalErrorLinkDialogBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.external_error_link_dialog, null, false);
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