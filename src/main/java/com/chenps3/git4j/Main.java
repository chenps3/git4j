package com.chenps3.git4j;

/**
 * @Author chenguanhong
 * @Date 2023/9/22
 */
public class Main {

    public static void main(String[] args) {
        String arg = null;
        if (args != null && args.length > 0) {
            arg = args[0];
        }
        if ("init".equals(arg)) {
            Git4j.init(null);
        }
        if ("add".equals(arg)) {
            Git4j.add("test.txt");
            Git4j.add("testdir");
        }
        if ("rm".equals(arg)) {
            Git4j.rm("test.txt", null);
        }
    }
}
