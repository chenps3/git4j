package com.chenps3.git4j.modules;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @Author chenguanhong
 * @Date 2023/10/10
 */
public class ObjectsModule {

    /**
     * 把content写入objects数据库
     * 返回hash
     */
    public static String write(String content) {
        String hash = UtilModule.hash(content);
        Path gitletPath = FilesModule.gitletPath(null);
        if (gitletPath == null) {
            throw new RuntimeException("当前目录不是git仓库");
        }
        Path path = gitletPath.resolve("objects").resolve(hash);
        FilesModule.write(path, content);
        return hash;
    }

    public static boolean exists(String objectHash) {
        if (objectHash == null) {
            return false;
        }
        Path gitletPath = FilesModule.gitletPath(null);
        if (gitletPath == null) {
            throw new RuntimeException("not in repo");
        }
        Path p = gitletPath.resolve("objects").resolve(objectHash);
        return Files.exists(p);
    }
}
