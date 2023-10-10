package com.chenps3.git4j;

/**
 * @Author chenguanhong
 * @Date 2023/9/22
 */
public class Main {

    public static void main(String[] args) {
        Git4j.init(null);
        Git4j.add("test.txt");
        Git4j.add("testdir");
    }
}
