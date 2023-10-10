package com.chenps3.git4j.modules;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author chenguanhong
 * @Date 2023/9/23
 */
public class UtilModule {

    /**
     * arr至少有2个元素
     * 最后一个元素是最内层的value
     * 其它元素作为key
     * 例如
     * setIn({}, ["a", "b", "me"]); // => { a: { b: "me" } }
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> setIn(Map<String, Object> obj, List<Object> arr) {
        if (arr == null) {
            return obj;
        }
        String first = (String) arr.get(0);
        if (arr.size() == 2) {
            obj.put(first, arr.get(1));
        } else if (arr.size() > 2) {
            if (!obj.containsKey(first)) {
                obj.put(first, new HashMap<>());
                setIn((Map<String, Object>) (obj.get(first)), arr.subList(1, arr.size()));
            }
        }
        return obj;
    }

    public static List<String> lines(String str) {
        return str.lines().filter(i -> !i.isBlank()).collect(Collectors.toList());
    }
}
