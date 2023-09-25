package com.chenps3.git4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author chenguanhong
 * @Date 2023/9/23
 */
public class UtilModule {

    @SuppressWarnings("unchecked")
    public static Map<String, Object> setIn(Map<String, Object> obj, List<String> arr) {
        if (arr == null) {
            return obj;
        }
        String first = arr.get(0);
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
}
