package com.chenps3.git4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author chenguanhong
 * @Date 2023/9/22
 */
public class Main {

    public static void main(String[] args) {
        if ("init".equals(args[0])) {
            Git4j.init(null);
        }
        if ("add".equals(args[0])) {
            Git4j.add(args[1]);
        }
        if ("rm".equals(args[0])) {
            Git4j.rm(args[1], null);
        }
        if ("commit".equals(args[0])) {
            Map<String, String> opts = new HashMap<>();
            opts.put("m", args[1]);
            Git4j.commit(opts);
        }
        if ("branch".equals(args[0])) {
            var tmp = Git4j.branch(args[1], null);
            System.out.println(tmp);
        }
        if ("checkout".equals(args[0])) {
            var tmp = Git4j.checkout(args[1]);
            System.out.println(tmp);
        }
        if ("diff".equals(args[0])) {
            var tmp = Git4j.diff(args[1], args[2], null);
            System.out.println(tmp);
        }
    }
}
