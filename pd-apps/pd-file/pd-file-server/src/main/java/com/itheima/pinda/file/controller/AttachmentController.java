package com.itheima.pinda.file.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.ArrayUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.pinda.base.BaseController;
import com.itheima.pinda.base.R;
import com.itheima.pinda.exception.code.ExceptionCode;
import com.itheima.pinda.file.dto.AttachmentDTO;
import com.itheima.pinda.file.dto.AttachmentRemoveDTO;
import com.itheima.pinda.file.dto.AttachmentResultDTO;
import com.itheima.pinda.file.dto.FilePageReqDTO;
import com.itheima.pinda.file.entity.Attachment;
import com.itheima.pinda.file.service.AttachmentService;
import com.itheima.pinda.utils.BizAssert;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

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

    /**
     * 根据id删除文件
     *
     * @param ids id数组
     * @return
     */
    @DeleteMapping
    @ApiOperation(value = "删除附件", notes = "删除附件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids[]", value = "文件id数组", dataType = "array", paramType = "query")
    })
    public R<Boolean> remove(@RequestParam(value = "ids[]") Long[] ids) {
        // 执行删除逻辑
        attachmentService.remove(ids);
        return this.success(true);
    }

    /**
     * 根据业务类型或业务id删除文件
     *
     * @param dto
     * @return
     */
    @ApiOperation(value = "根据业务类型或业务id删除文件",
            notes = "根据业务类型或业务id删除文件")
    @DeleteMapping(value = "/biz")
    public R<Boolean> removeByBizIdAndBizType(@RequestBody AttachmentRemoveDTO dto) {
        attachmentService.removeByBizIdAndBizType(dto.getBizId(),
                dto.getBizType());
        return success(true);
    }

    /**
     * 根据文件id打包下载
     *
     * @param ids
     */
    @ApiOperation(value = "根据文件ids打包下载", notes = "根据文件ids打包下载")
    @GetMapping(value = "/download",
            // 数据使用的是二进制流的形式
            produces = "application/octet-stream")
    public void download(@RequestParam(value = "ids[]") Long[] ids,
                         HttpServletRequest request,
                         HttpServletResponse response) throws Exception {
        attachmentService.download(request, response, ids);
    }

    /**
     * 根据业务类型或者业务id其中之一，或者2个同时打包下载文件
     *
     * @param bizIds   业务id
     * @param bizTypes 业务类型
     */
    @ApiImplicitParams({
            @ApiImplicitParam(name = "bizIds[]", value = "业务id数组", dataType = "array", paramType = "query"),
            @ApiImplicitParam(name = "bizTypes[]", value = "业务类型数组", dataType = "array", paramType = "query"),
    })
    @ApiOperation(value = "根据业务类型/业务id打包下载", notes = "根据业务id下载一个文件或多个文件打包下载")
    @GetMapping(value = "/download/biz", produces = "application/octet-stream")
    public void downloadByBiz(
            @RequestParam(value = "bizIds[]", required = false) String[] bizIds,
            @RequestParam(value = "bizTypes[]", required = false) String[] bizTypes,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 非空判断
        BizAssert.isTrue(
                !(ArrayUtils.isEmpty(bizTypes) && ArrayUtils.isEmpty(bizIds))
                // 如果判断结果为false,抛出指定异常
                , ExceptionCode.BASE_VALID_PARAM.build("附件业务id和业务类型不能同时为空"));
        // 下载
        attachmentService.downloadByBiz(request, response, bizTypes, bizIds);
    }

    /**
     * 分页查询附件
     */
    @ApiOperation(value = "分页查询附件", notes = "分页查询附件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "current", value = "当前页", dataType = "long", paramType = "query", defaultValue = "1"),
            @ApiImplicitParam(name = "size", value = "每页显示几条", dataType = "long", paramType = "query", defaultValue = "10"),
    })
    @GetMapping(value = "/page")
    public R<IPage<Attachment>> page(FilePageReqDTO data) {
        Page<Attachment> page = getPage();
        attachmentService.page(page, data);
        return success(page);
    }

    /**
     * 根据业务类型/业务id查询附件
     *
     * @param bizTypes
     * @param bizIds
     * @return
     */
    @ApiOperation(value = "查询附件", notes = "查询附件")
    @ApiResponses(
            @ApiResponse(code = 60103, message = "文件id为空")
    )
    @GetMapping
    public R<List<AttachmentResultDTO>> findAttachment(@RequestParam(value = "bizTypes", required = false) String[] bizTypes,
                                                       @RequestParam(value = "bizIds", required = false) String[] bizIds) {
        //不能同时为空
        BizAssert.isTrue(!(ArrayUtils.isEmpty(bizTypes) && ArrayUtils.isEmpty(bizIds)), ExceptionCode.BASE_VALID_PARAM.build("业务类型不能为空"));
        return success(attachmentService.find(bizTypes, bizIds));
    }
}
