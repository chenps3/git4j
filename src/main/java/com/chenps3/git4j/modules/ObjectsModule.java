package com.chenps3.git4j.modules;

import com.chenps3.git4j.Asserts;
import com.chenps3.git4j.domain.ObjectType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * objects 是位于目录 .gitlet/objects/ 下的文件，有3种类型：
 * 1. blob对象，储存文件的内容。例如一个叫number.txt的文件，内容为first。
 * 当文件加入到index，就会在这个目录创建一个blob文件，文件名为hash(first),内容为first
 * 2. tree对象储存repo中某个目录下的文件列表和目录列表。
 * 文件列表的条目指向blob对象，目录列表的条目指向其他tree对象。
 * 3. commit对象储存指向一个tree对象和一个messsage的指针。
 * 表示一个commit后repo的状态。
 *
 * @Author chenguanhong
 * @Date 2023/10/10
 */
public class ObjectsModule {

    /**
     * 储存表示 index当前内容 的tree对象的图
     */
    @SuppressWarnings("unchecked")
    public static String writeTree(Map<String, ?> tree) {
        var treeObject = tree.entrySet().stream().map(e -> {
            if (e.getValue() instanceof String) {
                return "blob " + e.getValue() + " " + e.getKey();
            } else {
                return "tree " + writeTree((Map<String, ?>) e.getValue()) + " " + e.getKey();
            }
        }).collect(Collectors.joining("\n")) + "\n";
        return ObjectsModule.write(treeObject);
    }

    /**
     * 把content写入objects数据库
     * 返回hash
     */
    public static String write(String content) {
        String hash = UtilModule.hash(content);
        Path gitletPath = FilesModule.gitletPath(null);
        Asserts.assertTrue(gitletPath != null, "当前目录不是git仓库");
        Path path = gitletPath.resolve("objects").resolve(hash);
        FilesModule.write(path, content);
        return hash;
    }

    /**
     * 如果objectHash对象在db里存在，返回true
     */
    public static boolean exists(String objectHash) {
        if (objectHash == null) {
            return false;
        }
        Path gitletPath = FilesModule.gitletPath(null);
        Asserts.assertTrue(gitletPath != null, "ot in repo");
        Path p = gitletPath.resolve("objects").resolve(objectHash);
        return Files.exists(p);
    }

    /**
     * 输入一个commit的hash，读取这个commit在tree里存储的内容
     * 把tree转换成一个目录，映射文件名和文件内容的hash
     * 例如{ "file1": hash(1), "a/file2": "hash(2)" }
     */
    public static Map<String, String> commitToc(String hash) {
        var content = read(hash);
        var tree = treeHash(content);
        var fileTree = fileTree(tree, null);
        return FilesModule.flattenNestedTree(fileTree, null, null);
    }

    /**
     * 返回objectHash对象的内容
     */
    public static String read(String objectHash) {
        if (objectHash != null) {
            var gitletPath = FilesModule.gitletPath(null);
            if (gitletPath == null) {
                throw new RuntimeException("not in git repo");
            }
            var objectPath = gitletPath.resolve("objects").resolve(objectHash);
            if (Files.exists(objectPath)) {
                return FilesModule.read(objectPath);
            }
        }
        return null;
    }

    /**
     * 把str解析为commit，返回其指向的tree
     * 如果str不是commit，返回null
     */
    public static String treeHash(String str) {
        if (type(str) == ObjectType.COMMIT) {
            return str.split("\\s")[1];
        }
        return null;
    }

    /**
     * 把str解析为一个object，并返回这个object的类型
     * object有3种类型commit tree blob
     */
    public static ObjectType type(String str) {
        var strArr = str.split(" ");
        var typeStr = strArr[0];
        if ("commit".equals(typeStr)) {
            return ObjectType.COMMIT;
        }
        if ("tree".equals(typeStr)) {
            return ObjectType.TREE;
        }
        if ("blob".equals(typeStr)) {
            return ObjectType.TREE;
        }
        return ObjectType.BLOB;
    }

    /**
     * 根据treeHash返回对应的tree对象
     * 把连接的tree对象以map的形式返回，如
     * {"file1":"hash(1)","src":{"file2":"hash(2)"}}
     */
    public static Map<String, Object> fileTree(String treeHash, Map<String, Object> tree) {
        if (tree == null) {
            return fileTree(treeHash, new HashMap<>());
        }
        var content = read(treeHash);
        UtilModule.lines(content).forEach(line -> {
            var lineTokens = line.split(" ");
            if (Objects.equals(lineTokens[0], "tree")) {
                var tmp = fileTree(lineTokens[1], new HashMap<>());
                tree.put(lineTokens[2], tmp);
            } else {
                tree.put(lineTokens[2], lineTokens[1]);
            }
        });
        return tree;
    }

    /**
     * 创建commit对象，写入到objects 数据库
     */
    public static String writeCommit(String treeHash, String message, List<String> parentHashes) {
        String parentStr = parentHashes.stream().map(i -> "parent " + i + "\n").collect(Collectors.joining(""));
        String sb = "commit " + treeHash + "\n" +
                parentStr +
                "Date: " + LocalDateTime.now() + "\n\n" +
                "    " + message + "\n";
        return ObjectsModule.write(sb);
    }

    /**
     * 返回objects数据库所有的对象的内容
     */
    public static List<String> allObjects() {
        Path p = FilesModule.gitletPath("objects");
        Asserts.assertTrue(p != null, "not in repo");
        try (var stream = Files.list(p)) {
            return stream.map(i -> ObjectsModule.read(i.toString()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 判断descendentHash是否是ancestorHash的后代
     */
    public static boolean isAncestor(String descendentHash, String ancestorHash) {
        var ancestors = ancestors(descendentHash);
        return ancestors.contains(ancestorHash);
    }

    /**
     * 返回commitHash的所有祖先commit hash
     */
    public static List<String> ancestors(String commitHash) {
        var commitContent = read(commitHash);
        var parents = parentHashes(commitContent);
        var parentsAncestors = parents.stream().map(ObjectsModule::ancestors).flatMap(Collection::stream).toList();
        List<String> result = new ArrayList<>(parents);
        result.addAll(parentsAncestors);
        return result;
    }

    /**
     * 把str解析为一个commit，然后返回其父commit
     */
    public static List<String> parentHashes(String str) {
        if (type(str) == ObjectType.COMMIT) {
            return Arrays.stream(str.split("\n"))
                    .filter(line -> {
                        Matcher m1 = p1.matcher(line);
                        return m1.find();
                    })
                    .map(line -> line.split(" ")[1])
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static final Pattern p1 = Pattern.compile("^parent");
}
