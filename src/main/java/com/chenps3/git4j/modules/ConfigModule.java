package com.chenps3.git4j.modules;

import com.chenps3.git4j.UtilModule;

import java.nio.file.Path;
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
    @SuppressWarnings("unchecked")
    public static boolean isBare() {
        Map<String, String> coreConfig = (Map<String, String>) read().get("core");
        return Objects.equals("true",coreConfig.get("bare"));
    }

    public static Map<String, Object> read() {
        Path configPath = FilesModule.gitletPath("config");
        String configStr = FilesModule.read(configPath);
        return strToObj(configStr);
    }
}
