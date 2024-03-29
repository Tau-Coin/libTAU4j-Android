package io.taucbd.news.publishing.ui.qrcode;

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.utils.ActivityUtil;
import io.taucbd.news.publishing.core.utils.BitmapUtil;
import io.taucbd.news.publishing.core.utils.CopyManager;
import io.taucbd.news.publishing.core.utils.DrawablesUtil;
import io.taucbd.news.publishing.core.utils.SpanUtils;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.core.utils.ToastUtils;
import io.taucbd.news.publishing.core.utils.UsersUtil;
import io.taucbd.news.publishing.databinding.ActivityQrCodeBinding;
import io.taucbd.news.publishing.ui.ScanTriggerActivity;
import io.taucbd.news.publishing.ui.constant.IntentExtra;
import io.taucbd.news.publishing.ui.constant.PublicKeyQRContent;
import io.taucbd.news.publishing.ui.constant.QRContent;
import io.taucbd.news.publishing.ui.user.UserViewModel;

/**
 * 用户QR Code页面
 */
public class UserQRCodeActivity extends ScanTriggerActivity implements View.OnClickListener {

    public static final int TYPE_QR_DISPLAY = 0x01;
    public static final int TYPE_QR_SHARE = 0x02;
    private ActivityQrCodeBinding binding;
    private UserViewModel userViewModel;
    private int type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ActivityUtil.setRequestedOrientation(this);
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_qr_code);
        binding.setListener(this);
        binding.qrCode.setListener(this);
        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        initParameter();
        initView();
    }

    private void initParameter() {
        type = getIntent().getIntExtra(IntentExtra.TYPE, TYPE_QR_DISPLAY);
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.qr_code_title);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        binding.llExportKey.setVisibility(type == TYPE_QR_DISPLAY ? View.VISIBLE : View.GONE);
        binding.llImportKey.setVisibility(type == TYPE_QR_DISPLAY ? View.VISIBLE : View.GONE);
        binding.qrCode.tvQrCode.setVisibility(View.GONE);
        binding.qrCode.ivCopy.setVisibility(View.GONE);
        loadQRCode();
        userViewModel.getChangeResult().observe(this, result -> {
            loadQRCode();
        });
    }

    private void loadQRCode() {
        userViewModel.queryCurrentUserAndFriends();
        userViewModel.getQRContent().observe(this, this::showQRCOdeImage);
        userViewModel.getQRBitmap().observe(this, bitmap -> {
            binding.qrCode.ivQrCode.setImageBitmap(bitmap);
        });
    }

    /**
     * 显示QRCode图片
     */
    private void showQRCOdeImage(QRContent content) {
        PublicKeyQRContent publicKeyQRContent = (PublicKeyQRContent) content;
        String midHideName = UsersUtil.getMidHideName(publicKeyQRContent.getPublicKey());
        SpannableStringBuilder stringBuilder = new SpanUtils()
                .append(getString(R.string.qr_code_tau_id))
                .setForegroundColor(getResources().getColor(R.color.gray_dark))
                .append(" ")
                .append(midHideName)
                .create();
        binding.qrCode.tvName.setText(stringBuilder);
        binding.qrCode.tvName.setTag(publicKeyQRContent.getPublicKey());
        DrawablesUtil.setEndDrawable(binding.qrCode.tvName, R.mipmap.icon_copy_text,
                getResources().getDimension(R.dimen.widget_size_18));
        userViewModel.generateQRCode(UserQRCodeActivity.this, content);
    }

    /**
     *  创建右上角Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_qr_code, menu);
        MenuItem scanMenu = menu.findItem(R.id.menu_scan);
        scanMenu.setVisible(type == TYPE_QR_DISPLAY);
        return true;
    }

    /**
     * 右上角Menu选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_share) {
            userViewModel.shareQRCode(this, binding.qrCode.ivQrCode.getDrawable(), 480);
        } else if (item.getItemId() == R.id.menu_scan) {
            openScanQRActivity();
        }
        return true;
    }

    /**
     * import seed和generate seed点击事件
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.ll_export_key) {
            ActivityUtil.startActivity(this, KeyQRCodeActivity.class);
        } else if (v.getId() == R.id.ll_import_key) {
            userViewModel.showSaveSeedDialog(this, false);
        } else if (v.getId() == R.id.tv_name) {
            String publicKey = StringUtil.getTag(binding.qrCode.tvName);
            CopyManager.copyText(publicKey);
            ToastUtils.showShortToast(R.string.copy_public_key);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (binding != null) {
            BitmapUtil.recycleImageView(binding.qrCode.ivQrCode);
            System.gc();
        }
    }
}