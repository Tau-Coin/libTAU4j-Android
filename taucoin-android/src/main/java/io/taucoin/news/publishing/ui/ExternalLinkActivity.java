package io.taucoin.news.publishing.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.databinding.DataBindingUtil;
import io.taucoin.news.publishing.BuildConfig;
import io.taucoin.news.publishing.R;
import io.taucoin.news.publishing.core.utils.LinkUtil;
import io.taucoin.news.publishing.databinding.ExternalErrorLinkDialogBinding;
import io.taucoin.news.publishing.ui.community.CommunityViewModel;
import io.taucoin.news.publishing.ui.constant.IntentExtra;
import io.taucoin.news.publishing.ui.customviews.CommonDialog;
import io.taucoin.news.publishing.ui.customviews.ConfirmDialog;
import io.taucoin.news.publishing.ui.main.MainActivity;

/**
 * 外部点击TAUchain link跳转页面
 */

public class ExternalLinkActivity extends BaseActivity {
    private static final Logger logger = LoggerFactory.getLogger("ExternalLink");
    public static final String ACTION_CHAIN_LINK_CLICK = BuildConfig.APPLICATION_ID + ".ui.ACTION_CHAIN_LINK_CLICK";
    public static final String ACTION_AIRDROP_LINK_CLICK = BuildConfig.APPLICATION_ID + ".ui.ACTION_AIRDROP_LINK_CLICK";
    public static final String ACTION_FRIEND_LINK_CLICK = BuildConfig.APPLICATION_ID + ".ui.ACTION_FRIEND_LINK_CLICK";

    private ConfirmDialog longTimeCreateDialog;
    private CommonDialog linkDialog;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setIsFullScreen(false);
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (null != uri) {
            String urlLink = uri.toString();
            LinkUtil.Link link = LinkUtil.decode(urlLink);
            if (link.isAirdropLink() || link.isChainLink() || link.isFriendLink()) {
                showLongTimeCreateDialog(link);
            } else {
                if (LinkUtil.isTauUrl(urlLink)) {
                    showErrorLinkDialog();
                } else {
                    this.finish();
                }
            }
        } else {
            this.finish();
        }

        overridePendingTransition(0, 0);
    }

    private void onAirdropLinkClick(LinkUtil.Link link) {
        showLongTimeCreateDialog(link);
        Intent mainIntent = new Intent(this.getApplicationContext(), MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mainIntent.setAction(ACTION_AIRDROP_LINK_CLICK);
        mainIntent.putExtra(IntentExtra.LINK, link.getLink());
        this.startActivity(mainIntent);
    }

    private void onChainLinkClick(LinkUtil.Link link) {
        showLongTimeCreateDialog(link);
        Intent mainIntent = new Intent(this.getApplicationContext(), MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mainIntent.setAction(ACTION_CHAIN_LINK_CLICK);
        mainIntent.putExtra(IntentExtra.LINK, link.getLink());
        this.startActivity(mainIntent);
    }

    private void onFriendLinkClick(LinkUtil.Link link) {
        Intent mainIntent = new Intent(this.getApplicationContext(), MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mainIntent.setAction(ACTION_FRIEND_LINK_CLICK);
        mainIntent.putExtra(IntentExtra.LINK, link.getLink());
        this.startActivity(mainIntent);
    }

    private void showLongTimeCreateDialog(LinkUtil.Link link) {
        if (longTimeCreateDialog != null && longTimeCreateDialog.isShowing()) {
            longTimeCreateDialog.closeDialog();
        }
        longTimeCreateDialog = CommunityViewModel.showLongTimeCreateDialog(this, link, false,
                new ConfirmDialog.ClickListener() {
                    @Override
                    public void proceed() {
                        if (link.isAirdropLink()) {
                            onAirdropLinkClick(link);
                        } else if (link.isChainLink()) {
                            onChainLinkClick(link);
                        } else if (link.isFriendLink()) {
                            onFriendLinkClick(link);
                        }
                        finish();
                    }

                    @Override
                    public void close() {

                    }
                }
        );
    }

    private void showErrorLinkDialog() {
        logger.info("ExternalLinkActivity::error link clicked");
        ExternalErrorLinkDialogBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.external_error_link_dialog, null, false);
        dialogBinding.tvClose.setOnClickListener(v -> {
            if(linkDialog != null){
                linkDialog.closeDialog();
                this.finish();
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
        if (longTimeCreateDialog != null && longTimeCreateDialog.isShowing()) {
            longTimeCreateDialog.closeDialog();
        }
        if (linkDialog != null) {
            linkDialog.closeDialog();
        }
    }
}
