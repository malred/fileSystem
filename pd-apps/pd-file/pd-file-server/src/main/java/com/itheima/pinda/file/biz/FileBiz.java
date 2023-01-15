package com.itheima.pinda.file.biz;

import cn.hutool.core.util.StrUtil;
import com.itheima.pinda.file.domain.FileDO;
import com.itheima.pinda.file.enumeration.DataType;
import com.itheima.pinda.file.utils.ZipUtils;
import com.itheima.pinda.utils.NumberHelper;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 提供文件下载的公共方法
 */
@Component
public class FileBiz {
    /**
     * 处理下载文件重名问题的类
     * <p>
     * 多个文件的原始文件名相同怎么办? 转换名称 -> a.doc a.doc => a.doc a(1).doc
     *
     * @param fileName 文件名
     * @param order    文件名拼接的数字
     * @return
     */
    private static String buildNewFileName(String fileName, Integer order) {
        return StrUtil.strBuilder(fileName)
                .insert(fileName.lastIndexOf("."), "(" + order + ")").toString();
    }

    /**
     * 文件下载方法
     *
     * @param fileDOList
     * @param request
     * @param response
     */
    public void down(List<FileDO> fileDOList, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 得到下载文件的总大小
        int fileSize = fileDOList.stream()
                // 过滤无效的文件
                .filter(
                        (file) -> file != null &&
                                !file.getDataType().eq(DataType.DIR) &&
                                StrUtil.isNotEmpty(file.getUrl())
                ).mapToInt(
                        // 过滤完成后,得到剩余文件的size,并由long转int,然后求和
                        (file) -> NumberHelper.intValueOf0(file.getSize())).sum();
        // 确认下载的文件名称 ( (多文件) -> xxx.doc等文件 || (单文件) -> xxx.doc )
        String downloadFileName
                // 得到第0个文件的原始文件名
                = fileDOList.get(0).getSubmittedFileName();
        // 多文件的情况要改变downloadFileName
        if (fileDOList.size() > 1) {
            // xxx.doc -> xxx等.zip
            downloadFileName =
                    StringUtils.substring(downloadFileName,
                            0, StringUtils.lastIndexOf(downloadFileName, "."))
                            + "等.zip";
        }
        // fileDOList -> Map<文件名,文件url>
        Map<String, String> fileMap = new LinkedHashMap<String, String>(fileDOList.size());
        // 同名下载文件名称的情况,Map<文件名,文件名重复次数>
        Map<String, Integer> duplicateFile = new HashMap<String, Integer>(fileDOList.size());
        // 保存文件到文件map
        fileDOList.stream()
                // 过滤无效文件
                .filter(
                        (file) -> file != null &&
                                !file.getDataType().eq(DataType.DIR) &&
                                StringUtils.isNotEmpty(file.getUrl()))
                .forEach(
                        (file) -> {
                            // 原始文件名
                            String submittedFileName = file.getSubmittedFileName();
                            // 如果文件map已经包含该文件名
                            if (fileMap.containsKey(submittedFileName)) {
                                // 如果重复文件名map已经包含该文件名,则重复次数+1
                                if (duplicateFile.containsKey(submittedFileName)) {
                                    duplicateFile.put(
                                            submittedFileName, duplicateFile.get(submittedFileName) + 1);
                                } else {
                                    // 第一次重复,则添加文件名到重复文件名map
                                    duplicateFile.put(submittedFileName, 1);
                                }
                                // 重复文件生成新文件名
                                submittedFileName = buildNewFileName(
                                        submittedFileName, duplicateFile.get(submittedFileName));
                            }
                            // 存入文件map
                            fileMap.put(submittedFileName, file.getUrl());
                        }
                );
        // 打包下载
        ZipUtils.zipFilesByInputStream(
                fileMap, Long.valueOf(fileSize), downloadFileName, request, response
        );
    }
}
