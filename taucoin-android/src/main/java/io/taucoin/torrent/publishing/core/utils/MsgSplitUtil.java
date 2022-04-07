package io.taucoin.torrent.publishing.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.service.LibJpegManager;

/**
 * 聊天消息拆分工具
 */
public class MsgSplitUtil {
    private static final Logger logger = LoggerFactory.getLogger("MsgSplit");

    /**
     * 拆分文本消息
     * @param textMsg 文本消息
     * @return 拆分的消息列表
     */
    public static List<byte[]> splitTextMsg(String textMsg) {
        List<byte[]> list = new ArrayList<>();
        int msgSize = textMsg.length();
        int statPos = 0;
        int endPos = 1;
        byte[] lastFragmentBytes = null;
        do {
            if (endPos >= msgSize) {
                endPos = msgSize;
            }
            String fragment = textMsg.substring(statPos, endPos);
            byte[] fragmentBytes = fragment.getBytes(StandardCharsets.UTF_8);
            if (fragmentBytes.length > Constants.MSG_MAX_BYTE_SIZE) {
                // 切片字节大于限制，上一次的切片作为最新切片
                statPos = endPos - 1;
                if (lastFragmentBytes != null) {
                    list.add(lastFragmentBytes);
                }
            } else if (fragmentBytes.length == Constants.MSG_MAX_BYTE_SIZE || endPos == msgSize) {
                // 切片字节等于限制或者消息结束，当前切片为最新切片
                statPos = endPos;
                list.add(fragmentBytes);
            } else {
                // 切片字节小于限制，直接跳到下一切片
                endPos += 1;
            }
            lastFragmentBytes = fragmentBytes;
        } while (statPos < msgSize);
        return list;
    }

    /**
     * 处理消息图片成DAG形式
     */
    public static String compressAndScansPic(String originalPath) throws Exception {
        String compressPath = LibJpegManager.getCompressFilePath();
        String progressivePath = LibJpegManager.getProgressiveFilePath();
        long startTime = System.currentTimeMillis();
        // 压缩图片
        MultimediaUtil.compressImage(originalPath, compressPath);
        long endTime = System.currentTimeMillis();
        logger.debug("compressImage:: times::{}ms", endTime - startTime);
        LibJpegManager.jpegScans(compressPath, progressivePath);
        return progressivePath;
    }

    /**
     * 拆分图片消息
     * @param progressivePath 渐进式图片路径
     * @return 拆分的消息列表
     */
    public static List<byte[]> splitPicMsg(String progressivePath) throws IOException {
        List<byte[]> list = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        File file = new File(progressivePath);
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[Constants.MSG_MAX_BYTE_SIZE];
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
        logger.debug("splitPicMsg end times::{}ms", endTime - startTime);
        return list;
    }
}