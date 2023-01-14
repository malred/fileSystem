package com.itheima.pinda.file.strategy.impl;

import com.itheima.pinda.exception.BizException;
import com.itheima.pinda.exception.code.ExceptionCode;
import com.itheima.pinda.file.domain.FileDeleteDO;
import com.itheima.pinda.file.entity.File;
import com.itheima.pinda.file.enumeration.IconType;
import com.itheima.pinda.file.utils.FileDataTypeUtil;
import com.itheima.pinda.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import com.itheima.pinda.file.properties.FileServerProperties;
import com.itheima.pinda.file.strategy.FileStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 抽象文件处理类
 */
@Slf4j
public abstract class AbstractFileStrategy implements FileStrategy {
    /**
     * 文件服务配置信息 pd-file-server.yml
     */
    @Autowired
    protected FileServerProperties fileServerProperties;
    /**
     * 声明具体使用的策略(只声明,由子类设置)
     */
    protected FileServerProperties.Properties properties;
    /**
     * 用于判断文件是否包含后缀
     */
    private static final String FILE_SPLIT = ".";

    /**
     * 文件上传
     *
     * @param multipartFile
     * @return
     */
    @Override
    public File upload(MultipartFile multipartFile) {
        try {
            // 获取原始文件名
            String originalFilename = multipartFile.getOriginalFilename();
            // 如果没有后缀名(不包含 . ),则是非法文件,直接抛异常
            if (!originalFilename.contains(FILE_SPLIT)) {
                throw BizException.wrap(
                        ExceptionCode.BASE_VALID_PARAM.build("上传文件缺失后缀"));
            }
            // 封装一个File对象(仅封装可以确定的基本信息)
            // 在完成文件上传后,需要将上传的文件的信息保存到本地数据库
            File file = File.builder()
                    // 文件是否被删除
                    .isDelete(false)
                    // 文件大小
                    .size(multipartFile.getSize())
                    // 文件类型
                    .contextType(multipartFile.getContentType())
                    // 数据类型
                    .dataType(FileDataTypeUtil.getDataType(multipartFile.getContentType()))
                    // 原始文件名称
                    .submittedFileName(multipartFile.getOriginalFilename())
                    // 获取文件后缀
                    .ext(FilenameUtils.getExtension(multipartFile.getOriginalFilename()))
                    .build();
            // 设置文件的图标(用于云盘展示)根据后缀名判断
            file.setIcon(IconType.getIcon(file.getExt()).getIcon());
            // 设置文件创建时间
            LocalDateTime now = LocalDateTime.now();
            file.setCreateMonth(DateUtils.formatAsYearMonthEn(now));
            file.setCreateWeek(DateUtils.formatAsYearWeekEn(now));
            file.setCreateDay(DateUtils.formatAsDateEn(now));
            // 封装File对象其他相关属性
            uploadFile(file, multipartFile);
            return file;
        } catch (Exception e) {
            // 记录异常日志
            log.error("e = {}", e);
            // 抛异常,让全局异常处理可以访问到
            throw BizException.wrap(
                    ExceptionCode.BASE_VALID_PARAM.build("文件上传失败"));
        }
    }

    /**
     * ps: 因为有不同策略,所以具体的上传和封装交给子类实现
     * 文件上传实际的业务代码
     *
     * @param file
     * @param multipartFile
     * @return
     */
    public abstract File uploadFile(File file, MultipartFile multipartFile) throws Exception;

    /**
     * 文件删除
     *
     * @param list 支持删除多个
     * @return
     */
    @Override
    public boolean delete(List<FileDeleteDO> list) {
        // 非空判断
        if (list == null || list.isEmpty()) {
            // 没必要抛异常,可以直接返回true,略过
            return true;
        }
        // 删除是否成功的标识
        boolean flag = false;
        for (FileDeleteDO fileDeleteDO : list) {
            try {
                delete(fileDeleteDO);
                flag = true;
            } catch (Exception e) {
                // 打印异常日志
                log.error("e = {}", e);
            }
        }
        return flag;
    }

    /**
     * 具体的文件删除方法,由子类实现
     *
     * @param fileDeleteDO
     */
    public abstract void delete(FileDeleteDO fileDeleteDO);

    /**
     * 获取下载地址前缀
     */
    protected String getUriPrefix() {
        // 如果配置文件里有prefix配置,就使用
        if (StringUtils.isNotEmpty(properties.getUriPrefix())) {
            return properties.getUriPrefix();
        } else {
            // endpoint D:\\uploadFile
            return properties.getEndpoint();
        }
    }
}
