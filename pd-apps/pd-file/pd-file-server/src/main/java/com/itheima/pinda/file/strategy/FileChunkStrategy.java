package com.itheima.pinda.file.strategy;

import com.itheima.pinda.base.R;
import com.itheima.pinda.file.dto.chunk.FileChunksMergeDTO;
import com.itheima.pinda.file.entity.File;

/**
 * 分片文件处理策略最高层接口
 */
public interface FileChunkStrategy {
    /**
     * 分片合并
     *
     * @param merge
     * @return
     */
    R<File> chunksMerge(FileChunksMergeDTO merge);
}
