package com.chenps3.git4j.modules;

import com.chenps3.git4j.Asserts;
import com.chenps3.git4j.domain.DiffData;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author chenguanhong
 * @Date 2023/10/16
 */
public class DiffModule {

    /**
     * 当前working copy里添加or修改的文件列表
     */
    public static List<String> addedOrModifiedFiles() {
        Map<String, String> headToc = new HashMap<>();
        var headHash = RefsModule.hash("HEAD");
        if (headHash != null) {
            headToc = ObjectsModule.commitToc(headHash);
        }
        var tocDiff = tocDiff(headToc, IndexModule.workingCopyToc(), null);
        var wc = nameStatus(tocDiff);
        return wc.keySet().stream().filter(i -> wc.get(i) != DiffFileStatus.DELETE).collect(Collectors.toList());
    }

    /**
     * receiver giver base，都是文件路径和内容hash的map
     * 返回receiver 和 giver的diff对象
     * base是receiver 和 giver的最近公共祖先；如果base为空，把receiver作为最近公共祖先
     * 只有merge时获取diff才会用到base，一般是处理冲突的场景
     */
    public static Map<String, DiffData> tocDiff(Map<String, String> receiver, Map<String, String> giver, Map<String, String> inputBase) {
        Asserts.assertTrue(receiver != null, "receiver is null");
        Asserts.assertTrue(giver != null, "giver is null");
        var base = inputBase == null ? receiver : inputBase;
        Set<String> paths = new HashSet<>(receiver.keySet());
        paths.addAll(giver.keySet());
        paths.addAll(base.keySet());
        return paths.stream().reduce(new HashMap<>(), (acc, p) -> {
            DiffData diffData = new DiffData(fileStatus(receiver.get(p), giver.get(p), base.get(p)),
                    receiver.get(p),
                    giver.get(p),
                    base.get(p));
            acc.put(p, diffData);
            return acc;
        }, (m1, m2) -> {
            m1.putAll(m2);
            return m1;
        });
    }

    /**
     * receiver giver base，表示文件三个不同版本的内容
     * 返回从receiver到giver进行的变更
     */
    private static DiffFileStatus fileStatus(String receiver, String giver, String base) {
        var receiverPresent = receiver != null;
        var giverPresent = giver != null;
        var basePresent = base != null;
        if (receiverPresent && giverPresent && !Objects.equals(receiver, giver)) {
            if (!Objects.equals(receiver, base) && !Objects.equals(giver, base)) {
                return DiffFileStatus.CONFLICT;
            } else {
                return DiffFileStatus.MODIFY;
            }
        }
        if (Objects.equals(receiver, giver)) {
            return DiffFileStatus.SAME;
        }
        if (!receiverPresent && !basePresent && giverPresent) {
            return DiffFileStatus.ADD;
        }
        if (receiverPresent && !basePresent && !giverPresent) {
            return DiffFileStatus.ADD;
        }
        if (receiverPresent && basePresent && !giverPresent) {
            return DiffFileStatus.DELETE;
        }
        if (!receiverPresent && basePresent && giverPresent) {
            return DiffFileStatus.DELETE;
        }
        return null;
    }

    /**
     * 接收diff对象，返回文件路径->文件状态的映射
     */
    public static Map<String, DiffFileStatus> nameStatus(Map<String, DiffData> diff) {
        Map<String, DiffFileStatus> result = new HashMap<>();
        for (Map.Entry<String, DiffData> e : diff.entrySet()) {
            //过滤same的
            if (e.getValue().getStatus() == DiffFileStatus.SAME) {
                continue;
            }
            result.put(e.getKey(), e.getValue().getStatus());
        }
        return result;
    }

    /**
     * 1 取working copy里变更的文件列表。
     * 2 取head commit和入参hash对应的commit，变更的文件列表
     * 取两者交集，即hash可能把working copy的变更覆盖的文件列表
     */
    public static List<String> changedFilesCommitWouldOverwrite(String hash) {
        var headHash = RefsModule.hash("HEAD");
        //head commit与当前working copy的不同
        var diffOfHeadAndWorkingCopy = DiffModule.diff(headHash, null);
        var diffStatusOfHeadAndWorkingCopy = DiffModule.nameStatus(diffOfHeadAndWorkingCopy);
        //head commit与hash的不同
        var diffOfHeadAndHash = DiffModule.diff(headHash, hash);
        var diffStatusOfHeadAndHash = DiffModule.nameStatus(diffOfHeadAndHash);
        return UtilModule.intersection(new ArrayList<>(diffStatusOfHeadAndWorkingCopy.keySet()),
                new ArrayList<>(diffStatusOfHeadAndHash.keySet()));
    }

    /**
     * 返回2个版本对应的DiffData
     * hash1表示版本1的hash，默认用IndexModule.toc，当前index文件里的tree
     * hash2表示版本2的hash，默认用IndexModule.workingCopyToc，当前工作目录对应的tree
     */
    public static Map<String, DiffData> diff(String hash1, String hash2) {
        var a = hash1 == null ? IndexModule.toc() : ObjectsModule.commitToc(hash1);
        var b = hash2 == null ? IndexModule.workingCopyToc() : ObjectsModule.commitToc(hash2);
        return tocDiff(a, b, null);
    }
}
