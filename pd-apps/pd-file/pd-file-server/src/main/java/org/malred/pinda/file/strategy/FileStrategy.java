package org.malred.pinda.file.strategy;

import com.itheima.pinda.file.domain.FileDeleteDO;
import com.itheima.pinda.file.entity.File;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 最高层策略处理接口
 */
public interface FileStrategy {
    /**
     * 上传文件
     *
     * @param file
     * @return
     */
    public File upload(MultipartFile file);

    /**
     * 删除文件
     *
     * @param list 支持删除多个
     * @return
     */
    public boolean delete(List<FileDeleteDO> list);
}
