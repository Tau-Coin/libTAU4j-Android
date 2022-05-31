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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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

    /**
     * 压缩文件
     * @param sourceFile 指定打包的源目录
     * @param tarGzPath    指定目标 tar 包的位置
     */
    public static void compress(String sourceFile, String tarGzPath) {
        logger.info("compress：{}", tarGzPath);
        TarArchiveOutputStream tarOs = null;
        try {
            // 创建一个 FileOutputStream 到输出文件（.tar.gz）
            FileOutputStream fos = new FileOutputStream(tarGzPath);
            // 创建一个 GZIPOutputStream，用来包装 FileOutputStream 对象
            GZIPOutputStream gos = new GZIPOutputStream(new BufferedOutputStream(fos));
            // 创建一个 TarArchiveOutputStream，用来包装 GZIPOutputStream 对象
            tarOs = new TarArchiveOutputStream(gos);
            // 使文件名支持超过 100 个字节
            tarOs.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            addFilesToTarGZ(sourceFile, "", tarOs);
        } catch (Exception e) {
            logger.error("compress，", e);
        } finally {
            try {
                if (tarOs != null) {
                    tarOs.close();
                }
            } catch (IOException ignore) {
            }
        }
    }

    /**
     * @param sourcePath 源文件
     * @param parent     源目录
     * @param tarArchive 压缩输出流
     * @throws IOException
     */
    private static void addFilesToTarGZ(String sourcePath, String parent,
                                        TarArchiveOutputStream tarArchive) throws IOException {
        File sourceFile = new File(sourcePath);
        // 获取新目录下的文件名称
        String fileName = parent.concat(sourceFile.getName());
        //打包压缩该文件
        tarArchive.putArchiveEntry(new TarArchiveEntry(sourceFile, fileName));
        if (sourceFile.isFile()) {
            FileInputStream fis = new FileInputStream(sourceFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            // 写入文件
            IOUtils.copy(bis, tarArchive);
            tarArchive.closeArchiveEntry();
            bis.close();
        } else if (sourceFile.isDirectory()) {
            // 因为是个文件夹，无需写入内容，关闭即可
            tarArchive.closeArchiveEntry();
            // 遍历文件夹下的文件
            for (File f : sourceFile.listFiles()) {
                // 递归遍历文件目录树
                addFilesToTarGZ(f.getAbsolutePath(), fileName + File.separator, tarArchive);
            }
        }
    }
}
