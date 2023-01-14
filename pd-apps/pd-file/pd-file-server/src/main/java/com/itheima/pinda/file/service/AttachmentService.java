package com.itheima.pinda.file.service;

import com.itheima.pinda.file.dto.AttachmentDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 附件-业务逻辑接口
 *
 * @author malguy-wang sir
 */
public interface AttachmentService {
    /**
     * 上传文件
     *
     * @param file
     * @param bizId    业务id
     * @param bizType  业务类型
     * @param id       文件id
     * @param isSingle 是否单文件
     * @return
     */
    public AttachmentDTO upload(MultipartFile file, Long bizId, String bizType, Long id, Boolean isSingle);
}
