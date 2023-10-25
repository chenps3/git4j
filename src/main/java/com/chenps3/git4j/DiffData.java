package com.chenps3.git4j;

import com.chenps3.git4j.modules.DiffFileStatus;
import lombok.Data;

/**
 * @Author chenguanhong
 * @Date 2023/10/25
 */
@Data
public class DiffData {

    private DiffFileStatus status;
    private String receiver;
    private String giver;
    private String base;

    public DiffData(DiffFileStatus status, String receiver, String giver, String base) {
        this.status = status;
        this.receiver = receiver;
        this.giver = giver;
        this.base = base;
    }

    public DiffData() {
    }
}
