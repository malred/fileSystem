package com.itheima.pinda.file.service.impl;

import cn.hutool.core.util.ArrayUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.pinda.base.id.IdGenerate;
import com.itheima.pinda.database.mybatis.conditions.Wraps;
import com.itheima.pinda.database.mybatis.conditions.query.LbqWrapper;
import com.itheima.pinda.dozer.DozerUtils;
import com.itheima.pinda.exception.BizException;
import com.itheima.pinda.file.biz.FileBiz;
import com.itheima.pinda.file.dao.AttachmentMapper;
import com.itheima.pinda.file.domain.FileDO;
import com.itheima.pinda.file.domain.FileDeleteDO;
import com.itheima.pinda.file.dto.AttachmentDTO;
import com.itheima.pinda.file.dto.AttachmentResultDTO;
import com.itheima.pinda.file.dto.FilePageReqDTO;
import com.itheima.pinda.file.entity.Attachment;
import com.itheima.pinda.file.entity.File;
import com.itheima.pinda.file.properties.FileServerProperties;
import com.itheima.pinda.file.service.AttachmentService;
import com.itheima.pinda.file.strategy.FileStrategy;
import com.itheima.pinda.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
     * 共用文件下载类
     */
    @Autowired
    private FileBiz fileBiz;

    /**
     * 上传附件
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

    /**
     * 根据id删除附件
     *
     * @param ids
     */
    @Override
    public void remove(Long[] ids) {
        if (ArrayUtil.isEmpty(ids)) {
            return;
        }
        // 0,查询数据库,获取被删除文件的信息
        // select * from pd_attachment where id in (1,2,...)
        List<Attachment> list = super.list(Wrappers.<Attachment>lambdaQuery()
                .in(Attachment::getId, ids));
        // 1,从数据库删除文件信息
        super.removeByIds(Arrays.asList(ids));
        log.debug("ids->{}", Arrays.asList(ids));
        //对象格式处理
        List<FileDeleteDO> fileDeleteDOList =
                list.stream().map((fi) -> FileDeleteDO.builder()
                        .relativePath(fi.getRelativePath()) //文件在服务器的相对路径
                        .fileName(fi.getFilename()) //唯一文件名
                        .group(fi.getGroup()) //fastDFS返回的组 用于FastDFS
                        .path(fi.getPath()) //fastdfs 的路径
                        .build())
                        .collect(Collectors.toList());
        log.debug("fileDeleteDOList->{}", fileDeleteDOList.toString());
        // 2,删除存放在磁盘的文件
        fileStrategy.delete(fileDeleteDOList);
    }

    /**
     * 根据业务id和业务类型删除附件
     *
     * @param bizId
     * @param bizType
     */
    @Override
    public void removeByBizIdAndBizType(String bizId, String bizType) {
        //根据业务类和业务id查询数据库
        List<Attachment> list = super.list(
                Wraps.<Attachment>lbQ()
                        .eq(Attachment::getBizId, bizId)
                        .eq(Attachment::getBizType, bizType));
        if (list.isEmpty()) {
            return;
        }
        //根据id删除文件
        remove(list.stream().mapToLong(
                Attachment::getId).boxed().toArray(Long[]::new));
    }

    /**
     * 根据文件id打包下载
     *
     * @param request
     * @param response
     * @param ids
     */
    @Override
    public void download(HttpServletRequest request, HttpServletResponse response, Long[] ids) throws Exception {
        // 根据文件id查询数据库
        List<Attachment> list = (List<Attachment>) super.listByIds(Arrays.asList(ids));
        // 下载
        down(list, request, response);
    }

    /**
     * 根据业务id和业务类型下载附件
     *
     * @param request
     * @param response
     * @param bizTypes
     * @param bizIds
     * @throws Exception
     */
    @Override
    public void downloadByBiz(HttpServletRequest request, HttpServletResponse response, String[] bizTypes, String[] bizIds) throws Exception {
        List<Attachment> list = super.list(
                Wraps.<Attachment>lbQ()
                        .in(Attachment::getBizType, bizTypes)
                        .in(Attachment::getBizId, bizIds));
        // 下载
        down(list, request, response);
    }

    /**
     * 公共转换list并下载的方法
     *
     * @param list
     * @param request
     * @param response
     * @throws Exception
     */
    public void down(List<Attachment> list, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 非空判断
        if (list.isEmpty()) {
            throw BizException.wrap("您下载的文件不存在!");
        }
        // 附件list转为filedo类型list
        List<FileDO> fileDOList = list.stream().map((file) -> FileDO.builder()
                .url(file.getUrl())
                .submittedFileName(file.getSubmittedFileName())
                .size(file.getSize())
                .dataType(file.getDataType())
                .build()
        ).collect(Collectors.toList());
        // 下载
        fileBiz.down(fileDOList, request, response);
    }

    /**
     * 查询附件分页数据
     *
     * @param page
     * @param data
     * @return
     */
    @Override
    public IPage<Attachment> page(Page<Attachment> page, FilePageReqDTO data) {
        Attachment attachment = dozerUtils.map(data, Attachment.class);
        // ${ew.customSqlSegment} 语法一定要手动eq like 等 不能用lbQ!
        LbqWrapper<Attachment> wrapper = Wraps.<Attachment>lbQ()
                .like(Attachment::getSubmittedFileName, attachment.getSubmittedFileName())
                .like(Attachment::getBizType, attachment.getBizType())
                .like(Attachment::getBizId, attachment.getBizId())
                .eq(Attachment::getDataType, attachment.getDataType())
                .orderByDesc(Attachment::getId);
        return baseMapper.page(page, wrapper);
    }

    /**
     * 根据业务类型和业务id查询附件
     *
     * @param bizTypes
     * @param bizIds
     * @return
     */
    @Override
    public List<AttachmentResultDTO> find(String[] bizTypes, String[] bizIds) {
        return baseMapper.find(bizTypes, bizIds);
    }
}
