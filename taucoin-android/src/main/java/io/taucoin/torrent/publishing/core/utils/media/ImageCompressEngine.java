package io.taucoin.torrent.publishing.core.utils.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.luck.picture.lib.engine.CompressEngine;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.entity.MediaExtraInfo;
import com.luck.picture.lib.listener.OnCallbackListener;
import com.luck.picture.lib.tools.MediaUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

import io.taucoin.torrent.publishing.core.utils.MultimediaUtil;

/**
 * 图片压缩引擎
 */
public class ImageCompressEngine implements CompressEngine {
    private Logger logger = LoggerFactory.getLogger("ImageCompress");

    @Override
    public void onCompress(Context context, List<LocalMedia> compressData, OnCallbackListener<List<LocalMedia>> listener) {
        // 1、使用自定义压缩框架进行图片压缩
        // 2、压缩成功后需要把compressData数据源中的LocalMedia里的isCompress和CompressPath字段赋值
        if (compressData != null && compressData.size() > 0) {
            for (LocalMedia media : compressData) {
                String path = MediaUtil.getImagePath(media);
                String compressPath = path.replace("IMG_CROP", "IMG_COMP");
                int index = compressPath.lastIndexOf(".");
                compressPath = compressPath.substring(0, index) + ".webp";
                try {
                    MultimediaUtil.compressImage(path, compressPath);
                    media.setCompressed(true);
                    media.setCompressPath(compressPath);
                    logger.debug("path::{}", path);
                    logger.debug("compressPath::{}", compressPath);
                    BitmapFactory.Options options = MultimediaUtil.getBitmapOption(compressPath);
                    logger.debug("compress wxh: " + options.outWidth + "x" + options.outHeight);
                    logger.debug("compress size: " + new File(compressPath).length());

                    logger.debug("是否压缩:" + media.isCompressed());
                    logger.debug("压缩:" + media.getCompressPath());
                    if (media.isCompressed()) {
                        options = MultimediaUtil.getBitmapOption(media.getCompressPath());
                        logger.debug("压缩宽高: " + options.outWidth + "x" + options.outHeight);
                        logger.debug("压缩Size: " + new File(media.getCompressPath()).length());
                    }
                    logger.debug("原图:" + media.getPath());
                    logger.debug("绝对路径:" + media.getRealPath());
                    logger.debug("是否裁剪:" + media.isCut());
                    logger.debug("裁剪:" + media.getCutPath());
                    if (media.isCut()) {
                        options = MultimediaUtil.getBitmapOption(media.getCutPath());
                        logger.debug("裁剪宽高: " + options.outWidth + "x" + options.outHeight);
                        logger.debug("裁剪Size: " + new File(media.getCutPath()).length());
                    }
                    logger.debug("是否开启原图:" + media.isOriginal());
                    logger.debug("原图路径:" + media.getOriginalPath());
                    logger.debug("Android Q 特有Path:" + media.getAndroidQToPath());
                    logger.debug("宽高: " + media.getWidth() + "x" + media.getHeight());
                    logger.debug("Size: " + media.getSize());
                } catch (IOException e) {
                    logger.error("compress error::" + path, e);
                }
            }
        }
        listener.onCall(compressData);
    }

    private ImageCompressEngine() {
    }

    private static ImageCompressEngine instance;

    public static ImageCompressEngine createCompressEngine() {
        if (null == instance) {
            synchronized (ImageCompressEngine.class) {
                if (null == instance) {
                    instance = new ImageCompressEngine();
                }
            }
        }
        return instance;
    }
}
