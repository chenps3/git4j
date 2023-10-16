package com.chenps3.git4j.modules;

import java.nio.file.Path;
import java.util.Objects;

/**
 * @Author chenguanhong
 * @Date 2023/10/16
 */
public class RefsModule {

    /**
     * 返回refOrHash指向的hash
     */
    public static String hash(String refOrHash) {
        if (ObjectsModule.exists(refOrHash)) {
            return refOrHash;
        }
        return "";
    }

    /**
     * 解析ref返回最具体的ref
     */
    public static void terminalRef(String ref) {
        if (Objects.equals(ref, "HEAD")) {

        }
    }

    /**
     * 如果HEAD指向一个commit而不是另一个分支的引用，返回true
     */
    public static boolean isHeadDetached() {
        Path p = FilesModule.gitletPath("HEAD");
        String str = FilesModule.read(p);
        return str != null && !str.contains("refs");
    }
}
