package com.chenps3.git4j;

import com.chenps3.git4j.modules.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
    public static void init(Map<String, String> opts) {
        if (FilesModule.inRepo()) {
            System.out.println("当前目录已经是个git仓库");
            return;
        }
        if (opts == null) {
            opts = new HashMap<>();
        }
        //是否纯仓库
        boolean bare = Boolean.parseBoolean(opts.get("bare"));

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
            _updateIndex(p.toString(), opts);
        }
    }

    /**
     * 把path文件里的内容添加到index或者从index里删除
     */
    public static String _updateIndex(String path, Map<String, String> opts) {
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
    public static void rm(String path, Map<String, ?> opts) {
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
                _updateIndex(p, opt);
            });
        }
    }

    /**
     * 创建一个commit对象，表示当前index的状态
     * 把这个对象写入objects目录，然后把HEAD指向这个commit
     */
    public static String commit(Map<String, String> opts) {
        FilesModule.assertInRepo();
        ConfigModule.assertNotBare();

        var treeHash = _writeTree();
        var headHash = RefsModule.hash("HEAD");
        var headDesc = RefsModule.isHeadDetached() ? "detached HEAD" : RefsModule.headBranchName();
        //比较最新的tree对象和HEAD commit指向的tree对象，如果相同说明没有变更，不需要commit
        if (headHash != null) {
            var headContent = ObjectsModule.read(headHash);
            var headTree = ObjectsModule.treeHash(headContent);
            if (Objects.equals(headTree, treeHash)) {
                throw new RuntimeException("# On " + headDesc + "\nnothing to commit, working directory clean");
            }
        }
        //处于merge状态且冲突未解决，不可commit
        var conflictPaths = IndexModule.conflictPaths();
        var isMergeInProgress = MergeModule.isMergeInProgress();
        if (isMergeInProgress && conflictPaths.size() > 0) {
            String tmp = conflictPaths.stream().map(i -> "U " + i).collect(Collectors.joining("\n"))
                    + "\ncannot commit because you have unmerged files\n";
            throw new RuntimeException(tmp);
        }
        //如果repo处于merge状态，使用MERGE_MSG文件里的内容作为commit message
        //否则读取-m参数作为commit message
        var m = isMergeInProgress ? FilesModule.read(FilesModule.gitletPath("MERGE_MSG")) : opts.get("m");
        //commit写入objects数据库
        var commitHash = ObjectsModule.writeCommit(treeHash, m, RefsModule.commitParentHashes());
        //HEAD指向新的commit
        _updateRef("HEAD", commitHash);
        if (MergeModule.isMergeInProgress()) {
            Path mergeMsgPath = FilesModule.gitletPath("MERGE_MSG");
            Asserts.assertTrue(mergeMsgPath != null, "mergeMsgPath is null");
            try {
                Files.delete(mergeMsgPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            RefsModule.rm("MERGE_HEAD");
            return "Merge made by the three-way strategy";
        }
        //返回提交完成
        return "[" + headDesc + " " + commitHash + "] " + m;
    }

    /**
     * 获取索引内容，并把表示索引内容的tree对象存储到objects目录
     */
    public static String _writeTree() {
        FilesModule.assertInRepo();
        var toc = IndexModule.toc();            //索引内容
        var tree = FilesModule.flattenNestedTree(toc, null, null);      //索引内容转为tree对象
        return ObjectsModule.writeTree(tree);       //写入objects，返回文件名
    }

    /**
     * 获取refToUpdateTo指向的commit hash，把refToUpdate也指向这个commit hash
     */
    public static void _updateRef(String refToUpdate, String refToUpdateTo) {
        FilesModule.assertInRepo();
        var hash = RefsModule.hash(refToUpdateTo);

        //refToUpdateTo 必须指向有效的hash
        if (!ObjectsModule.exists(hash)) {
            throw new RuntimeException(refToUpdateTo + " not a valid SHA1");
        }
        //refToUpdate必须符合ref格式
        if (!RefsModule.isRef(refToUpdate)) {
            throw new RuntimeException("cannot lock the ref " + refToUpdate);
        }
        //hash指向的对象必须是个commit
        var hashContent = ObjectsModule.read(hash);
        if (!Objects.equals("commit", ObjectsModule.type(hashContent))) {
            var branch = RefsModule.terminalRef(refToUpdate);
            throw new RuntimeException(branch + " cannot refer to non-commit object " + hash + "\n");
        }
        var terminalRefToUpdate = RefsModule.terminalRef(refToUpdate);
        RefsModule.write(terminalRefToUpdate, hash);
    }
}
