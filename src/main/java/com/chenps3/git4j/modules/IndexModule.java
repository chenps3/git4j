package com.chenps3.git4j.modules;

import com.chenps3.git4j.domain.KeyPieces;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * index保存文件路径到文件内容hash的映射
 * 当创建一个commit，就会建立一个tree，反映对应index的内容
 * index的key的形式为 【path,stage】，文件不冲突的情况下，stage总是0
 *
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
     * key: 文件路径+stage
     * value: 文件内容的哈希
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
     * 返回索引里匹配pathSpec的所有路径
     */
    public static List<String> matchingFiles(String pathSpec) {
        var searchPath = FilesModule.pathFromRepoRoot(pathSpec);
        var toc = toc();
        var pattern = Pattern.compile("^" + searchPath.toString().replace("\\", "\\\\"));
        return toc.keySet().stream().filter(k -> {
            Matcher matcher = pattern.matcher(k);
            return matcher.matches();
        }).collect(Collectors.toList());
    }

    /**
     * 返回一个map，key是文件路径，value是文件hash
     * 和read方法类似
     * 但返回的map只使用文件路径作为key（read()里key包含了stage）
     */
    public static Map<String, String> toc() {
        var idx = read();
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> e : idx.entrySet()) {
            String[] strs = e.getKey().split(",");
            String key = strs[0];
            result.put(key, e.getValue());
        }
        return result;
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

    /**
     * 返回一个map，key是working copy的文件路径
     * value是文件内容的hash
     */
    public static Map<String, String> workingCopyToc() {
        Map<String, String> idx = read();
        Map<String, String> result = new HashMap<>();
        for (String key : idx.keySet()) {
            String p = key.split(",")[0];
            if (!Files.exists(FilesModule.workingCopyPath(p))) {
                continue;
            }
            String content = FilesModule.read(FilesModule.workingCopyPath(p));
            String hash = UtilModule.hash(content);
            result.put(p, hash);
        }
        return result;
    }

    /**
     * 处于冲突状态的文件列表
     */
    public static List<String> conflictPaths() {
        var idx = read();
        return idx.keySet().stream()
                .map(IndexModule::keyPieces)
                .filter(i -> i.getStage() == 2)
                .map(KeyPieces::getPath).collect(Collectors.toList());
    }

    /**
     * 把入参key转为KeyPieces对象
     */
    public static KeyPieces keyPieces(String key) {
        String[] pieces = key.split(",");
        return new KeyPieces(pieces[0], Integer.parseInt(pieces[1]));
    }

    /**
     * 入参为map，key是文件路径，value是文件内容的hash
     * 返回几乎相同的map，但是key是文件路径 + stage 0
     * 可以认为是toc的反函数
     * 例如: `{ “file1,0”: hash(1), “src/file2,0”: hash(2) }’
     */
    public static Map<String, String> tocToIndex(Map<String, String> toc) {
        Map<String, String> result = new HashMap<>();
        for (var e : toc.entrySet()) {
            result.put(key(e.getKey(), "0"), e.getValue());
        }
        return result;
    }
}
