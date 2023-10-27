package com.chenps3.git4j.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author chenguanhong
 * @Date 2023/10/26
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KeyPieces {
    private String path;

    private Integer stage;
}
