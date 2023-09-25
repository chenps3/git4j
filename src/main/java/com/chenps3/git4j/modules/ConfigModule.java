package com.chenps3.git4j.modules;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author chenguanhong
 * @Date 2023/9/23
 */
public class ConfigModule {

    /**
     * 解析config字符串转为配置对象
     */
    public static Map<String, Object> strToObj(String str) {
//        Map<String, Object> m = new HashMap<>();
//        return Arrays.stream(str.split("\\["))
//                .map(String::trim)
//                .filter(i -> !i.isEmpty())
//                .map(item -> {
//                    String[] lines = item.split("\n");
//                    List<String> entry = new ArrayList<>();
//
//                })
        return null;
    }

    /**
     * configObj是个对象map，维护仓库的配置
     * 将其转为字符串形式，是strToObj的逆操作
     */
    @SuppressWarnings("unchecked")
    public static String objToStr(Map<String, Map<String, Object>> configObj) {
        List<Map<String, String>> arr = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> e : configObj.entrySet()) {
            String section = e.getKey();
            Map<String, Object> subsection = e.getValue();
            List<Map<String, String>> list =
                    subsection.keySet().stream().map(i -> Map.of("section", section, "subsection", i)).toList();
            arr.addAll(list);
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, String> m : arr) {
            String section = m.get("section");
            String subsection = "".equals(m.get("subsection")) ? "" : " \"" + m.get("subsection") + "\"";
            Map<String, Object> settings = (Map<String, Object>) configObj.get(section).get(subsection);
            String subSettings = settings.keySet()
                    .stream().map(i -> "  %s = %s".formatted(i, settings.get(i)))
                    .collect(Collectors.joining("\n"));
            String i = "[%s%s]\n".formatted(section, subsection) + subSettings + "\n";
            sb.append(i);
        }
        return sb.toString();
    }


    public static void assertNotBare() {

    }

    /**
     * 当前仓库是否纯仓库
     */
    public static boolean isBare() {
        return false;
    }

    public static String read() {
        return "false";
    }
}
