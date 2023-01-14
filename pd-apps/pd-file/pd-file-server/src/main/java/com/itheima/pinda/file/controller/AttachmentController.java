package com.itheima.pinda.file.controller;

import com.itheima.pinda.base.BaseController;
import com.itheima.pinda.base.R;
import com.itheima.pinda.file.dto.AttachmentDTO;
import com.itheima.pinda.file.service.AttachmentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务--附件处理控制器
 *
 * @author Mr. wang-malred
 */
@Slf4j
@RestController
@RequestMapping("/attachment")
@Api(value = "附件", tags = "附件")
public class AttachmentController extends BaseController {
    @Autowired
    private AttachmentService attachmentService;

    /**
     * 上传附件
     *
     * @param file
     * @param bizId    业务id
     * @param bizType  业务类型
     * @param id       文件id
     * @param isSingle 是否单文件
     * @return
     */
    @PostMapping("/upload")
    // api生成文档相关注解
    @ApiOperation(value = "附件上传", notes = "附件上传")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "isSingle", value = "是否单文件", dataType = "boolean", paramType = "query"),
            @ApiImplicitParam(name = "id", value = "文件id", dataType = "long", paramType = "query"),
            @ApiImplicitParam(name = "bizId", value = "业务id", dataType = "long", paramType = "query"),
            @ApiImplicitParam(name = "bizType", value = "业务类型", dataType = "long", paramType = "query"),
            @ApiImplicitParam(name = "file", value = "附件", dataType = "MultipartFile", allowMultiple = true, required = true),
    })
    public R<AttachmentDTO> upload(
            // 如果文件名和value相同,可以省略
            @RequestParam(value = "file")
                    MultipartFile file,
            @RequestParam(value = "bizId", required = false)
                    Long bizId,
            @RequestParam(value = "bizType", required = false)
                    String bizType,
            @RequestParam(value = "id", required = false)
                    Long id,
            @RequestParam(value = "isSingle", required = false, defaultValue = "false")
                    Boolean isSingle
    ) {
        // 非空判断
        if (file == null || file.isEmpty()) {
            // 返回错误提示
            return this.fail("请求中必须包含有效文件");
        }
        // 执行上传逻辑
        AttachmentDTO attachmentDTO = attachmentService.upload(file, bizId, bizType, id, isSingle);
        return this.success(attachmentDTO);
    }
}