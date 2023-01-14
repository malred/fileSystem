import com.itheima.pinda.utils.DateUtils;
import org.junit.Test;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @author malred
 * 目录相关测试
 */
public class TestPath {
    /**
     * 测试生成年月相对路径
     */
    @Test
    public void testGenRelativePath() {
        // Paths.get会根据操作系统的不同而生成不同的路径 win->\ linux->/
        String dateStr = Paths.get(LocalDate.now()
                .format(DateTimeFormatter.ofPattern(DateUtils.DEFAULT_MONTH_FORMAT_SLASH))).toString();
        // 2023/01
        System.out.println(dateStr);
    }

    /**
     * 测试生成文件存放的绝对路径
     */
    @Test
    public void testGenAbsolutePath() {
        String absolutePath =
                Paths.get("D:\\uploadFile", "oss-file-service", "2023\\01").toString();
        // D:\\uploadFile\\oss-file-service\\2023\01
        System.out.println(absolutePath);
    }
}
