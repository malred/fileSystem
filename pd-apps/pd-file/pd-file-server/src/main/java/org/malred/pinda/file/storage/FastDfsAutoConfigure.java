package org.malred.pinda.file.storage;

import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.itheima.pinda.file.domain.FileDeleteDO;
import com.itheima.pinda.file.entity.File;
import lombok.extern.slf4j.Slf4j;
import org.malred.pinda.file.properties.FileServerProperties;
import org.malred.pinda.file.strategy.impl.AbstractFileStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
}
