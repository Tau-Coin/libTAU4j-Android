package io.taucoin.torrent.publishing.ui.qrcode;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.HandlerThread;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.libTAU4j.Ed25519;
import com.google.gson.Gson;
import com.google.zxing.qrcode.QRCodeReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.camera.AndroidUtilities;
import io.taucoin.torrent.publishing.core.camera.Bitmaps;
import io.taucoin.torrent.publishing.core.camera.CameraController;
import io.taucoin.torrent.publishing.core.camera.CameraView;
import io.taucoin.torrent.publishing.core.camera.MyHandler;
import io.taucoin.torrent.publishing.core.camera.Size;
import io.taucoin.torrent.publishing.core.camera.Utilities;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.LinkUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.constant.PublicKeyQRContent;
import io.taucoin.torrent.publishing.ui.constant.SeedQRContent;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.friends.FriendsActivity;
import io.taucoin.torrent.publishing.ui.main.MainActivity;
import io.taucoin.torrent.publishing.ui.user.SeedActivity;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;
import io.taucoin.torrent.publishing.core.utils.rlp.ByteUtil;

/**
 * ??????QR Code????????????
 */
public class ScanQRCodeActivity extends BaseActivity implements View.OnClickListener, Camera.PreviewCallback {

    private static final Logger logger = LoggerFactory.getLogger("QRCode");
    private static final int CHOOSE_REQUEST = 0x100;
    private CompositeDisposable disposables = new CompositeDisposable();
    private Disposable disposable;
    private TextView tvNoQrCode;
    private CommunityViewModel communityViewModel;
    private UserViewModel userViewModel;
    private String friendPk; // ?????????????????????
    private boolean scanKeyOnly; // ????????????Key(Seed)

    private HandlerThread backgroundHandlerThread = new HandlerThread("ScanCamera");
    private MyHandler handler;
    private boolean recognized;
    private CameraView cameraView;
    private QRCodeReader qrReader;

    private static final int POINT_SIZE = 20;
    // ?????????????????????????????? ??????15??????
    private static final int scannerAnimationDelay = 15;
    private Paint paint = new Paint();
    private Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint laserPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Path path = new Path();
    private Rect frame;
    // ?????????????????????
    public int scannerStart = 0;
    // ?????????????????????
    public int scannerEnd = 0;
    // ???????????????
    private int scannerLineHeight;
    // ???????????????????????????
    private int scannerLineMoveDistance;
    // ???????????????
    private int laserColor;

    private CommonDialog longTimeCreateDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ActivityUtil.fullScreenAll(this);
        ActivityUtil.setRequestedOrientation(this);
        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //??????AppCompatActivity????????????
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_scan_qr_code);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        userViewModel = provider.get(UserViewModel.class);
        initView();
        initCameraView();
    }

    private void initCameraView() {
        CameraController.getInstance().initCamera(() -> {
            if (cameraView != null) {
                cameraView.initCamera();
            }
        });

        qrReader = new QRCodeReader();

        FrameLayout frameLayout = findViewById(R.id.frame_layout);

        paint.setColor(0x7f000000);
        cornerPaint.setColor(0xffffffff);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(AndroidUtilities.dimen(R.dimen.widget_size_4));
        cornerPaint.setStrokeJoin(Paint.Join.ROUND);

        scannerLineHeight = AndroidUtilities.dimen(R.dimen.widget_size_5);
        scannerLineMoveDistance = AndroidUtilities.dimen(R.dimen.widget_size_2);
        laserColor = getResources().getColor(R.color.color_blue_light);
        laserPaint.setColor(laserColor);

        ViewGroup viewGroup = new ViewGroup(this) {

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int width = MeasureSpec.getSize(widthMeasureSpec);
                int height = MeasureSpec.getSize(heightMeasureSpec);
                cameraView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
                setMeasuredDimension(width, height);
            }

            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                cameraView.layout(0, 0, cameraView.getMeasuredWidth(), cameraView.getMeasuredHeight());
            }

            @Override
            protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
                boolean result = super.drawChild(canvas, child, drawingTime);
                if (child instanceof CameraView) {
                    int size = (int) (Math.min(child.getWidth(), child.getHeight()) / 1.5f);
                    int x = (child.getWidth() - size) / 2;
                    int y = (child.getHeight() - size) / 2;
                    canvas.drawRect(0, 0, child.getMeasuredWidth(), y, paint);
                    canvas.drawRect(0, y + size, child.getMeasuredWidth(), child.getMeasuredHeight(), paint);
                    canvas.drawRect(0, y, x, y + size, paint);
                    canvas.drawRect(x + size, y, child.getMeasuredWidth(), y + size, paint);

                    if (null == frame) {
                        frame = new Rect(x, y, x + size, y + size);
                    }

                    if(scannerStart == 0 || scannerEnd == 0) {
                        scannerStart = frame.top;
                        scannerEnd = frame.bottom - scannerLineHeight;
                    }

                    // ???????????????
                    drawLaserScanner(canvas, frame);

                    path.reset();
                    path.moveTo(x, y + AndroidUtilities.dimen(R.dimen.widget_size_20));
                    path.lineTo(x, y);
                    path.lineTo(x + AndroidUtilities.dimen(R.dimen.widget_size_20), y);
                    canvas.drawPath(path, cornerPaint);

                    path.reset();
                    path.moveTo(x + size, y + AndroidUtilities.dimen(R.dimen.widget_size_20));
                    path.lineTo(x + size, y);
                    path.lineTo(x + size - AndroidUtilities.dimen(R.dimen.widget_size_20), y);
                    canvas.drawPath(path, cornerPaint);

                    path.reset();
                    path.moveTo(x, y + size - AndroidUtilities.dimen(R.dimen.widget_size_20));
                    path.lineTo(x, y + size);
                    path.lineTo(x + AndroidUtilities.dimen(R.dimen.widget_size_20), y + size);
                    canvas.drawPath(path, cornerPaint);

                    path.reset();
                    path.moveTo(x + size, y + size - AndroidUtilities.dimen(R.dimen.widget_size_20));
                    path.lineTo(x + size, y + size);
                    path.lineTo(x + size - AndroidUtilities.dimen(R.dimen.widget_size_20), y + size);
                    canvas.drawPath(path, cornerPaint);

                    postInvalidateDelayed(scannerAnimationDelay,
                            frame.left - POINT_SIZE,
                            frame.top - POINT_SIZE,
                            frame.right + POINT_SIZE,
                            frame.bottom + POINT_SIZE);
                }
                return result;
            }
        };
        viewGroup.setOnTouchListener((v, event) -> true);
        viewGroup.setBackgroundColor(0xff000000);
        frameLayout.addView(viewGroup, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        cameraView = new CameraView(this);
        cameraView.setUseMaxPreview(true);
        cameraView.setOptimizeForBarcode(true);
        cameraView.setDelegate(new CameraView.CameraViewDelegate() {
            @Override
            public void onCameraCreated(Camera camera) {

            }

            @Override
            public void onCameraInit() {
                startRecognizing();
            }
        });

        viewGroup.addView(cameraView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private void startRecognizing() {
        backgroundHandlerThread.start();
        handler = new MyHandler(backgroundHandlerThread.getLooper());
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (!recognized && cameraView != null && cameraView.getCameraSession() != null) {
                    cameraView.getCameraSession().setOneShotPreviewCallback(ScanQRCodeActivity.this);
                    AndroidUtilities.runOnUIThread(this, 500);
                }
            }
        });
    }

    /**
     * ???????????????
     */
    private void initView() {
        if (getIntent() != null) {
            scanKeyOnly = getIntent().getBooleanExtra(IntentExtra.SCAN_KEY_ONLY, false);
        }
        tvNoQrCode = findViewById(R.id.tv_no_qr_code);
        cameraView = findViewById(R.id.camera_view);
    }

    /**
     * ??????????????????
     */
    private void handleScanResult(String scanResult) {
        try {
            if (StringUtil.isNotEmpty(scanResult)) {
                logger.info("scanResult::{}", scanResult);
                LinkUtil.Link decode = LinkUtil.decode(scanResult);
                if (decode.isChainLink()) {
                    String chainID = decode.getData();
                    openChainLink(chainID, decode);
                    return;
                } else if (decode.isFriendLink() && ByteUtil.toByte(decode.getPeer()).length == Ed25519.PUBLIC_KEY_SIZE) {
                    String nickname = decode.getData();
                    openFriendLink(nickname, decode);
                    return;
                }
                SeedQRContent content = new Gson().fromJson(scanResult, SeedQRContent.class);
                String seed = content.getSeed();
                if (Utils.isKeyValid(seed)) {
                    if (scanKeyOnly) {
                        // ?????????????????????????????????
                        Intent intent = new Intent();
                        intent.putExtra(IntentExtra.DATA, scanResult);
                        setResult(RESULT_OK, intent);
                        onBackPressed();
                    } else {
                        // ????????????????????????
                        userViewModel.importSeed(seed, content.getNickName());
                    }
                    return;
                }
                // ???????????????
                PublicKeyQRContent publicKeyQRContent = new Gson().fromJson(scanResult, PublicKeyQRContent.class);
                if (publicKeyQRContent != null &&
                        ByteUtil.toByte(publicKeyQRContent.getPublicKey()).length == Ed25519.PUBLIC_KEY_SIZE) {
                    friendPk = publicKeyQRContent.getPublicKey();
                    userViewModel.addFriend(publicKeyQRContent.getPublicKey(), content.getNickName());
                    return;
                }
            }
        } catch (Exception e){
            logger.error("handleScanResult::{}", e.getMessage());
        }
        if (cameraView != null && cameraView.getCameraSession() != null) {
            cameraView.getCameraSession().resume();
        }
        resumeTextTip();
    }

    private void resumeTextTip() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        } else {
            tvNoQrCode.setText(R.string.contacts_error_invalid_qr);
        }
        disposable = Observable.timer(5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(aLong -> {
                    tvNoQrCode.setText(R.string.qr_code_scan_qr);
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        //??????activity???onStart
        subscribeAddCommunity();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cameraView != null && cameraView.getCameraSession() != null) {
            cameraView.getCameraSession().resume();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (cameraView != null && cameraView.getCameraSession() != null) {
            cameraView.getCameraSession().stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //??????activity???onDestroy

        if (cameraView != null) {
            cameraView.destroy(false, null);
            cameraView = null;
        }
        backgroundHandlerThread.quitSafely();

        disposables.clear();
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }

        if (longTimeCreateDialog != null && longTimeCreateDialog.isShowing()) {
            longTimeCreateDialog.closeDialog();
        }
    }

    private void subscribeAddCommunity(){
        communityViewModel.getAddCommunityState().observe(this, result -> {
            if (result.isSuccess()) {
                openCommunityActivity(result.getMsg());
            } else {
                ToastUtils.showShortToast(R.string.community_added_failed);
            }
        });

        userViewModel.getAddFriendResult().observe(this, result -> {
            openFriendActivity();
        });

        userViewModel.getChangeResult().observe(this, result -> {
            if(StringUtil.isNotEmpty(result)){
                ToastUtils.showShortToast(result);
            }
            openSeedActivity();
        });
    }

    /**
     * ??????Seed??????
     */
    private void openSeedActivity() {
        ActivityUtil.startActivity(this, SeedActivity.class);
        onBackPressed();
    }

    /**
     * ????????????????????????
     */
    private void openFriendActivity() {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(IntentExtra.PUBLIC_KEY, friendPk);
        ActivityUtil.startActivity(intent, this, FriendsActivity.class);
        onBackPressed();
    }

    /**
     * ??????????????????
     */
    private void openCommunityActivity(String chainID) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.CHAIN_ID, chainID);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(IntentExtra.TYPE, 0);
        ActivityUtil.startActivity(intent, this, MainActivity.class);
        onBackPressed();
    }

    /**
     * ??????chain link
     * @param chainID
     */
    private void openChainLink(String chainID, LinkUtil.Link link) {
        if (longTimeCreateDialog != null && longTimeCreateDialog.isShowing()) {
            longTimeCreateDialog.closeDialog();
        }
        longTimeCreateDialog = communityViewModel.showLongTimeCreateDialog(this, link,
                new CommonDialog.ClickListener() {
                    @Override
                    public void proceed() {
                        // ?????????
                        userViewModel.addFriend(link.getPeer(), null);
                        communityViewModel.addCommunity(chainID, link);
                    }

                    @Override
                    public void close() {
                        ScanQRCodeActivity.this.finish();
                    }
        });
    }

    /**
     * ??????Friend link
     * @param nickname
     */
    private void openFriendLink(String nickname, LinkUtil.Link link) {
        if (longTimeCreateDialog != null && longTimeCreateDialog.isShowing()) {
            longTimeCreateDialog.closeDialog();
        }
        longTimeCreateDialog = communityViewModel.showLongTimeCreateDialog(this, link,
                new CommonDialog.ClickListener() {
                    @Override
                    public void proceed() {
                        // ?????????
                        friendPk = link.getPeer();
                        userViewModel.addFriend(friendPk, nickname);
                    }

                    @Override
                    public void close() {
                        ScanQRCodeActivity.this.finish();
                    }
                });
    }

    /**
     * ??????????????????
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.iv_back:
                onBackPressed();
                break;
            case R.id.iv_qr_code:
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.TYPE, UserQRCodeActivity.TYPE_QR_SHARE);
                ActivityUtil.startActivity(intent, this, UserQRCodeActivity.class);
                break;
            case R.id.iv_gallery:
                startPhotoSelectActivity();
                break;
        }
    }

    public void startPhotoSelectActivity() {
        try {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, CHOOSE_REQUEST);
        } catch (Exception e) {
            logger.error("startPhotoSelectActivity ", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode){
                case CHOOSE_REQUEST:
                    handleSelectedImage(data);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * ?????????????????????
     * @param data
     * */
    private void handleSelectedImage(Intent data) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<String>) emitter -> {
            String scanResult = "";
            if (data != null && data.getData() != null) {
                try {
                    Point screenSize = AndroidUtilities.getRealScreenSize();
                    Bitmap bitmap = Bitmaps.loadBitmap(null, data.getData(), screenSize.x, screenSize.y,
                            true);
                    String text = Utilities.tryReadQr(qrReader, null, null, 0, 0, 0, bitmap);
                    if (StringUtil.isNotEmpty(text)) {
                        scanResult = text;
                    }
                } catch (Throwable e) {
                    logger.error("handleSelectedImage ", e);
                }
            }
            emitter.onNext(scanResult);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (StringUtil.isEmpty(result)) {
                        resumeTextTip();
                    } else {
                        handleScanResult(result);
                    }
                });
        disposables.add(disposable);
    }

    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        handler.post(() -> {
            try {
                Size size = cameraView.getPreviewSize();
                int format = camera.getParameters().getPreviewFormat();
                int side = (int) (Math.min(size.getWidth(), size.getHeight()) / 1.5f);
                int x = (size.getWidth() - side) / 2;
                int y = (size.getHeight() - side) / 2;

                String text = Utilities.tryReadQr(qrReader, data, size, x, y, side, null);
                if (StringUtil.isNotEmpty(text)) {
                    recognized = true;
                    camera.stopPreview();

                    AndroidUtilities.runOnUIThread(() -> {
                        handleScanResult(text);
                    });
                }
            } catch (Throwable ignore) { }
        });
    }

    /**
     * ?????????????????????
     * @param canvas
     * @param frame
     */
    private void drawLaserScanner(Canvas canvas, Rect frame) {
        drawLineScanner(canvas,frame);
        laserPaint.setShader(null);
    }

    /**
     * ?????????????????????
     * @param canvas
     * @param frame
     */
    private void drawLineScanner(Canvas canvas,Rect frame){
        //????????????
        LinearGradient linearGradient = new LinearGradient(
                frame.left, scannerStart,
                frame.left, scannerStart + scannerLineHeight,
                shadeColor(laserColor),
                laserColor,
                Shader.TileMode.MIRROR);

        laserPaint.setShader(linearGradient);
        if(scannerStart <= scannerEnd) {
            //??????
            RectF rectF = new RectF(frame.left + 2 * scannerLineHeight, scannerStart,
                    frame.right - 2 * scannerLineHeight, scannerStart + scannerLineHeight);
            canvas.drawOval(rectF, laserPaint);
            scannerStart += scannerLineMoveDistance;
        } else {
            scannerStart = frame.top;
        }
    }

    /**
     * ??????????????????
     * @param color
     * @return
     */
    public int shadeColor(int color) {
        String hax = Integer.toHexString(color);
        String result = "01"+hax.substring(2);
        return Integer.valueOf(result, 16);
    }
}
