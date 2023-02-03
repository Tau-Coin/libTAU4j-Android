package io.taucbd.news.publishing.ui.qrcode;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.XMLReader;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucbd.news.publishing.MainApplication;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.Constants;
import io.taucbd.news.publishing.core.utils.ActivityUtil;
import io.taucbd.news.publishing.core.utils.ChainIDUtil;
import io.taucbd.news.publishing.core.utils.DrawablesUtil;
import io.taucbd.news.publishing.core.utils.LinkUtil;
import io.taucbd.news.publishing.core.utils.CopyManager;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.core.utils.ToastUtils;
import io.taucbd.news.publishing.databinding.ActivityCommunityQrCodeBinding;
import io.taucbd.news.publishing.ui.ScanTriggerActivity;
import io.taucbd.news.publishing.ui.community.CommunityViewModel;
import io.taucbd.news.publishing.ui.constant.IntentExtra;
import io.taucbd.news.publishing.ui.user.UserViewModel;

/**
 * 社区QR Code页面
 */
public class CommunityQRCodeActivity extends ScanTriggerActivity implements View.OnClickListener {

    private static final Logger logger = LoggerFactory.getLogger("QRCode");
    private final CompositeDisposable disposables = new CompositeDisposable();
    private ActivityCommunityQrCodeBinding binding;
    private CommunityViewModel communityViewModel;
    private UserViewModel userViewModel;
    private String chainID;
    private String chainUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ActivityUtil.setRequestedOrientation(this);
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_community_qr_code);
        binding.setListener(this);
        binding.qrCode.setListener(this);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        userViewModel = provider.get(UserViewModel.class);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        if (getIntent() != null) {
            chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
        }
        if (StringUtil.isEmpty(chainID)) {
            return;
        }
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.community_qr_code);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        String showName = ChainIDUtil.getName(chainID);
        binding.qrCode.tvName.setVisibility(View.GONE);
        binding.qrCode.tvQrCode.setVisibility(View.GONE);
        binding.qrCode.ivCopy.setVisibility(View.GONE);

        communityViewModel.getQRBitmap().observe(this, bitmap -> {
            binding.qrCode.ivQrCode.setImageBitmap(bitmap);
        });
        disposables.add(communityViewModel.observeLatestMiner(chainID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(miner -> {
                    String userPk = MainApplication.getInstance().getPublicKey();
                    chainUrl = LinkUtil.encodeChain(userPk, chainID, miner);
                    String qrExplainShare = getString(R.string.setting_community_qr_share, showName, Constants.TX_MAX_OVERDRAFT, chainUrl);
                    String qrExplain = getString(R.string.setting_community_qr_explain, showName, Constants.TX_MAX_OVERDRAFT, chainUrl, R.mipmap.icon_copy_text);
                    int size = getResources().getDimensionPixelSize(R.dimen.widget_size_16);
                    binding.tvQrExplain.setText(Html.fromHtml(qrExplain,
                        new Html.ImageGetter() {
                            @Override
                            public Drawable getDrawable(String source) {
                                return DrawablesUtil.getDrawable(CommunityQRCodeActivity.this,
                                        R.mipmap.icon_copy_text, size, size);
                            }
                        }, new Html.TagHandler() {
                            @Override
                            public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
                                if (tag.toLowerCase(Locale.getDefault()).equals("img")) {
                                    // 获取长度
                                    int len = output.length();
                                    output.setSpan(new ClickableSpan() {
                                        @Override
                                        public void onClick(@NonNull View widget) {
                                            CopyManager.copyText(qrExplainShare);
                                            ToastUtils.showShortToast(R.string.copy_share_link);
                                        }
                                    }, len - 1, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }
                            }
                        }));
                    binding.tvQrExplain.setMovementMethod(LinkMovementMethod.getInstance());
                    logger.info("chainUrl::{}", chainUrl);
                    communityViewModel.generateQRCode(this, chainUrl, this.chainID, showName);
                }));
    }

    /**
     *  创建右上角Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_qr_code, menu);
        MenuItem scanMenu = menu.findItem(R.id.menu_scan);
        scanMenu.setVisible(true);
        return true;
    }

    /**
     * 右上角Menu选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_share) {
            String communityName = ChainIDUtil.getName(chainID);
            String qrExplainShare = getString(R.string.setting_community_qr_share, communityName, Constants.TX_MAX_OVERDRAFT, chainUrl);
            ActivityUtil.shareText(this, getString(R.string.contacts_share_qr_code), qrExplainShare);
//            userViewModel.shareQRCode(this, binding.qrCode.ivQrCode.getDrawable(), 480);
        } else if (item.getItemId() == R.id.menu_scan) {
            openScanQRActivityAndExit();
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }

    /**
     * import seed和generate seed点击事件
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.ll_scan_qr_code) {
            openScanQRActivityAndExit();
        } else if (v.getId() == R.id.iv_copy) {
            if (StringUtil.isNotEmpty(chainUrl)) {
                CopyManager.copyText(chainUrl);
                ToastUtils.showShortToast(R.string.copy_share_link);
            }
        }
    }
}