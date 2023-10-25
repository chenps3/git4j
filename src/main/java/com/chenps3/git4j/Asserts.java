package com.chenps3.git4j;

/**
 * @Author chenguanhong
 * @Date 2023/10/25
 */
public class Asserts {

    public static void assertTrue(boolean condition, String errMsg) {
        if (!condition) {
            throw new RuntimeException(errMsg);
        }
    }
}
