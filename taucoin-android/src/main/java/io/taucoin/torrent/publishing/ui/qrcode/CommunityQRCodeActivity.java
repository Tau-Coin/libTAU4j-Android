package io.taucoin.torrent.publishing.ui.qrcode;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.ChainIDUtil;
import io.taucoin.torrent.publishing.core.utils.ChainUrlUtil;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.databinding.ActivityCommunityQrCodeBinding;
import io.taucoin.torrent.publishing.ui.ScanTriggerActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 社区QR Code页面
 */
public class CommunityQRCodeActivity extends ScanTriggerActivity implements View.OnClickListener {

    private static final Logger logger = LoggerFactory.getLogger("QRCode");
    private CompositeDisposable disposables = new CompositeDisposable();
    private ActivityCommunityQrCodeBinding binding;
    private CommunityViewModel communityViewModel;
    private UserViewModel userViewModel;
    private String chainID;
    private String chainUrl;
    private String chainUrlCopy;
    private int maxLines = 1;

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
        binding.qrCode.tvName.setText(showName);
        binding.qrCode.tvName.setMaxLines(maxLines);
        binding.qrCode.tvName.setEllipsize(TextUtils.TruncateAt.END);
        binding.qrCode.tvQrCode.setVisibility(View.GONE);
        binding.qrCode.ivCopy.setVisibility(View.GONE);

        // 获取10个社区成员的公钥
        disposables.add(communityViewModel.getCommunityMembersLimit(chainID, Constants.CHAIN_LINK_BS_LIMIT)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(list -> {
                    if (StringUtil.isNotEmpty(chainID)) {
                        chainUrl = ChainUrlUtil.encode(chainID, list);
                        if (list.size() > Constants.AIRDROP_LINK_BS_LIMIT) {
                            List<String> newList = list.subList(0, Constants.AIRDROP_LINK_BS_LIMIT);
                            chainUrlCopy = ChainUrlUtil.encode(chainID, newList);
                        } else {
                            chainUrlCopy = chainUrl;
                        }
                        binding.qrCode.tvName.setText(chainUrlCopy);
                        logger.info("chainUrl::{}", chainUrl);
                        communityViewModel.generateQRCode(this, chainUrl, this.chainID, showName);
                    }
                }));

        communityViewModel.getQRBitmap().observe(this, bitmap -> {
            binding.qrCode.ivQrCode.setImageBitmap(bitmap);
        });
    }

    /**
     *  创建右上角Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_qr_code, menu);
        return true;
    }

    /**
     * 右上角Menu选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_share) {
            userViewModel.shareQRCode(this, binding.qrCode.ivQrCode.getDrawable(), 480);
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
        } else if (v.getId() == R.id.iv_see) {
            if (StringUtil.isNotEmpty(chainUrlCopy)) {
                int maxLines = binding.qrCode.tvName.getMaxLines();
                if (this.maxLines == maxLines) {
                    binding.qrCode.tvName.setMaxLines(1000);
                    binding.ivSee.setImageResource(R.mipmap.icon_no_see);
                } else {
                    binding.qrCode.tvName.setMaxLines(this.maxLines);
                    binding.ivSee.setImageResource(R.mipmap.icon_see);
                }
            }
        } else if (v.getId() == R.id.iv_copy) {
            if (StringUtil.isNotEmpty(chainUrlCopy)) {
                CopyManager.copyText(chainUrlCopy);
                ToastUtils.showShortToast(R.string.copy_share_link);
            }
        }
    }
}