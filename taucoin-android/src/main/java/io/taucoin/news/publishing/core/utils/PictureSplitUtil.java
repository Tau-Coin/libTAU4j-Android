package io.taucoin.news.publishing.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.taucoin.news.publishing.MainApplication;

/**
 * 图片拆分工具
 */
public class PictureSplitUtil {
    private static final Logger logger = LoggerFactory.getLogger("PictureSplitUtil");

    /**
     * 拆分图片消息
     * @param picturePath 图片路径
     * @return 拆分的消息列表
     */
    public static List<byte[]> splitPicture(String picturePath) throws IOException {
        List<byte[]> list = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        File file = new File(picturePath);
        logger.debug("splitPicture pictureSize::{}", file.length());
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[MultimediaUtil.MAX_IMAGE_SIZE];
        int num;
        byte[] msg;
        while (true) {
            num = fis.read(buffer);
            if (num != -1) {
                msg = new byte[num];
                System.arraycopy(buffer, 0, msg, 0, num);
                list.add(msg);
            } else {
                break;
            }
        }
        fis.close();
        long endTime = System.currentTimeMillis();
        logger.debug("splitPicture size::{}, end times::{}ms", list.size(), endTime - startTime);
        if (list.size() < MultimediaUtil.IMAGE_SLICES_NUM) {
            for (int i = list.size(); i < MultimediaUtil.IMAGE_SLICES_NUM; i ++) {
                list.add(new byte[]{});
            }
            logger.debug("splitPicture add size::{}", list.size());
        }
        for (int i = 0; i < list.size(); i++) {
            logger.debug("splitPicture slices {}::{}", i + 1, list.get(i).length);
        }
        return list;
    }

    /**
     * 判断切片是否存在
     * @param sliceDirName 新增切片文件目录名
     * @param sliceName 切片文件名（要包含顺序）
     * @return boolean 切片是否存在
     */
    public static boolean isSliceExists(String sliceDirName, String sliceName) {
        String filePath = MainApplication.getInstance().getApplicationInfo().dataDir;
        filePath += "/pic/" + sliceDirName;
        File fileDir = new File(filePath);
        if (!fileDir.exists()) {
           return false;
        }
        filePath += File.separator + sliceName;
        File sliceFile = new File(filePath);
        return sliceFile.exists();
    }

    /**
     * 判断切片获取是否完成
     * @param sliceDirName 新增切片文件目录名
     * @return boolean 切片获取是否完成
     */
    public static boolean isSlicesGetCompleted(String sliceDirName) {
        String filePath = MainApplication.getInstance().getApplicationInfo().dataDir;
        filePath += "/pic/" + sliceDirName;
        File fileDir = new File(filePath);
        if (!fileDir.exists()) {
            return false;
        }
        File[] files = fileDir.listFiles();
        return files != null && files.length >= MultimediaUtil.IMAGE_SLICES_NUM;
    }

    /**
     * 保存图片切片
     * @param sliceDirName 新增切片文件目录名
     * @param sliceName 切片文件名（要包含顺序）
     * @param slice 切片数据
     * @return String 切片合成后的图片路径
     * @throws IOException
     */
    public static String savePictureSlices(String sliceDirName, String sliceName, byte[] slice) throws IOException {
        String filePath = MainApplication.getInstance().getApplicationInfo().dataDir;
        filePath += "/pic/" + sliceDirName;
        File file = new File(filePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        filePath += File.separator + sliceName;
        FileOutputStream fos = new FileOutputStream(filePath);
        fos.write(slice);
        fos.flush();
        fos.close();

        if (isSlicesGetCompleted(sliceDirName)) {
            return mergePictureSlices(sliceDirName);
        }
        return null;
    }

    /**
     * 合并文件下图片切片
     * @param sliceDirName 图片切片的所在目录名
     * @throws IOException
     */
    private static String mergePictureSlices(String sliceDirName) throws IOException {
        String filePath = MainApplication.getInstance().getApplicationInfo().dataDir;
        filePath += "/pic/" + sliceDirName;
        File dirFile = new File(filePath);
        if (dirFile.exists()) {
            List<File> fileList = FileUtil.getFiles(filePath);
            Collections.sort(fileList, (o1, o2) -> {
                String fileName1 = o1.getName();
                String fileName2 = o2.getName();
                return fileName1.compareTo(fileName2);
            });
            if (fileList.size() > 0) {
                String outFileName = filePath + File.separator + sliceDirName + ".webp";
                FileOutputStream fileOutputStream = new FileOutputStream(outFileName);
                for (File file : fileList) {
                    logger.debug("fileName::{}", file.getName());
                    if (file.isFile()) {
                        FileInputStream fis = new FileInputStream(file);
                        byte[] buffer = new byte[MultimediaUtil.MAX_IMAGE_SIZE];
                        int num;
                        byte[] msg;
                        while (true) {
                            num = fis.read(buffer);
                            if (num != -1) {
                                msg = new byte[num];
                                System.arraycopy(buffer, 0, msg, 0, num);
                                fileOutputStream.write(msg);
                            } else {
                                break;
                            }
                        }
                        fis.close();
                    }
                }
                return outFileName;
            }
        }
        return null;
    }
}