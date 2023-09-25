package com.chenps3.git4j;

import java.util.Map;

/**
 * @Author chenguanhong
 * @Date 2023/9/22
 */
public class Main {

    public static void main(String[] args) {
        Map<String, Object> opts = Map.of("bare", "true");
        Git4j.init(opts);
    }
}
