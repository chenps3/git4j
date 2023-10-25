package com.chenps3.git4j.modules;

import com.chenps3.git4j.Asserts;
import com.chenps3.git4j.DiffData;

import java.util.*;

/**
 * @Author chenguanhong
 * @Date 2023/10/16
 */
public class DiffModule {

    public static void addedOrModifiedFiles() {
        Map<String, String> headToc = new HashMap<>();
        var headHash = RefsModule.hash("HEAD");
        if (headHash != null) {
            headToc = ObjectsModule.commitToc(headHash);
        }
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
}
