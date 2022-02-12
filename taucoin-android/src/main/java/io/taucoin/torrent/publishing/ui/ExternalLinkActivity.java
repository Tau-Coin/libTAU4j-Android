package io.taucoin.torrent.publishing.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import org.libTAU4j.ChainURL;

import io.taucoin.torrent.publishing.core.model.data.AirdropUrl;
import io.taucoin.torrent.publishing.core.utils.ChainUrlUtil;
import io.taucoin.torrent.publishing.core.utils.UrlUtil;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.main.MainActivity;

/**
 * 外部点击TAUchain link跳转页面
 */

public class ExternalLinkActivity extends BaseActivity {
    public static final String ACTION_CHAIN_LINK_CLICK = "io.taucoin.torrent.publishing.ui.ACTION_CHAIN_LINK_CLICK";
    public static final String ACTION_AIRDROP_LINK_CLICK = "io.taucoin.torrent.publishing.ui.ACTION_AIRDROP_LINK_CLICK";
    public static final String ACTION_ERROR_LINK_CLICK = "io.taucoin.torrent.publishing.ui.ACTION_ERROR_LINK_CLICK";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setIsFullScreen(false);
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (null != uri) {
            String urlLink = uri.toString();
            AirdropUrl airdropUrl = UrlUtil.decodeAirdropUrl(urlLink);
            if (airdropUrl != null) {
                Intent mainIntent = new Intent(this.getApplicationContext(), MainActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                mainIntent.setAction(ACTION_AIRDROP_LINK_CLICK);
                mainIntent.putExtra(IntentExtra.LINK, urlLink);
                this.startActivity(mainIntent);
            } else {
                ChainURL decode = ChainUrlUtil.decode(urlLink);
                if (decode != null) {
                    Intent mainIntent = new Intent(this.getApplicationContext(), MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    mainIntent.setAction(ACTION_CHAIN_LINK_CLICK);
                    mainIntent.putExtra(IntentExtra.LINK, urlLink);
                    this.startActivity(mainIntent);
                } else {
                    if (UrlUtil.isTauUrl(urlLink)) {
                        Intent mainIntent = new Intent(this.getApplicationContext(), MainActivity.class);
                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        mainIntent.setAction(ACTION_ERROR_LINK_CLICK);
                        this.startActivity(mainIntent);
                    }
                }
            }
        }

        finish();
        overridePendingTransition(0, 0);
    }
}
