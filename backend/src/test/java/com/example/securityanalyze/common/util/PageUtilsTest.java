package com.example.securityanalyze.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PageUtilsTest {

    @Test
    void shouldNormalizeValidParams() {
        int[] result = PageUtils.normalize(0, 20);
        assertEquals(0, result[0]);
        assertEquals(20, result[1]);
    }

    @Test
    void shouldCapSizeAt100() {
        int[] result = PageUtils.normalize(0, 200);
        assertEquals(100, result[1]);
    }

    @Test
    void shouldSetDefaultSizeWhenLessThan1() {
        int[] result = PageUtils.normalize(0, 0);
        assertEquals(20, result[1]);
    }

    @Test
    void shouldSetPageTo0WhenNegative() {
        int[] result = PageUtils.normalize(-1, 20);
        assertEquals(0, result[0]);
    }

    @Test
    void shouldNormalizeBoundaryValues() {
        int[] result = PageUtils.normalize(5, 100);
        assertEquals(5, result[0]);
        assertEquals(100, result[1]);
    }
}
