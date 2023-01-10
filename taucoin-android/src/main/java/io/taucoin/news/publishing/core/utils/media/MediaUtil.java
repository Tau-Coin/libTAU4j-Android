package io.taucoin.news.publishing.core.utils.media;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.language.LanguageConfig;
import com.luck.picture.lib.manager.PictureCacheManager;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.view.OverlayView;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

import cc.shinichi.library.ImagePreview;
import io.taucoin.news.publishing.MainApplication;
import io.taucoin.news.publishing.core.utils.MultimediaUtil;
import io.taucoin.news.publishing.core.utils.StringUtil;
import io.taucoin.news.publishing.core.utils.ZipUtil;

public class MediaUtil {

    public static String getImagePath(LocalMedia media) {
        String imagePath = media.getRealPath();
        if (media.isCut()) {
            imagePath = media.getCutPath();
        }
        if (media.isCompressed()) {
            imagePath = media.getCompressPath();
        }
        if (StringUtil.isEmpty(imagePath) && StringUtil.isNotEmpty(media.getAndroidQToPath())) {
            imagePath = media.getAndroidQToPath();
        }
        return imagePath;
    }

    public static byte[] media2Bytes(LocalMedia media) {
        String imagePath = getImagePath(media);
        byte[] data = file2Bytes(imagePath);
        return ZipUtil.gZip(data);
    }

    public static byte[] file2Bytes(String imagePath) {
        int byteSize = 1024;
        byte[] b = new byte[byteSize];
        try {
            FileInputStream fileInputStream = new FileInputStream(imagePath);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(byteSize);
            for (int length; (length = fileInputStream.read(b)) != -1;) {
                outputStream.write(b, 0, length);
            }
            fileInputStream.close();
            outputStream.close();
            return outputStream.toByteArray();
        } catch (IOException ignore) {
        }
        return null;
    }

    public static Bitmap bytes2Bitmap(byte[] bytes) {
        try {
            if (bytes != null) {
                byte[] unGzipData = ZipUtil.unGZip(bytes);
                return BitmapFactory.decodeByteArray(unGzipData, 0, unGzipData.length);
            }
        } catch (Exception ignore) { }
        return null;
    }

    public static void openGalleryAndCamera(Activity activity) {
        UCrop.Options options = new UCrop.Options();
        // 圆形头像模式
        options.setCircleDimmedLayer(true);
        options.setHideBottomControls(true);
        options.setShowCropFrame(false);
        options.setShowCropGrid(false);
        options.withAspectRatio(1, 1);
        options.withMaxResultSize(MultimediaUtil.MAX_IMAGE_WIDTH, MultimediaUtil.MAX_IMAGE_HEIGHT);
        options.setCompressionQuality(100);

        PictureSelector.create(activity)
            .openGallery(PictureMimeType.ofImage())
            .setLanguage(LanguageConfig.ENGLISH)
            .imageEngine(GlideEngine.createGlideEngine())
            .isWeChatStyle(true)
            .selectionMode(PictureConfig.SINGLE)
            .isSingleDirectReturn(true)
            .isGif(false)
            .isPreviewImage(true)
            .isEnableCrop(true)
            .basicUCropConfig(options)
            .freeStyleCropMode(OverlayView.DEFAULT_FREESTYLE_CROP_MODE)// 裁剪框拖动模式
            .isCropDragSmoothToCenter(true)// 裁剪框拖动时图片自动跟随居中
            .withAspectRatio(1, 1)
            .cropImageWideHigh(MultimediaUtil.MAX_IMAGE_WIDTH, MultimediaUtil.MAX_IMAGE_HEIGHT)
            .isCompress(true)
            .compressQuality(100)
            .compressEngine(ImageCompressEngine.createCompressEngine())
            .minimumCompressSize(1)
            .forResult(PictureConfig.CHOOSE_REQUEST);
    }

    public static void openGalleryAndCameraWithoutCrop(Activity activity) {
        PictureSelector.create(activity)
                .openGallery(PictureMimeType.ofImage())
                .setLanguage(LanguageConfig.ENGLISH)
                .imageEngine(GlideEngine.createGlideEngine())
                .isWeChatStyle(true)
                .selectionMode(PictureConfig.SINGLE)
                .isSingleDirectReturn(true)
                .isGif(false)
                .isPreviewImage(true)
                .isEnableCrop(false)
                .isCompress(false)
//                .compressQuality(100)
//                .compressEngine(NewsImageCompressEngine.createCompressEngine())
//                .minimumCompressSize(1)
                .forResult(PictureConfig.CHOOSE_REQUEST);
    }

    public static void deleteAllCacheImageFile() {
        Context context = MainApplication.getInstance();
        PictureCacheManager.deleteCacheDirFile(context, PictureMimeType.ofImage());
    }

    public static void previewPicture(Context context, String imagePath) {
        ImagePreview.getInstance()
            // 上下文，必须是activity，不需要担心内存泄漏，本框架已经处理好；
            .setContext(context)
            // 设置从第几张开始看（索引从0开始）
            .setIndex(0)
            .setShowDownButton(true)// 是否显示下载按钮
            .setImage(imagePath)
            // 开启预览
            .start();
    }

    public static void previewThumbnailPicture(Context context, ImageView targetView, String picturePath) {
        Glide.with(context)
            .asBitmap()
            .load(picturePath)
            .fitCenter()
            .thumbnail(0.1f)
            .into(targetView);
    }
}