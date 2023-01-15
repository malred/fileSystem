package com.itheima.pinda.file.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.pinda.file.dto.AttachmentDTO;
import com.itheima.pinda.file.dto.AttachmentResultDTO;
import com.itheima.pinda.file.dto.FilePageReqDTO;
import com.itheima.pinda.file.entity.Attachment;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

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

    /**
     * 删除附件
     *
     * @param ids
     */
    public void remove(Long[] ids);

    /**
     * 根据业务id/业务类型删除附件
     *
     * @param bizId
     * @param bizType
     */
    public void removeByBizIdAndBizType(String bizId, String bizType);

    /**
     * 根据文件id打包下载
     *
     * @param request
     * @param response
     * @param ids
     */
    public void download(HttpServletRequest request, HttpServletResponse response, Long[] ids) throws Exception;

    /**
     * 根据业务id和业务类型下载附件
     *
     * @param request
     * @param response
     * @param bizTypes
     * @param bizIds
     * @throws Exception
     */
    void downloadByBiz(HttpServletRequest request, HttpServletResponse response, String[] bizTypes, String[] bizIds) throws Exception;

    /**
     * 查询附件分页数据
     *
     * @param page
     * @param data
     * @return
     */
    IPage<Attachment> page(Page<Attachment> page, FilePageReqDTO data);

    /**
     * 根据业务类型和业务id查询附件
     *
     * @param bizTypes
     * @param bizIds
     * @return
     */
    List<AttachmentResultDTO> find(String[] bizTypes, String[] bizIds);

}
