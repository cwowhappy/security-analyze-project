package org.cwowhappy.securityanalyze.shared.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 共享异常类单元测试。
 */
class SharedExceptionTest {

    @Test
    void shouldCreateNotFoundExceptionWithMessage() {
        NotFoundException ex = new NotFoundException("Stock", "000001");
        assertThat(ex.getMessage()).contains("Stock", "000001");
    }

    @Test
    void shouldCreateDomainExceptionWithMessage() {
        DomainException ex = new DomainException("领域错误");
        assertThat(ex.getMessage()).isEqualTo("领域错误");
    }

    @Test
    void shouldCreateApplicationExceptionWithMessage() {
        ApplicationException ex = new ApplicationException("应用错误");
        assertThat(ex.getMessage()).isEqualTo("应用错误");
    }

    @Test
    void shouldCreateInfrastructureExceptionWithMessage() {
        InfrastructureException ex = new InfrastructureException("基础设施错误");
        assertThat(ex.getMessage()).isEqualTo("基础设施错误");
    }

    @Test
    void shouldCreateDomainExceptionWithCause() {
        RuntimeException cause = new RuntimeException("原始异常");
        DomainException ex = new DomainException("包装异常", cause);
        assertThat(ex.getMessage()).isEqualTo("包装异常");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    void shouldCreateApplicationExceptionWithCause() {
        RuntimeException cause = new RuntimeException("原始异常");
        ApplicationException ex = new ApplicationException("应用异常", cause);
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    void shouldCreateInfrastructureExceptionWithCause() {
        RuntimeException cause = new RuntimeException("原始异常");
        InfrastructureException ex = new InfrastructureException("基础设施异常", cause);
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}
