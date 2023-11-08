package com.chenps3.git4j.modules;

/**
 * @Author chenguanhong
 * @Date 2023/10/27
 */
public class MergeModule {

    /**
     * repo是否处于merge状态
     */
    public static boolean isMergeInProgress() {
        return RefsModule.hash("MERGE_HEAD") != null;
    }

    public static boolean isAForceFetch(String receiverHash, String giverHash) {
        return false;
    }
}
