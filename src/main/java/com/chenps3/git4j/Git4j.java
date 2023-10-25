package com.chenps3.git4j;

import com.chenps3.git4j.modules.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 实现各种git命令
 *
 * @Author chenguanhong
 * @Date 2023/9/23
 */
public class Git4j {

    /**
     * 初始化一个新仓库
     */
    public static void init(Map<String, Object> opts) {
        if (FilesModule.inRepo()) {
            System.out.println("当前目录已经是个git仓库");
            return;
        }
        if (opts == null) {
            opts = new HashMap<>();
        }
        //是否纯仓库
        boolean bare = Boolean.parseBoolean((String) opts.get("bare"));

        Map<String, Object> git4jStructure = new HashMap<>();
        git4jStructure.put("HEAD", "ref: refs/heads/master\n");
        git4jStructure.put("config", ConfigModule.objToStr(Map.of("core", Map.of("", Map.of("bare", bare)))));
        git4jStructure.put("objects", new HashMap<>());
        git4jStructure.put("refs", Map.of("heads", new HashMap<>()));

        FilesModule.writeFilesFromTree(bare ? git4jStructure : Map.of(".gitlet", git4jStructure),
                FilesModule.cwd().toString());
    }

    /**
     * 把path的文件写入索引
     */
    public static void add(String pathStr) {
        FilesModule.assertInRepo();
        ConfigModule.assertNotBare();
        //path包含的所有文件
        List<Path> addedFiles = FilesModule.lsRecursive(Path.of(pathStr));
        if (addedFiles.size() == 0) {
            throw new RuntimeException(FilesModule.pathFromRepoRoot(pathStr) + "没有匹配到文件");
        }
        Map<String, String> opts = new HashMap<>();
        opts.put("add", "true");
        for (Path p : addedFiles) {
            updateIndex(p.toString(), opts);
        }
    }

    /**
     * 把path文件里的内容添加到index或者从index里删除
     */
    public static String updateIndex(String path, Map<String, String> opts) {
        FilesModule.assertInRepo();
        ConfigModule.assertNotBare();
        if (opts == null) {
            opts = new HashMap<>();
        }
        boolean remove = Objects.equals(opts.get("remove"), "true");
        boolean add = Objects.equals(opts.get("add"), "true");

        Path pathFromRoot = FilesModule.pathFromRepoRoot(path);
        boolean isOnDisk = Files.exists(Path.of(path));
        boolean isInIndex = IndexModule.hasFile(path, 0);
        //updateIndex只处理单个文件
        if (isOnDisk && Files.isDirectory(Path.of(path))) {
            throw new RuntimeException(pathFromRoot + "是个目录 - 在内部添加文件");
        }
        //删除文件
        else if (remove && !isOnDisk && isInIndex) {
            //不支持删除冲突文件的索引
            if (IndexModule.isFileInConflict(path)) {
                throw new RuntimeException("不支持删除冲突文件的索引");
            }
            //文件已删除但文件索引还在，需要进行索引删除
            else {
                IndexModule.writeRm(path);
                return "\n";
            }
        }
        //索引里也删除了，就不需要做什么
        else if (remove && !isOnDisk && !isInIndex) {
            return "\n";
        }
        //文件存在时，添加索引必须有--add参数，
        else if (!add && isOnDisk && !isInIndex) {
            throw new RuntimeException("无法把" + pathFromRoot + "添加到索引，请使用 --add 选项");
        }
        //文件存在时，索引存在or使用了add参数，则把文件写入索引
        else if (isOnDisk && (add || isInIndex)) {
            String content = FilesModule.read(FilesModule.workingCopyPath(path));
            IndexModule.writeNonConflict(path, content);
            return "\n";
        }
        //无--remove参数且文件不存在时报错
        else if (!remove && !isOnDisk) {
            throw new RuntimeException(pathFromRoot + "不存在，且无 --remove 选项");
        }
        return "\n";
    }

    /**
     * 把path的文件移除索引
     */
    public void rm(String path, Map<String, Object> opts) {
        FilesModule.assertInRepo();
        ConfigModule.assertNotBare();
        if (opts == null) {
            opts = new HashMap<>();
        }
        var filesToRm = IndexModule.matchingFiles(path);
        boolean f = Objects.equals(opts.get("f"), "true");
        boolean r = Objects.equals(opts.get("r"), "true");
        //rm -f 强制移除，暂不支持把有变更的文件移除
        if (f) {
            throw new RuntimeException("unsupported");
        }
        //路径无文件匹配
        else if (filesToRm.size() == 0) {
            throw new RuntimeException(FilesModule.pathFromRepoRoot(path) + " did not match any files");
        }
        //目录需要使用递归删除 即rm -r
        else if (Files.exists(Path.of(path)) && Files.isDirectory(Path.of(path)) && !r) {
            throw new RuntimeException("not removing " + path + " recursively without -r");
        } else {
            var addedOrModifiedFiles = DiffModule.addedOrModifiedFiles();
            var changeToRm = UtilModule.intersection(addedOrModifiedFiles, filesToRm);
            //已变更的文件，不可删除
            if (changeToRm.size() > 0) {
                String files = String.join("\n", changeToRm);
                throw new RuntimeException("these files have changes:\n" + files + "\n");
            }
            //从磁盘删除
            filesToRm.stream()
                    .map(FilesModule::workingCopyPath)     //转为绝对路径
                    .filter(Files::exists)
                    .forEach(i -> {
                        try {
                            Files.delete(i);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            //从index删除
            var opt = new HashMap<String, String>();
            opt.put("remove", "true");
            filesToRm.forEach(p -> {
                updateIndex(p, opt);
            });
        }
    }
}
