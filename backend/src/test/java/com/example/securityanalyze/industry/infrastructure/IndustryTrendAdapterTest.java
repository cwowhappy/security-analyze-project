package com.example.securityanalyze.industry.infrastructure;

import com.example.securityanalyze.industry.domain.IndustryTrendPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndustryTrendAdapterTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private IndustryTrendAdapter industryTrendAdapter;

    @Test
    void shouldReturnTrendDataWhenScriptReturnsValidJsonArray() throws Exception {
        IndustryTrendAdapter spyAdapter = spy(industryTrendAdapter);
        String json = "[{\"date\":\"2024-01\",\"close\":100,\"changePercent\":5.5}]";
        doReturn(json).when(spyAdapter).executeScript(anyString(), anyString());

        List<IndustryTrendPoint> result = spyAdapter.fetchTrend("白酒", "1y");

        assertEquals(1, result.size());
        assertEquals("2024-01", result.get(0).getDate());
        assertEquals(0, new java.math.BigDecimal("100").compareTo(result.get(0).getClose()));
    }

    @Test
    void shouldReturnEmptyListWhenScriptReturnsJsonObjectThatCannotBeParsedAsList() throws Exception {
        IndustryTrendAdapter spyAdapter = spy(industryTrendAdapter);
        // 以 { 开头的 JSON 对象，但无法反序列化为 List，最终会进入 catch 返回空列表
        String json = "{\"date\":\"2024-01\",\"close\":100,\"changePercent\":5.5}";
        doReturn(json).when(spyAdapter).executeScript(anyString(), anyString());

        List<IndustryTrendPoint> result = spyAdapter.fetchTrend("白酒", "1y");

        assertTrue(result.isEmpty(), "JSON 对象无法解析为 List<IndustryTrendPoint>，应返回空列表");
    }

    @Test
    void shouldReturnEmptyListWhenScriptOutputIsBlank() throws Exception {
        IndustryTrendAdapter spyAdapter = spy(industryTrendAdapter);
        doReturn("").when(spyAdapter).executeScript(anyString(), anyString());

        List<IndustryTrendPoint> result = spyAdapter.fetchTrend("白酒", "1y");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyListWhenScriptOutputIsNotJson() throws Exception {
        IndustryTrendAdapter spyAdapter = spy(industryTrendAdapter);
        doReturn("ERROR: script failed").when(spyAdapter).executeScript(anyString(), anyString());

        List<IndustryTrendPoint> result = spyAdapter.fetchTrend("白酒", "1y");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyListWhenScriptThrowsException() throws Exception {
        IndustryTrendAdapter spyAdapter = spy(industryTrendAdapter);
        doThrow(new RuntimeException("超时")).when(spyAdapter).executeScript(anyString(), anyString());

        List<IndustryTrendPoint> result = spyAdapter.fetchTrend("白酒", "1y");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyListWhenScriptReturnsInvalidJson() throws Exception {
        IndustryTrendAdapter spyAdapter = spy(industryTrendAdapter);
        doReturn("{").when(spyAdapter).executeScript(anyString(), anyString());

        List<IndustryTrendPoint> result = spyAdapter.fetchTrend("白酒", "1y");

        assertTrue(result.isEmpty());
    }
}
