package org.malred.pinda.file.controller;

import com.itheima.pinda.base.BaseController;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @PostMapping("/upload")
    public 
}
