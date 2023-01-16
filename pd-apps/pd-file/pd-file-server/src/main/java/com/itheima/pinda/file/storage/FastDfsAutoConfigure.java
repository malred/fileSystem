package com.itheima.pinda.file.storage;

import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.AppendFileStorageClient;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.itheima.pinda.base.R;
import com.itheima.pinda.file.domain.FileDeleteDO;
import com.itheima.pinda.file.dto.chunk.FileChunksMergeDTO;
import com.itheima.pinda.file.entity.File;
import com.itheima.pinda.file.properties.FileServerProperties;
import com.itheima.pinda.file.strategy.impl.AbstractFileChunkStrategy;
import com.itheima.pinda.file.strategy.impl.AbstractFileStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * FASTDFS(分布式文件系统)配置类
 * <p>
 * EnableConfigurationProperties注解 -> 加载配置文件里的属性
 * ConditionalOnProperty -> 当配置文件的 pinda.file.type 为 FAST_DFS 时生效
 *
 * @author malred
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(FileServerProperties.class)
@ConditionalOnProperty(name = "pinda.file.type", havingValue = "FAST_DFS")
public class FastDfsAutoConfigure {
    /**
     * FAST_DFS文件策略处理类
     */
    @Service // 委托spring创建
    public class FastDfsServiceImpl extends AbstractFileStrategy {
        /**
         * 注入操作fastdfs的客户端对象
         */
        @Autowired
        private FastFileStorageClient storageClient;

        /**
         * 上传文件
         *
         * @param file
         * @param multipartFile
         * @return
         * @throws Exception
         */
        @Override
        public File uploadFile(File file, MultipartFile multipartFile) throws Exception {
            // 调用FastDfs客户端提供的api,上传文件(返回值是文件url路径)
            StorePath storePath = storageClient.uploadFile(
                    multipartFile.getInputStream(),
                    multipartFile.getSize(),
                    file.getExt(),
                    null
            );
            // 文件上传完成,将相关信息保存到file对象里,之后把file对象的信息存到数据库
            file.setUrl(fileServerProperties.getUriPrefix() + storePath.getFullPath()); // 文件url
            file.setGroup(storePath.getGroup()); // 文件分组(fastdfs自动分组)
            file.setPath(storePath.getPath()); // 文件路径
            return file;
        }

        /**
         * 删除文件
         *
         * @param fileDeleteDO
         */
        @Override
        public void delete(FileDeleteDO fileDeleteDO) {
            // 文件删除
            storageClient.deleteFile(fileDeleteDO.getGroup(), fileDeleteDO.getPath());
        }
    }

    /**
     * FAST_DFS文件分片策略处理类
     */
    @Service
    public class FastDfsChunkServiceImpl extends AbstractFileChunkStrategy {
        /**
         * fastdfs提供的分片处理的client
         */
        @Autowired
        private AppendFileStorageClient storageClient;

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
            StorePath storePath = null;
            for (int i = 0; i < files.size(); i++) {
                java.io.File chunkFile = files.get(i);
                if (i == 0) {
                    // 第一片分片
                    storePath = storageClient.uploadAppenderFile(null,
                            FileUtils.openInputStream(chunkFile),
                            chunkFile.length(),
                            info.getExt());
                } else {
                    // 后续分片追加到第一片分片
                    storageClient.appendFile(
                            storePath.getGroup(),
                            storePath.getPath(),
                            FileUtils.openInputStream(chunkFile),
                            chunkFile.length()
                    );
                }
            }
            // storePath为空
            if (storePath == null) {
                log.error("分片合并失败");
                return R.fail("分片合并失败");
            }
            // 合并成功后,封装file对象返回
            String url = new StringBuilder(fileProperties.getUriPrefix())
                    .append(storePath.getFullPath())
                    .toString();
            File filePo = File.builder()
                    .url(url)
                    .group(storePath.getGroup())
                    .path(storePath.getPath())
                    .build();
            return R.success(filePo);
        }
    }
}
