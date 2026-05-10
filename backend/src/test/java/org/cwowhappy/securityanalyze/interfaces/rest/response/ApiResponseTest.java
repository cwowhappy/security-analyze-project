package org.cwowhappy.securityanalyze.interfaces.rest.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ApiResponse 单元测试。
 */
class ApiResponseTest {

    @Test
    void shouldCreateSuccessResponseWithData() {
        ApiResponse<String> response = ApiResponse.success("hello");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEqualTo("hello");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void shouldCreateSuccessResponseWithMessageAndData() {
        ApiResponse<String> response = ApiResponse.success("操作成功", "hello");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("操作成功");
        assertThat(response.getData()).isEqualTo("hello");
    }

    @Test
    void shouldCreateErrorResponseWithCodeAndMessage() {
        ApiResponse<Void> response = ApiResponse.error(404, "资源不存在");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo(404);
        assertThat(response.getMessage()).isEqualTo("资源不存在");
        assertThat(response.getData()).isNull();
    }

    @Test
    void shouldCreateErrorResponseWithDefaultCode() {
        ApiResponse<Void> response = ApiResponse.error("系统错误");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo(500);
        assertThat(response.getMessage()).isEqualTo("系统错误");
    }
}
