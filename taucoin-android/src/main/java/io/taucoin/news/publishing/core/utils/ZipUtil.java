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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
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

    /**
     * tar打包，GZip压缩
     *
     * @param file    待压缩的文件或文件夹
     * @param taos    压缩流
     * @param baseDir 相对压缩文件的相对路径
     */
    private static void tarGZip(File file, TarArchiveOutputStream taos, String baseDir) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                tarGZip(f, taos, baseDir + file.getName() + File.separator);
            }
        } else {
            byte[] buffer = new byte[1024];
            int len;
            FileInputStream fis = null;
            TarArchiveEntry tarArchiveEntry = null;
            try {
                fis = new FileInputStream(file);
                tarArchiveEntry = new TarArchiveEntry(baseDir + file.getName());
                tarArchiveEntry.setSize(file.length());
                taos.putArchiveEntry(tarArchiveEntry);
                while ((len = fis.read(buffer)) != -1) {
                    taos.write(buffer, 0, len);
                }
                taos.flush();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fis != null) fis.close();
                    if (tarArchiveEntry != null) taos.closeArchiveEntry();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * tar打包，GZip压缩
     *
     * @param srcFile 待压缩的文件或文件夹
     */
    public static void tarGZip(File srcFile) {
        tarGZip(srcFile, srcFile.getParentFile().getAbsolutePath());
    }

    /**
     * tar打包，GZip压缩
     *
     * @param srcFile 待压缩的文件或文件夹
     * @param dstDir  压缩至该目录，保持原文件名，后缀改为zip
     */
    public static void tarGZip(File srcFile, String dstDir) {
        File file = new File(dstDir);
        //需要判断该文件存在，且是文件夹
        if (!file.exists() || !file.isDirectory()) file.mkdirs();
        // 先打包成tar格式
        String dstTarPath = dstDir + File.separator + getTarFileName(srcFile) + ".tar";
        String dstPath = dstTarPath + ".gz";
        FileOutputStream fos = null;
        TarArchiveOutputStream taos = null;
        try {
            fos = new FileOutputStream(dstTarPath);
            taos = new TarArchiveOutputStream(fos);
            tarGZip(srcFile, taos, "");
            taos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                //关闭数据流的时候要先关闭外层，否则会报Stream Closed的错误
                if (taos != null) taos.close();
                if (fos != null) fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        File tarFile = new File(dstTarPath);
        fos = null;
        GZIPOutputStream gzip = null;
        FileInputStream fis = null;
        try {
            //再压缩成gz格式
            fos = new FileOutputStream(dstPath);
            gzip = new GZIPOutputStream(fos);
            fis = new FileInputStream(tarFile);
            int len;
            byte[] buffer = new byte[1024];
            while ((len = fis.read(buffer)) != -1) {
                gzip.write(buffer, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) fis.close();
                //关闭数据流的时候要先关闭外层，否则会报Stream Closed的错误
                if (gzip != null) gzip.close();
                if (fos != null) fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //删除生成的tar临时文件
        if (tarFile.exists()) tarFile.delete();
    }

    public static String getTarFileName(File file) {
        String name = "";
        if (file != null) {
            name = file.getName();
            name = name.substring(0, name.indexOf("."));
        }
        return name;
    }

    public static String getTarFileNameAndType(File file) {
        return getTarFileName(file) + ".tar.gz";
    }
}
