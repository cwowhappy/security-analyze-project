package org.cwowhappy.securityanalyze.shared.dto;

import lombok.Data;

/**
 * 分页查询参数。
 */
@Data
public class PageQuery {

    private int page = 1;
    private int size = 20;
}
