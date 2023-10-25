package com.chenps3.git4j;

/**
 * @Author chenguanhong
 * @Date 2023/9/22
 */
public class Main {

    public static void main(String[] args) {
        if (args != null && args.length > 0) {
            String arg = args[0];
        }
        if (args.equals("init")) {
            Git4j.init(null);
        }
        if (args.equals("add")) {
            Git4j.add("test.txt");
            Git4j.add("testdir");
        }
        if (args.equals("rm")) {
            Git4j.rm("test.txt", null);
        }
    }
}
