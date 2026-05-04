package com.example.securityanalyze.common.util;

public final class PageUtils {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private PageUtils() {
        // 工具类禁止实例化
    }

    /**
     * 规范化分页参数
     *
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 规范化后的参数，[0]=page, [1]=size
     */
    public static int[] normalize(int page, int size) {
        if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
        if (size < 1) {
            size = DEFAULT_SIZE;
        }
        if (page < 0) {
            page = 0;
        }
        return new int[]{page, size};
    }
}
