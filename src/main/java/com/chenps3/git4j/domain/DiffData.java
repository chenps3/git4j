package com.chenps3.git4j.domain;

import com.chenps3.git4j.modules.DiffFileStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author chenguanhong
 * @Date 2023/10/25
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiffData {

    private DiffFileStatus status;
    private String receiver;
    private String giver;
    private String base;
}
