package io.taucbd.news.publishing.ui.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.annotation.Nullable;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucbd.news.publishing.R;
import io.taucbd.news.publishing.core.storage.RepositoryHelper;
import io.taucbd.news.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucbd.news.publishing.core.storage.sqlite.repo.ChatRepository;
import io.taucbd.news.publishing.core.utils.Formatter;
import io.taucbd.news.publishing.core.utils.StringUtil;
import io.taucbd.news.publishing.core.utils.rlp.ByteUtil;

/**
 * 根据图片信息的Hash，递归获取全部信息并显示
 */
@SuppressLint("AppCompatCustomView")
public class HashImageView extends RoundImageView {

    private static final Logger logger = LoggerFactory.getLogger("HashImageView");
    private static final int heightLimit = 300;
    private static final int widthLimit = 300;
    private static final int loadBitmapLimit = 20;
    private ChatRepository chatRepo;
    private String imageHash;
    private byte[] senderPk;
    private byte[] cryptoKey;
    private byte[] totalBytes;
    private Disposable disposable;
    private boolean reload = false;
    private BitmapFactory.Options options;
    private int loadBitmapNum = 0;
    private boolean isLoadSuccess;

    public HashImageView(Context context) {
        this(context, null);
    }

    public HashImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HashImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        chatRepo = RepositoryHelper.getChatRepository(context);
        options = new BitmapFactory.Options();
    }

    private void showImage(Bitmap bitmap) {
        if (bitmap != null) {
            this.setImageBitmap(bitmap);
            loadBitmapNum += 1;
            if (loadBitmapNum >= loadBitmapLimit) {
                isLoadSuccess = true;
                disposable.dispose();
            }
            logger.debug("showImage imageHash::{}, loadBitmapNum::{}", imageHash, loadBitmapNum);
        } else {
            setImageResource(R.mipmap.icon_image_loading);
        }
    }

    /**
     * 设置ImageHash
     * @param imageHash
     * @param senderPk
     */
    public void setImageHash(String imageHash, String senderPk, byte[] cryptoKey) {
        // 如果是图片已加载，并且显示的图片不变，直接返回
        if (isLoadSuccess && totalBytes != null
                && StringUtil.isEquals(imageHash, this.imageHash)) {
            return;
        }
        this.cryptoKey = cryptoKey;
        this.imageHash = imageHash;
        this.senderPk = ByteUtil.toByte(senderPk);
        setImageHash(ByteUtil.toByte(imageHash), senderPk);
    }

    /**
     * 设置ImageHash
     * @param imageHash
     */
    private void setImageHash(byte[] imageHash, String senderPk) {
        logger.debug("setImageHash start::{}", this.imageHash);
        showImage(null);
        isLoadSuccess = false;
        loadBitmapNum = 0;
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
        totalBytes = null;
        disposable = Flowable.create((FlowableOnSubscribe<Bitmap>) emitter -> {
            try {
                if (imageHash != null) {
                    logger.debug("showHorizontalData start");
                    long startTime = System.currentTimeMillis();
                    showFragmentData(imageHash, senderPk, emitter);
                    long endTime = System.currentTimeMillis();
                    logger.debug("showImageFromDB times::{}ms", endTime - startTime);
                }
            } catch (InterruptedException ignore) {
            } catch (Exception e) {
                logger.error("showImageFromDB error", e);
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showImage);
    }

    /**
     * 递归显示图片切分的片段数据
     * @param imageHash
     * @param senderPk
     * @param emitter
     * @throws Exception
     */
    private void showFragmentData(byte[] imageHash, String senderPk,
                                  FlowableEmitter<Bitmap> emitter) throws Exception {
        if (emitter.isCancelled()) {
            return;
        }
        byte[] content = null;
        String hash = ByteUtil.toHexString(imageHash);
        ChatMsg chatMsg = chatRepo.queryChatMsg(senderPk, hash);
        if (chatMsg != null) {
            content = chatMsg.content;
        }
        if (!emitter.isCancelled()) {
            refreshImageView(content, emitter);
        }
    }

    private void refreshImageView(byte[] msgContent, FlowableEmitter<Bitmap> emitter) {
        if (totalBytes == null) {
            totalBytes = new byte[msgContent.length];
            System.arraycopy(msgContent, 0, totalBytes, 0, totalBytes.length);
        } else {
            byte[] tempBytes = new byte[totalBytes.length + msgContent.length];
            System.arraycopy(totalBytes, 0, tempBytes, 0, totalBytes.length);
            System.arraycopy(msgContent, 0, tempBytes, totalBytes.length, msgContent.length);
            totalBytes = tempBytes;
        }
        Bitmap bitmap = loadImageView();
        logger.debug("RefreshScanImages, bitmap::{}, size::{}", bitmap,
                Formatter.formatFileSize(getContext(), totalBytes.length));
        if (bitmap != null && !emitter.isCancelled()) {
            emitter.onNext(bitmap);
        }
    }

    private Bitmap loadImageView() {
        byte lastTwo = totalBytes[totalBytes.length - 2];
        byte lastOne = totalBytes[totalBytes.length - 1];
        totalBytes[totalBytes.length - 2] = -1;
        totalBytes[totalBytes.length - 1] = -39;
        options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(totalBytes, 0, totalBytes.length, options);
        int heightRatio = options.outHeight / heightLimit;
        int widthRatio = options.outWidth / widthLimit;
        if (heightRatio > 0 || widthRatio > 1) {
            options.inSampleSize = Math.max(heightRatio, widthRatio);
        }
        logger.debug("loadImageView::{}, {}, {}", options.outHeight, widthLimit, options.inSampleSize);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeByteArray(totalBytes, 0, totalBytes.length, options);
        totalBytes[totalBytes.length - 2] = lastTwo;
        totalBytes[totalBytes.length - 1] = lastOne;
        return bitmap;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // 加在View
        if (reload && StringUtil.isNotEmpty(imageHash)
                && disposable != null && disposable.isDisposed()) {
            setImageHash(imageHash, ByteUtil.toHexString(senderPk), cryptoKey);
        }
        reload = false;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // 销毁View
        if (disposable != null && !disposable.isDisposed()) {
            reload = true;
            disposable.dispose();
        }
    }
}