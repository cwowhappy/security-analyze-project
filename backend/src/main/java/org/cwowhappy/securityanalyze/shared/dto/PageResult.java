package org.cwowhappy.securityanalyze.shared.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 分页响应结果。
 */
@Getter
@Builder
public class PageResult<T> {

    private final List<T> list;
    private final long total;
    private final int page;
    private final int size;
}
