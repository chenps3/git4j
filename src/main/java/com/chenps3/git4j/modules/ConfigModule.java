package com.chenps3.git4j.modules;

import com.chenps3.git4j.Asserts;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Author chenguanhong
 * @Date 2023/9/23
 */
public class ConfigModule {

    private static Pattern pattern = Pattern.compile("([^ \\]]+)( |\\])");
    private static Pattern subsectionPattern = Pattern.compile("\\\"(.+)\\\"");


    /**
     * 解析config字符串转为配置对象
     */
    public static Map<String, Object> strToObj(String str) {
        Map<String, Object> m = new HashMap<>();
        m.put("remote", new HashMap<>());
        String[] cfgs = str.split("\\[");

        for (int i = 0; i < cfgs.length; i++) {
            String cfg = cfgs[i].trim();
            if (cfg.isEmpty()) {
                continue;
            }
            String[] lines = cfg.split("\n");
            List<Object> entry = new ArrayList<>();
            Matcher matcher = pattern.matcher(lines[0]);
            if (matcher.matches()) {
                entry.add(matcher.group(1));
            }
            String subsection = "";
            Matcher subsectionMatch = subsectionPattern.matcher(lines[0]);
            if (subsectionMatch.matches()) {
                subsection = subsectionMatch.group(2);
            }
            entry.add(subsection);
            //配置
            Map<String, String> map = new HashMap<>();
            for (int j = 1; j < lines.length; j++) {
                String line = lines[j];
                String[] vals = line.split("=");
                map.put(vals[0].trim(), vals[1].trim());
            }
            entry.add(map);
            UtilModule.setIn(m, entry);
        }
        return m;
    }

    /**
     * configObj是个对象map，维护仓库的配置
     * 将其转为字符串形式，是strToObj的逆操作
     */
    @SuppressWarnings("unchecked")
    public static String objToStr(Map<String, Object> configObj) {
        List<Map<String, String>> arr = new ArrayList<>();
        for (var e : configObj.entrySet()) {
            String section = e.getKey();
            Map<String, Object> subsection = (Map<String, Object>) e.getValue();
            List<Map<String, String>> list =
                    subsection.keySet().stream().map(i -> Map.of("section", section, "subsection", i)).toList();
            arr.addAll(list);
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, String> m : arr) {
            String section = m.get("section");
            String subsection = "".equals(m.get("subsection")) ? "" : " \"" + m.get("subsection") + "\"";
            Map<String, Object> subsectionMap = (Map<String, Object>) configObj.get(section);
            Map<String, Object> settings = (Map<String, Object>) subsectionMap.get(subsection);
            String subSettings = settings.keySet()
                    .stream().map(i -> "  %s = %s".formatted(i, settings.get(i)))
                    .collect(Collectors.joining("\n"));
            String i = "[%s%s]\n".formatted(section, subsection) + subSettings + "\n";
            sb.append(i);
        }
        return sb.toString();
    }


    public static void assertNotBare() {
        if (isBare()) {
            throw new RuntimeException("add 操作必须在work tree执行");
        }
    }

    /**
     * 当前仓库是否纯仓库
     */
    @SuppressWarnings("unchecked")
    public static boolean isBare() {
        var coreSection = (Map<String, Object>) read().get("core");
        var blankSubSection = (Map<String, Object>) coreSection.get("");
        return Objects.equals("true", blankSubSection.get("bare"));
    }

    /**
     * 以对象形式返回config文件
     */
    public static Map<String, Object> read() {
        var configPath = FilesModule.gitletPath("config");
        var configStr = FilesModule.read(configPath);
        if (configStr == null) {
            throw new RuntimeException("找不到config文件");
        }
        return strToObj(configStr);
    }

    /**
     * 配置对象写入文件
     */
    public static void write(Map<String, Object> configObj) {
        var path = FilesModule.gitletPath("config");
        Asserts.assertTrue(path != null, "not in repo");
        var content = objToStr(configObj);
        FilesModule.write(path, content);
    }
}
