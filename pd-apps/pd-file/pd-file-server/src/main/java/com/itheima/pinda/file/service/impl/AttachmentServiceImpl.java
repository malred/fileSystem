package com.itheima.pinda.file.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.pinda.base.id.IdGenerate;
import com.itheima.pinda.database.mybatis.conditions.Wraps;
import com.itheima.pinda.dozer.DozerUtils;
import com.itheima.pinda.file.dto.AttachmentDTO;
import com.itheima.pinda.file.entity.Attachment;
import com.itheima.pinda.file.entity.File;
import com.itheima.pinda.file.service.AttachmentService;
import com.itheima.pinda.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import com.itheima.pinda.file.dao.AttachmentMapper;
import com.itheima.pinda.file.properties.FileServerProperties;
import com.itheima.pinda.file.strategy.FileStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * 附件-业务逻辑类
 *
 * @author malguy-wang sir
 * @create ---
 */
@Slf4j
@Service
public class AttachmentServiceImpl
        extends ServiceImpl<AttachmentMapper, Attachment>
        implements AttachmentService {
    /**
     * 文件处理策略顶层接口
     */
    @Autowired
    private FileStrategy fileStrategy;

    /**
     * Id生成器
     *
     * @param file
     * @param bizId
     * @param bizType
     * @param id
     * @param isSingle
     * @return
     */
    @Autowired
    private IdGenerate<Long> idGenerate;
    /**
     * 复制类对象的工具类
     */
    @Autowired
    private DozerUtils dozerUtils;
    /**
     * 配置文件信息类
     */
    @Autowired
    private FileServerProperties fileServerProperties;

    /**
     * 上传文件
     *
     * @param multipartFile
     * @param bizId         业务id
     * @param bizType       业务类型
     * @param id            文件id
     * @param isSingle      是否单文件
     * @return
     */
    @Override
    public AttachmentDTO upload(MultipartFile multipartFile, Long bizId, String bizType, Long id, Boolean isSingle) {
        // 判断bizId是否为空
        String bizIdStr = String.valueOf(bizId);
        if (bizId == null) {
            // 为空需要生成一个(转为string是为了方便存放数据库)
            bizIdStr = String.valueOf(idGenerate.generate());
        }
        // 调用策略处理对象,实现真正的上传
        File file = fileStrategy.upload(multipartFile);
        // 因为附件是另一个实体类,所以需要转换
        Attachment attachment = dozerUtils.map(file, Attachment.class);
        // 设置attachment特有的属性
        attachment.setBizId(bizIdStr);
        attachment.setBizType(bizType);
        LocalDateTime now = LocalDateTime.now();
        attachment.setCreateMonth(DateUtils.formatAsYearMonthEn(now));
        attachment.setCreateWeek(DateUtils.formatAsYearWeekEn(now));
        attachment.setCreateDay(DateUtils.formatAsDateEn(now));
        // 判断当前业务是否单一文件
        if (isSingle) {
            // 需要将当前业务下的其他文件信息从数据库删除
            super.remove(Wraps.<Attachment>lbQ()
                    .eq(Attachment::getBizId, bizIdStr)
                    .eq(Attachment::getBizType, bizType));
        }
        // 完成文件上传后,保存文件信息到数据库
        // 如果id不为空,说明是修改
        if (id != null && id > 0) {
            attachment.setId(id);
            // 数据库修改操作
            super.updateById(attachment);
        } else {
            // 新增需要id,新生成一个
            attachment.setId(idGenerate.generate());
            // 执行新增操作
            super.save(attachment);
        }
        // 转换attachment为DTO对象,并返回
        return dozerUtils.map(attachment, AttachmentDTO.class);
    }
}
