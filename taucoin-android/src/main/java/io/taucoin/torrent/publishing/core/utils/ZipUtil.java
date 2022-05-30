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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Zip压缩
 */
public class ZipUtil {

    private static final Logger logger = LoggerFactory.getLogger("ZipUtil");

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

    /**
     * 压缩文件
     * @param sourceFile 源文件
     * @param destZipFile 目标压缩文件
     */
    public static void gZip(File sourceFile, String destZipFile) {
        GZIPOutputStream bos = null;
        try {
            bos = new GZIPOutputStream(new FileOutputStream(destZipFile));
            byte[] buf = new byte[1024];
            int len;
            FileInputStream in = new FileInputStream(sourceFile);
            while ((len = in.read(buf)) != -1) {
                bos.write(buf, 0, len);
            }
            bos.close();
            in.close();
        } catch (Exception e) {
            logger.error("gZip error ::{}", e.getMessage());
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException ignore) {
                }
            }
        }
    }
}
