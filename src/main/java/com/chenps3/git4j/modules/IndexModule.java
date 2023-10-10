package com.chenps3.git4j.modules;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author chenguanhong
 * @Date 2023/10/10
 */
public class IndexModule {

    /**
     * index是否存在path文件的索引，且stage状态为stage
     */
    public static boolean hasFile(String path, int stage) {
        Map<String, String> indexMap = read();
        String key = key(path, String.valueOf(stage));
        return indexMap.containsKey(key);
    }

    /**
     * 读取index并以map形式返回
     */
    public static Map<String, String> read() {
        var indexFilePath = FilesModule.gitletPath("index");
        String indexStr = "\n";
        if (indexFilePath != null && Files.exists(indexFilePath)) {
            String indexFileContent = FilesModule.read(indexFilePath);
            if (indexFileContent != null) {
                indexStr = indexFileContent;
            }
        }
        List<String> lines = UtilModule.lines(indexStr);
        Map<String, String> indexMap = new HashMap<>();
        for (String blobStr : lines) {
            var blobData = blobStr.split(" ");
            String key = key(blobData[0], blobData[1]);
            indexMap.put(key, blobData[2]);
        }
        return indexMap;
    }

    /**
     * path+stage 确定的index的key
     */
    public static String key(String path, String stage) {
        return path + "," + stage;
    }

    /**
     * 文件是否存在冲突
     */
    public static boolean isFileInConflict(String path) {
        return hasFile(path, 2);
    }

    /**
     * 删除索引里path文件的entry，即使path文件可能存在冲突
     * 参考writeConflict
     */
    public static void writeRm(String path) {
        Map<String, String> indexMap = read();
        for (int stage = 0; stage <= 3; stage++) {
            indexMap.remove(key(path, String.valueOf(stage)));
        }
        write(indexMap);
    }

    /**
     * indexMap表示的索引内容写入到.gitlet/index文件
     */
    public static void write(Map<String, String> indexMap) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> index : indexMap.entrySet()) {
            String[] tmp = index.getKey().split(",");
            sb.append(tmp[0]).append(" ")
                    .append(tmp[1]).append(" ")
                    .append(index.getValue()).append("\n");
        }
        FilesModule.write(FilesModule.gitletPath("index"), sb.toString());
    }

    public static void writeNonConflict(String path, String content) {
        writeRm(path);
        _writeStageEntry(path, 0, content);
    }

    /**
     * 把content的hash写入索引
     */
    private static void _writeStageEntry(String path, int stage, String content) {
        var idx = read();
        var key = key(path, String.valueOf(stage));
        var value = ObjectsModule.write(content);
        idx.put(key, value);
        write(idx);
    }
}
