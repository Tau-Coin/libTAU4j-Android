/**
 * Copyright 2018 Taucoin Core Developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.taucoin.news.publishing.core.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.luck.picture.lib.entity.LocalMedia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import io.taucoin.news.publishing.MainApplication;
import io.taucoin.news.publishing.core.utils.media.MediaUtil;

/**
 *
 * Multimedia processing related logic processing:
 * compression, clipping, image extraction, audio extraction
 *
 * */
public class MultimediaUtil {
    private static final Logger logger = LoggerFactory.getLogger("MultimediaUtil");
    protected static final int MAX_IMAGE_SIZE = 988;              // byte
    public static final int MAX_IMAGE_WIDTH = 100;              // px
    public static final int MAX_IMAGE_HEIGHT = 100;             // px

    public static final int IMAGE_SLICES_NUM = 10;
    public static final int MAX_NEWS_IMAGE_SIZE = MAX_IMAGE_SIZE * IMAGE_SLICES_NUM; // byte
    public static final int MAX_NEWS_IMAGE_WIDTH = 960;         // px
    public static final int MAX_NEWS_IMAGE_HEIGHT = 540;        // px

    private static int calculateInSampleSize(BitmapFactory.Options options, int maxWidth, int maxHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > maxHeight || width > maxWidth) {
            final int heightRatio = Math.round((float) height/ (float) maxHeight);
            final int widthRatio = Math.round((float) width / (float) maxWidth);
            inSampleSize = Math.min(heightRatio, widthRatio);
        }
        return inSampleSize;
    }

    private static Bitmap imageScale(Bitmap originBitmap, int maxWidth, int maxHeight,
                                     boolean recycleOrigin) {
        int w = originBitmap.getWidth();
        int h = originBitmap.getHeight();
        float compressRatio = 1;
        if (h > maxHeight || w > maxWidth) {
            if(w >= h){
                compressRatio = (float) maxWidth / w;
            }else{
                compressRatio = (float) maxHeight / h;
            }
        }
        Matrix matrix = new Matrix();
        matrix.postScale(compressRatio, compressRatio);
        Bitmap newBitmap = Bitmap.createBitmap(originBitmap, 0, 0, w, h, matrix, true);
        if (recycleOrigin) {
            originBitmap.recycle();
        }
        return newBitmap;
    }

    public static void compressImage(String originPath, String compressPath)
            throws IOException {
        compressImage(originPath, compressPath, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT, MAX_IMAGE_SIZE, true);
    }

    public static void compressImage(String originPath, String compressPath, int maxWidth,
                                     int maxHeight, int maxImageSize, boolean isGzipCompress)
            throws IOException {
        logger.debug("compressImage length::{}", new File(originPath).length());
        Bitmap tagBitmap = getSmallBitmap(originPath, maxWidth, maxHeight);
        int quality = 100;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        tagBitmap.compress(Bitmap.CompressFormat.WEBP, quality, baos);
        // 循环判断如果压缩后图片是否大于MAX_IMAGE_SIZE,大于继续压缩
        int minQuality = 20;
        int imageSize = baos.toByteArray().length;
        while (imageSize > maxImageSize) {
            if (quality >= minQuality) {
                int times = imageSize / maxImageSize;
                if (times > 1) {
                    quality -= 15;
                } else {
                    quality -= 5;
                }
            }
            if (quality < minQuality) {
                float scale;
                int times = imageSize / maxImageSize;
                if (times > 1) {
                    scale = 0.8f;
                } else {
                    scale = 0.9f;
                }
                int width = (int) (tagBitmap.getWidth() * scale);
                int height = (int) (tagBitmap.getHeight() * scale);
                tagBitmap = imageScale(tagBitmap, width, height, true);
                baos.reset();
                tagBitmap.compress(Bitmap.CompressFormat.WEBP, minQuality, baos);
                if (isGzipCompress) {
                    byte[] zipData = ZipUtil.gZip(baos.toByteArray());
                    if (zipData != null) {
                        imageSize = zipData.length;
                    }
                } else  {
                    imageSize = baos.toByteArray().length;
                }
                logger.debug("compressImage width::{}, height::{}, bytesCount::{}, quality::{}, minQuality::{}, imageSize::{}",
                        width, height, baos.toByteArray().length, quality, minQuality, imageSize);
                continue;
            }
            baos.reset();
            tagBitmap.compress(Bitmap.CompressFormat.WEBP, quality, baos);
            if (isGzipCompress) {
                byte[] zipData = ZipUtil.gZip(baos.toByteArray());
                if (zipData != null) {
                    imageSize = zipData.length;
                }
            } else {
                imageSize = baos.toByteArray().length;
            }
            logger.debug("compressImage width::{}, height::{}, bytesCount::{}, quality::{}, minQuality::{}, imageSize::{}",
                    tagBitmap.getWidth(), tagBitmap.getHeight(), baos.toByteArray().length, quality, minQuality, imageSize);
        }
        tagBitmap.recycle();

        FileOutputStream fos = new FileOutputStream(compressPath);
        fos.write(baos.toByteArray());
        fos.flush();
        fos.close();
        baos.close();
    }

    private static Bitmap getSmallBitmap(String filePath, int maxWidth, int maxHeight) {
        BitmapFactory.Options options = getBitmapOption(filePath);
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(filePath, options);
    }

    public static BitmapFactory.Options getBitmapOption(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
        if (bitmap != null) {
            bitmap.recycle();
        }
        return options;
    }

    public static String newsImageCompress(LocalMedia media) {
        String path = MediaUtil.getImagePath(media);
        Context context = MainApplication.getInstance();
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
            return compressPath;
        } catch (IOException e) {
            logger.error("compress error::" + path, e);
        }
        return "";
    }
}
