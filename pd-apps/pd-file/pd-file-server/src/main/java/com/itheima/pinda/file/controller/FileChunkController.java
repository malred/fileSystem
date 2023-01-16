package com.itheima.pinda.file.controller;

import com.itheima.pinda.base.BaseController;
import com.itheima.pinda.base.R;
import com.itheima.pinda.dozer.DozerUtils;
import com.itheima.pinda.file.dto.chunk.FileChunksMergeDTO;
import com.itheima.pinda.file.dto.chunk.FileUploadDTO;
import com.itheima.pinda.file.entity.File;
import com.itheima.pinda.file.manager.WebUploader;
import com.itheima.pinda.file.properties.FileServerProperties;
import com.itheima.pinda.file.service.FileService;
import com.itheima.pinda.file.strategy.FileChunkStrategy;
import com.itheima.pinda.file.strategy.FileStrategy;
import com.itheima.pinda.file.utils.FileDataTypeUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件分片上传
 */
@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/chunk")
@Api(value = "分片上传", tags = "分片上传")
public class FileChunkController extends BaseController {
    /**
     * 文件策略类,可以获得文件上传方法
     */
    @Autowired
    private FileStrategy fileStrategy;

    /**
     * 文件业务逻辑类,可以操作数据库
     *
     * @param fileUploadDTO
     * @param multipartFile
     * @return
     */
    @Autowired
    private FileService fileService;
    /**
     * 文件配置属性类
     */
    @Autowired
    private FileServerProperties fileServerProperties;
    /**
     * 生成分片文件目录的工具类
     */
    @Autowired
    private WebUploader webUploader;
    /**
     * 对象拷贝工具类
     */
    @Autowired
    private DozerUtils dozerUtils;

    /**
     * 文件分片策略处理类
     */
    @Autowired
    private FileChunkStrategy fileChunkStrategy;

    /**
     * 文件分片上传接口
     *
     * @param fileUploadDTO
     * @param multipartFile
     * @return
     * @throws Exception
     */
    @PostMapping("/upload")
    @ApiOperation(value = "分片上传", notes = "分片上传")
    public R<FileChunksMergeDTO> uploadFile(
            FileUploadDTO fileUploadDTO,
            @RequestParam(value = "file") MultipartFile multipartFile) throws Exception {
        log.info("接收到分片: " + multipartFile + "分片信息: " + fileUploadDTO);
        // 非空判断
        if (multipartFile == null || multipartFile.isEmpty()) {
            log.error("分片文件为空");
            return this.fail("分片文件为空");
        }
        // 判断是不是分片文件(大文件前端会分片,小文件前端不会分片,则没有分片信息)
        if (fileUploadDTO.getChunk() == null || fileUploadDTO.getChunks() <= 0) {
            // 当前上传没有分片,按普通文件上传
            File file = fileStrategy.upload(multipartFile);
            // 设置md5名称
            file.setFileMd5(fileUploadDTO.getMd5());
            // 保存到数据库
            fileService.save(file);
            return this.success(null);
        } else {
            // 分片上传
            // 获取配置的存放分片的临时目录
            String storagePath = fileServerProperties.getStoragePath(); // D:\\uploadFile
            // 临时目录拼接日期文件夹 -> D:\\uploadFile\\2023\\01
            String uploadFolder = FileDataTypeUtil.getUploadPathPrefix(storagePath);
            // 分配分片文件的存放位置 D:\\uploadFile\\2023\\01\\${md5}\\1,D:\\uploadFile\\2023\\01\\${md5}\\2 ...
            java.io.File targetFile = webUploader.getReadySpace(fileUploadDTO, uploadFolder);
            // 准备的存放位置为空,则报错
            if (targetFile == null) {
                return this.fail("分片上传失败");
            }
            // 将分片文件写入磁盘
            multipartFile.transferTo(targetFile);
            // 封装信息给前端,用于之后的分片合并
            FileChunksMergeDTO fileChunksMergeDTO = new FileChunksMergeDTO();
            // 原始文件名
            fileChunksMergeDTO.setSubmittedFileName(multipartFile.getOriginalFilename());
            // copy类的属性值
            dozerUtils.map(fileUploadDTO, fileChunksMergeDTO);
            return this.success(fileChunksMergeDTO);
        }
    }

    /**
     * 分片合并
     *
     * @param fileChunksMergeDTO
     * @return
     */
    @PostMapping("/merge")
    @ApiOperation(value = "分片合并", notes = "分片合并")
    public R<File> merge(FileChunksMergeDTO fileChunksMergeDTO) {
        R<File> fileR = fileChunkStrategy.chunksMerge(fileChunksMergeDTO);
        return fileR;
    }
}
