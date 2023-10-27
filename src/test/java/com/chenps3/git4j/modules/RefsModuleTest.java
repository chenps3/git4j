package com.chenps3.git4j.modules;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

/**
 * @Author chenguanhong
 * @Date 2023/10/27
 */
public class RefsModuleTest {

    @Test
    void testPatternP4() {
        String str = "ref: refs/heads/master";
        Matcher m4 = RefsModule.p4.matcher(str);
        if (m4.find()) {
            System.out.println(m4.group(1));
        }
    }
}
