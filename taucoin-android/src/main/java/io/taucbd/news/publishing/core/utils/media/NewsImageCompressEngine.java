package io.taucbd.news.publishing.core.utils.media;

import android.content.Context;
import android.graphics.BitmapFactory;

import com.luck.picture.lib.engine.CompressEngine;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.listener.OnCallbackListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

import io.taucbd.news.publishing.core.utils.DateUtil;
import io.taucbd.news.publishing.core.utils.FmtMicrometer;
import io.taucbd.news.publishing.core.utils.MultimediaUtil;

/**
 * 图片压缩引擎
 */
public class NewsImageCompressEngine implements CompressEngine {
    private final Logger logger = LoggerFactory.getLogger("NewsImageCompressEngine");

    @Override
    public void onCompress(Context context, List<LocalMedia> compressData, OnCallbackListener<List<LocalMedia>> listener) {
        // 1、使用自定义压缩框架进行图片压缩
        // 2、压缩成功后需要把compressData数据源中的LocalMedia里的isCompress和CompressPath字段赋值
        if (compressData != null && compressData.size() > 0) {
            for (LocalMedia media : compressData) {
                String path = MediaUtil.getImagePath(media);
                String compressPath = context.getApplicationInfo().dataDir + "/pic_temp/compress_" + DateUtil.getMillisTime() + ".webp";
                try {
                    File file = new File(compressPath);
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdir();
                    }
                    MultimediaUtil.compressImage(path, compressPath, MultimediaUtil.MAX_NEWS_IMAGE_WIDTH,
                            MultimediaUtil.MAX_NEWS_IMAGE_HEIGHT, MultimediaUtil.MAX_NEWS_IMAGE_SIZE, false);
                    media.setCompressed(true);
                    media.setCompressPath(compressPath);
                    logger.debug("path::{}", path);

                    logger.debug("是否压缩:" + media.isCompressed());
                    logger.debug("压缩路径:" + media.getCompressPath());
                    long compressSize = 0;
                    if (media.isCompressed()) {
                        BitmapFactory.Options options = MultimediaUtil.getBitmapOption(media.getCompressPath());
                        logger.debug("压缩宽高: " + options.outWidth + "x" + options.outHeight);
                        compressSize = new File(media.getCompressPath()).length();
                        logger.debug("压缩Size: " + compressSize);
                    }
                    logger.debug("原图:" + media.getPath());
                    logger.debug("绝对路径:" + media.getRealPath());

                    logger.debug("是否开启原图:" + media.isOriginal());
                    logger.debug("原图路径:" + media.getOriginalPath());
                    logger.debug("Android Q 特有Path:" + media.getAndroidQToPath());
                    logger.debug("宽高: " + media.getWidth() + "x" + media.getHeight());
                    logger.debug("Size: " + media.getSize());
                    if (media.getSize() > 0) {
                        logger.debug("压缩率: {}%", FmtMicrometer.formatTwoDecimal(compressSize * 100.0 / media.getSize()));
                    }
                } catch (IOException e) {
                    logger.error("compress error::" + path, e);
                }
            }
        }
        listener.onCall(compressData);
    }

    private NewsImageCompressEngine() {
    }

    private static NewsImageCompressEngine instance;

    public static NewsImageCompressEngine createCompressEngine() {
        if (null == instance) {
            synchronized (NewsImageCompressEngine.class) {
                if (null == instance) {
                    instance = new NewsImageCompressEngine();
                }
            }
        }
        return instance;
    }
}
