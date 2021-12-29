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
package io.taucoin.torrent.publishing.core.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.luck.picture.lib.compress.Luban;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 * Multimedia processing related logic processing:
 * compression, clipping, image extraction, audio extraction
 *
 * */
public class MultimediaUtil {
    private static final Logger logger = LoggerFactory.getLogger("MultimediaUtil");
    private static final int MAX_IMAGE_SIZE = 850;              // byte
    public static final int MAX_IMAGE_WIDTH = 100;              // px
    public static final int MAX_IMAGE_HEIGHT = 100;             // px

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
        compressImage(originPath, compressPath, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT);
    }

    public static void compressImage(String originPath, String compressPath, int maxWidth, int maxHeight)
            throws IOException {
        logger.debug("compressImage length::{}", new File(originPath).length());
        Bitmap tagBitmap = getSmallBitmap(originPath, maxWidth, maxHeight);
        int quality = 100;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        tagBitmap.compress(Bitmap.CompressFormat.WEBP, quality, baos);
        // 循环判断如果压缩后图片是否大于MAX_IMAGE_SIZE,大于继续压缩
        int minQuality = 20;
        int gzipSize = baos.toByteArray().length;
        while (gzipSize > MAX_IMAGE_SIZE) {
            quality -= 5;

            if (quality < minQuality) {
                int width = (int) (tagBitmap.getWidth() * 0.98);
                int height = (int) (tagBitmap.getHeight() * 0.98);
                tagBitmap = imageScale(tagBitmap, width, height, true);
                baos.reset();
                tagBitmap.compress(Bitmap.CompressFormat.WEBP, minQuality, baos);
                byte[] zipData = gZip(baos.toByteArray());
                if (zipData != null) {
                    gzipSize = zipData.length;
                }
                logger.debug("compressImage width::{}, height::{}, bytesCount::{}, minQuality::{}, gzipSize::{}",
                        width, height, baos.toByteArray().length, minQuality, gzipSize);
                continue;
            }
            baos.reset();
            tagBitmap.compress(Bitmap.CompressFormat.WEBP, quality, baos);
            byte[] zipData = gZip(baos.toByteArray());
            if (zipData != null) {
                gzipSize = zipData.length;
            }
            logger.debug("compressImage width::{}, height::{}, bytesCount::{}, minQuality::{}, gzipSize::{}",
                    tagBitmap.getWidth(), tagBitmap.getHeight(), baos.toByteArray().length, minQuality, gzipSize);
        }
        tagBitmap.recycle();

        FileOutputStream fos = new FileOutputStream(compressPath);
        fos.write(baos.toByteArray());
        fos.flush();
        fos.close();
        baos.close();
    }

    public static byte[] gZip(byte[] data) {
        byte[] zipData = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(bos);
            gzip.write(data);
            gzip.finish();
            gzip.close();
            zipData = bos.toByteArray();
            bos.close();
        } catch (Exception e) {
            logger.error("gZip error ::{}", e.getMessage());
        }
        return zipData;
    }

    public static byte[] unGZip(byte[] zipData) {
        byte[] data = null;
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(zipData);
            GZIPInputStream gzip = new GZIPInputStream(bis);
            byte[] buf = new byte[1024];
            int num;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while ((num = gzip.read(buf, 0, buf.length)) != -1) {
                bos.write(buf, 0, num);
            }
            data = bos.toByteArray();
            bos.flush();
            bos.close();
            gzip.close();
            bis.close();
        } catch (Exception e) {
            logger.error("unGZip error ::{}", e.getMessage());
        }
        return data;
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
}
