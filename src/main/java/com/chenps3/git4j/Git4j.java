package com.chenps3.git4j;

import com.chenps3.git4j.modules.ConfigModule;
import com.chenps3.git4j.modules.FilesModule;

import java.util.HashMap;
import java.util.Map;

/**
 * 实现各种git命令
 *
 * @Author chenguanhong
 * @Date 2023/9/23
 */
public class Git4j {

    public static void init(Map<String, Object> opts) {
        if (FilesModule.inRepo()) {
            System.out.println("当前目录已经是个git仓库");
            return;
        }
        if (opts == null) {
            opts = new HashMap<>();
        }
        boolean bare = Boolean.parseBoolean((String) opts.get("bare"));

        Map<String, Object> git4jStructure = new HashMap<>();
        git4jStructure.put("HEAD", "ref: refs/heads/master\n");
        git4jStructure.put("config", ConfigModule.objToStr(Map.of("core", Map.of("", Map.of("bare", bare)))));
        git4jStructure.put("objects", new HashMap<>());
        git4jStructure.put("refs", Map.of("heads", new HashMap<>()));

        FilesModule.writeFilesFromTree(bare ? git4jStructure : Map.of(".gitlet", git4jStructure),
                FilesModule.cwd().toString());
    }

    public static void add(){
        FilesModule.assertInRepo();

    }
}
