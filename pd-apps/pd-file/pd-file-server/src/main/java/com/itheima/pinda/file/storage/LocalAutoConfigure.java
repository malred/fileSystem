package com.itheima.pinda.file.storage;

import cn.hutool.core.util.StrUtil;
import com.itheima.pinda.base.R;
import com.itheima.pinda.file.domain.FileDeleteDO;
import com.itheima.pinda.file.dto.chunk.FileChunksMergeDTO;
import com.itheima.pinda.file.entity.File;
import com.itheima.pinda.file.properties.FileServerProperties;
import com.itheima.pinda.file.strategy.impl.AbstractFileChunkStrategy;
import com.itheima.pinda.file.strategy.impl.AbstractFileStrategy;
import com.itheima.pinda.utils.DateUtils;
import com.itheima.pinda.utils.StrPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 本地上传配置类
 * <p>
 * EnableConfigurationProperties注解 -> 加载配置文件里的属性
 * ConditionalOnProperty -> 当配置文件的 pinda.file.type 为 LOCAL 时生效
 *
 * @author malred
 */
@Configuration
@Slf4j
@EnableConfigurationProperties(FileServerProperties.class)
@ConditionalOnProperty(name = "pinda.file.type", havingValue = "LOCAL")
public class LocalAutoConfigure {
    /**
     * 本地文件策略处理类
     * 当LocalAutoConfigure配置类生效时创建
     * Service注解 -> 交给spring容器创建并管理
     */
    @Service
    public class LocalServiceImpl extends AbstractFileStrategy {
        /**
         * 获取local配置文件对象
         */
        private void buildClient() {
            /**
             * 该属性定义在 AbstractFileStrategy
             * @see AbstractFileStrategy
             */
            properties = fileServerProperties.getLocal();
        }

        /**
         * 文件上传
         *
         * @param file
         * @param multipartFile
         * @return
         */
        @Override
        public File uploadFile(File file, MultipartFile multipartFile) throws Exception {
            buildClient();
            String endpoint = properties.getEndpoint();
            String bucketName = properties.getBucketName();
            String uriPrefix = properties.getUriPrefix();
            // 使用UUID为文件生成新文件名
            String fileName = UUID.randomUUID().toString() + StrPool.DOT + file.getExt();
            // D:\\uploadFile\\存储策略\\2023\\01\\xxx.doc
            // 根据年月分目录存储
            // Paths.get会根据操作系统的不同而选择不同分隔符 win->\ linux->/
            String relativePath = Paths.get(LocalDate.now()
                    .format(DateTimeFormatter.ofPattern(DateUtils.DEFAULT_MONTH_FORMAT_SLASH))).toString();
            // 文件存放的绝对路径
            String absolutePath = Paths.get(endpoint, bucketName, relativePath).toString();
            log.debug(Paths.get(absolutePath, fileName).toString());
            // 目标输出文件
            java.io.File outFile = new java.io.File(Paths.get(absolutePath, fileName).toString());
            // 写入数据
            FileUtils.writeByteArrayToFile(outFile, multipartFile.getBytes());
            // 封装上传文件的信息,要保存到数据库(url,filename,relativePath)
            String url = new StringBuilder(getUriPrefix())
                    .append(StrPool.SLASH)
                    .append(properties.getBucketName())
                    .append(StrPool.SLASH)
                    .append(relativePath)
                    .append(StrPool.SLASH)
                    .append(fileName)
                    .toString();
            // 替换掉windows环境的\路径
            url = StrUtil.replace(url, "\\\\", StrPool.SLASH);
            url = StrUtil.replace(url, "\\", StrPool.SLASH);
            file.setUrl(url); // http://ip:port/存储策略/2023/01/xxx.doc
            file.setFilename(fileName);
            file.setRelativePath(relativePath);
            return file;
        }

        /**
         * 文件删除
         *
         * @param fileDeleteDO
         */
        @Override
        public void delete(FileDeleteDO fileDeleteDO) {
            buildClient();
            log.debug(properties.toString());
            // 拼接要删除的文件的绝对磁盘路径
            String filePath = Paths.get(
                    properties.getEndpoint(),
                    properties.getBucketName(),
                    fileDeleteDO.getRelativePath(),
                    fileDeleteDO.getFileName()
            ).toString();
            log.debug("filePath->{}", filePath);
            // 封装file对象
            java.io.File file = new java.io.File(filePath);
            // 删除文件
            FileUtils.deleteQuietly(file);
        }

    }

    /**
     * 本地分片文件处理策略类
     */
    @Service
    public class LocalChunkServiceImpl extends AbstractFileChunkStrategy {
        /**
         * 分片合并
         *
         * @param files    分片文件
         * @param fileName 唯一名 含后缀
         * @param info     分片信息
         * @return
         * @throws IOException
         */
        @Override
        protected R<File> merge(List<java.io.File> files, String fileName, FileChunksMergeDTO info) throws IOException {
            properties = fileProperties.getLocal();
            //日期目录
            String relativePath = Paths.get(LocalDate.now().format(DateTimeFormatter.ofPattern(DateUtils.DEFAULT_MONTH_FORMAT_SLASH))).toString();
            //合并后文件的存储路径 例如：D:\\uploadFiles\\oss-file-service\\2020\\05
            String path = Paths.get(properties.getEndpoint(), properties.getBucketName(), relativePath).toString();
            //上传文件存放目录，如果不存在则创建
            java.io.File uploadFolder = new java.io.File(path);
            if (!uploadFolder.exists()) {
                uploadFolder.mkdirs();
            }
            //创建合并后的文件
            java.io.File outputFile = new java.io.File(Paths.get(path, fileName).toString());
            if (!outputFile.exists()) {
                boolean newFile = outputFile.createNewFile();
                if (!newFile) {
                    return R.fail("创建文件失败");
                }
                try (FileChannel outChannel = new FileOutputStream(outputFile).getChannel()) {
                    //同步nio 方式对分片进行合并, 有效的避免文件过大导致内存溢出
                    for (java.io.File file : files) {
                        try (FileChannel inChannel = new FileInputStream(file).getChannel()) {
                            inChannel.transferTo(0, inChannel.size(), outChannel);
                        } catch (FileNotFoundException ex) {
                            log.error("文件转换失败", ex);
                            return R.fail("文件转换失败");
                        }
                        //删除分片
                        if (!file.delete()) {
                            log.error("分片[" + info.getName() + "=>" + file.getName() + "]删除失败");
                        }
                    }
                } catch (FileNotFoundException e) {
                    log.error("文件输出失败", e);
                    return R.fail("文件输出失败");
                }
            } else {
                log.warn("文件[{}], fileName={}已经存在", info.getName(), fileName);
            }
            String url = new StringBuilder(properties.getUriPrefix()).append(StrPool.SLASH).
                    append(properties.getBucketName()).append(StrPool.SLASH).
                    append(relativePath).append(StrPool.SLASH).
                    append(fileName).toString();
            File filePo = File.builder()
                    .relativePath(relativePath)
                    .url(StringUtils.replace(url, "\\", StrPool.SLASH))
                    .build();
            return R.success(filePo);
        }
    }
}
