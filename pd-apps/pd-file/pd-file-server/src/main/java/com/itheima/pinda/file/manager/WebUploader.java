package com.itheima.pinda.file.manager;

import com.itheima.pinda.file.dto.chunk.FileUploadDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 封装分片操作的工具类,创建分片临时文件和分片存放的目录
 */
@Slf4j
@Component
public class WebUploader {
    /**
     * 为分片上传创建对应的保存位置,还可以创建临时文件 .tmp
     *
     * @param fileUploadDTO
     * @param path
     * @return
     */
    public java.io.File getReadySpace(FileUploadDTO fileUploadDTO, String path) {
        // 创建文件路径
        boolean flag = createFileFolder(path, false);
        if (!flag) {
            return null;
        }
        // 获取文件md5名
        String fileFolder = fileUploadDTO.getName();
        // 非空判断
        if (fileFolder == null) {
            return null;
        }
        path += "/" + fileFolder;
        // 创建临时文件和分片文件的目录
        flag = createFileFolder(path, true);
        if (!flag) {
            return null;
        }
        // 构造需要上传的分片文件对应的路径 D:\\uploadFile\\2023\\01\\${md5}\\0
        // getChunk()是获取当前是第几个分片文件
        return new java.io.File(path, String.valueOf(fileUploadDTO.getChunk()));
    }

    /**
     * 创建临时文件夹
     *
     * @return
     */
    private boolean createFileFolder(String file, boolean hasTmp) {
        // 临时文件(或目录)
        java.io.File tmpFile = new java.io.File(file);
        // 如果此临时文件不存在
        if (!tmpFile.exists()) {
            // 创建
            try {
                // 创建目录,可以创建多层
                tmpFile.mkdirs();
            } catch (Exception e) {
                log.error("创建文件分片所在目录失败", e);
                return false;
            }
        }
        if (hasTmp) {
            // 需要创建临时文件
            tmpFile = new java.io.File(file + ".tmp"); // D:\\uploadFile\\2023\\01\\abc.tmp
            if (tmpFile.exists()) {
                // 已经存在该临时文件,就修改临时文件的最后更新时间
                return tmpFile.setLastModified(System.currentTimeMillis());
            } else {
                // 临时文件不存在,需要创建
                try {
                    // 创建临时文件
                    tmpFile.createNewFile();
                } catch (IOException e) {
                    log.error("创建分片对应的临时文件失败");
                    return false;
                }
            }
        }
        return true;
    }
}
