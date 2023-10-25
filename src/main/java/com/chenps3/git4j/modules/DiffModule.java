package com.chenps3.git4j.modules;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @Author chenguanhong
 * @Date 2023/10/16
 */
public class DiffModule {

    public static void addedOrModifiedFiles() {
        Map<String, Object> headToc = new HashMap<>();
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
    public static void tocDiff(Map<String, Object> receiver, Map<String, Object> giver, Map<String, Object> base) {

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
