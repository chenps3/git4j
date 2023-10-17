package com.chenps3.git4j.modules;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * refs是commit hash的名字，文件的名字
 * 一些ref表示本地分支，如refs/heads/master or refs/heads/feature
 * 一些表示仓库的重要状态，比如HEAD, MERGE_HEAD , FETCH_HEAD
 * ref文件的可能包含一个hash，或者另一个ref
 *
 * @Author chenguanhong
 * @Date 2023/10/16
 */
public class RefsModule {

    private static final Pattern p1 = Pattern.compile("^refs/heads/[A-Za-z-]+$");
    private static final Pattern p2 = Pattern.compile("^refs/remotes/[A-Za-z-]+/[A-Za-z-]+$");
    private static final Set<String> s = Set.of("HEAD", "FETCH_HEAD", "MERGE_HEAD");
    private static final Pattern p3 = Pattern.compile("ref: (refs/heads/.+)");
    private static final Pattern p4 = Pattern.compile("refs/heads/(.+)");

    /**
     * 返回refOrHash指向的hash
     */
    public static String hash(String refOrHash) {
        if (ObjectsModule.exists(refOrHash)) {
            return refOrHash;
        }
        var terminalRef = terminalRef(refOrHash);
        if (Objects.equals(terminalRef, "FETCH_HEAD")) {
            return fetchHeadBranchToMerge(refOrHash);
        } else {
            return "";
        }
    }

    /**
     * 解析ref返回最具体的ref
     */
    public static String terminalRef(String ref) {
        //如果ref是head，并且head指向的是一个分支，返回这个分支
        if (Objects.equals(ref, "HEAD") && !isHeadDetached()) {
            String headContent = FilesModule.read(FilesModule.gitletPath("HEAD"));
            Matcher m3 = p3.matcher(headContent == null ? "" : headContent);
            if (m3.find()) {
                return m3.group(1);
            }
        }
        //如果已经是全限定名，直接返回
        if (isRef(ref)) {
            return ref;
        }
        return toLocalRef(ref);
    }

    /**
     * 如果HEAD指向一个commit而不是另一个分支的引用，返回true
     */
    public static boolean isHeadDetached() {
        Path p = FilesModule.gitletPath("HEAD");
        String str = FilesModule.read(p);
        return str != null && !str.contains("refs");
    }

    /**
     * 如果ref与特定语法匹配，返回true
     */
    public static boolean isRef(String ref) {
        if (ref == null) {
            return false;
        }
        Matcher m1 = p1.matcher(ref);
        Matcher m2 = p2.matcher(ref);
        return m1.matches() || m2.matches() || s.contains(ref);
    }

    /**
     * 把分支名name转为本地分支ref
     */
    public static String toLocalRef(String name) {
        return "refs/heads/" + name;
    }

    /**
     * 读取FETCH_HEAD文件，获取 branchName 指向的hash
     */
    public static String fetchHeadBranchToMerge(String branchName) {
        var fetchHeadPath = FilesModule.gitletPath("FETCH_HEAD");
        var fetchHeadContent = FilesModule.read(fetchHeadPath);
        var lines = UtilModule.lines(fetchHeadContent == null ? "" : fetchHeadContent);
        var p1 = Pattern.compile("^.+ branch " + branchName + " of");
        var p2 = Pattern.compile("^([^ ]+) ");
        var tmp = lines.stream().filter(i -> {
            var m = p1.matcher(i);
            return m.matches();
        }).map(i -> {
            var m = p2.matcher(i);
            return m.group(1);
        }).toList();
        return tmp.get(0);
    }

    /**
     * 返回HEAD指针指向的分支名
     */
    public static String headBranchName() {
        //前提是HEAD指向的不是commit而是分支
        if (!isHeadDetached()) {
            Path headPath = FilesModule.gitletPath("HEAD");
            String headContent = FilesModule.read(headPath);
            Matcher m4 = p4.matcher(headContent == null ? "" : headContent);
            return m4.group(1);
        }
        return null;
    }
}
