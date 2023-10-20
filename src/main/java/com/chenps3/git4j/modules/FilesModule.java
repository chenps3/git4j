package com.chenps3.git4j.modules;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @Author chenguanhong
 * @Date 2023/9/19
 */
public class FilesModule {

    /**
     * 入参tree的每个key表示一个子目录，这个方法将其扁平化
     * 如{"a":{"b":"me"}} => {"a/b":"me"}
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> flattenNestedTree(Map<String, Object> tree, Map<String, Object> obj, String prefix) {
        if (obj == null) {
            return flattenNestedTree(tree, new HashMap<>(), "");
        }
        for (Map.Entry<String, Object> entry : tree.entrySet()) {
            var dir = entry.getKey();
            var path = Path.of(prefix, dir);
            if (entry.getValue() instanceof String) {
                obj.put(path.toString(), entry.getValue());
            } else {
                flattenNestedTree((Map<String, Object>) entry.getValue(), obj, path.toString());
            }
        }
        return obj;
    }

    public static void nestFlatTree(Map<String, String> pathContent) {
        for (Map.Entry<String, String> e : pathContent.entrySet()) {
            String wholePath = e.getKey();
            String content = e.getValue();

        }
    }

    /**
     * 递归返回path下的所有文件
     */
    public static List<Path> lsRecursive(Path path) {
        if (!Files.exists(path)) {
            return Collections.emptyList();
        }
        if (Files.isRegularFile(path)) {
            return Collections.singletonList(path);
        }
        if (Files.isDirectory(path)) {
            File dir = path.toFile();
            File[] files = dir.listFiles();
            List<Path> children = new LinkedList<>();
            if (files != null) {
                for (File f : files) {
                    children.addAll(lsRecursive(f.toPath()));
                }
            }
            return children;
        }
        return Collections.emptyList();
    }

    /**
     * 递归删除path内的空目录
     */
    public static void removeEmptyDirs(Path path) {
        if (!Files.isDirectory(path)) {
            return;
        }
        File dir = path.toFile();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                removeEmptyDirs(path.resolve(f.getPath()));
            }
        }
        dir.delete();         //del必须为空才能删除
    }

    /**
     * 把content写入到path，会覆盖原有内容
     */
    public static void write(Path path, String content) {
        var os = System.getProperty("os.name").toLowerCase();
        var prefix = os.startsWith("win") ? "." : "/";
        List<Object> arr = new ArrayList<>(Arrays.asList(path.toString().split(File.separator)));
        arr.add(content);
        Map<String, Object> tree = UtilModule.setIn(new HashMap<>(), arr);
        writeFilesFromTree(tree, prefix);
    }

    /**
     * 把用tree对象表示的各个文件写入磁盘，每个文件用prefix作为前缀
     * tree的形式为 { a: { b: { c: "filecontent" }}}
     */
    @SuppressWarnings("unchecked")
    public static void writeFilesFromTree(Map<String, Object> tree, String prefix) {
        for (Map.Entry<String, Object> e : tree.entrySet()) {
            try {
                Path p = Path.of(prefix, e.getKey());
                if (e.getValue() instanceof String) {
                    Files.writeString(p, (String) e.getValue());
                } else {
                    if (!Files.exists(p)) {
                        Files.createDirectory(p);
                    }
                    writeFilesFromTree((Map<String, Object>) e.getValue(), p.toString());
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * 当前路径+path 与 仓库根目录的相对路径
     */
    public static Path pathFromRepoRoot(String path) {
        Path repoRoot = workingCopyPath(null);
        Path cwdPath = cwd().resolve(path);
        return repoRoot.relativize(cwdPath);
    }

    /**
     * 返回路径：仓库根目录 + path
     */
    public static Path workingCopyPath(String path) {
        Path p = gitletPath(null);
        if (p != null) {
            return p.getParent().resolve(path == null ? "" : path);
        }
        throw new RuntimeException("当前目录非git仓库");
    }

    public static void assertInRepo() {
        if (!inRepo()) {
            throw new RuntimeException("当前目录非git仓库");
        }
    }

    /**
     * 当前目录是否在git仓库里
     */
    public static boolean inRepo() {
        return gitletPath(null) != null;
    }

    /**
     * 返回仓库.gitlet目录的绝对路径 + path
     * 如果当前目录不在git仓库里，返回null
     */
    public static Path gitletPath(String path) {
        Path gDir = gitletDir(cwd());
        if (gDir != null) {
            return gDir.resolve(path == null ? "" : path);
        }
        return null;
    }

    /**
     * 读取文件文本内容
     */
    public static String read(Path path) {
        if (Files.exists(path)) {
            try {
                return Files.readString(path, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    /**
     * 从dir找到所属的git仓库的git对象目录
     * 普通仓库返回.git目录
     * 纯仓库返回仓库根目录
     * 如果返回null，dir不属于任何git仓库
     */
    private static Path gitletDir(Path dir) {
        if (!Files.exists(dir)) {
            return null;
        }
        var potentialConfigFile = dir.resolve("config");
        var potentialGitletPath = dir.resolve(".gitlet");
        if (Files.exists(potentialConfigFile)) {
            if (Files.isRegularFile(potentialConfigFile)) {
                String content = FilesModule.read(potentialConfigFile);
                if (content != null) {
                    if (p.matcher(content).find()) {
                        return dir;
                    }
                }
            }
        }
        if (Files.exists(potentialGitletPath)) {
            return potentialGitletPath;
        }
        if (dir != dir.getRoot()) {
            return gitletDir(dir.getParent());
        }
        return null;
    }

    public static Path cwd() {
        return Path.of(System.getProperty("user.dir"));
    }


    private static final Pattern p = Pattern.compile("\\[core\\]");
}
