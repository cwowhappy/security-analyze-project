package org.cwowhappy.securityanalyze.interfaces.rest.advice;

import org.cwowhappy.securityanalyze.shared.exception.ApplicationException;
import org.cwowhappy.securityanalyze.shared.exception.DomainException;
import org.cwowhappy.securityanalyze.shared.exception.InfrastructureException;
import org.cwowhappy.securityanalyze.shared.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GlobalExceptionHandler 单元测试。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnNotFoundWhenNotFoundException() {
        NotFoundException ex = new NotFoundException("Stock", "999999");
        ResponseEntity<?> response = handler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).extracting("success").isEqualTo(false);
        assertThat(response.getBody()).extracting("code").isEqualTo(404);
        assertThat(response.getBody()).extracting("message").asString().contains("Stock", "999999");
    }

    @Test
    void shouldReturnBadRequestWhenDomainException() {
        DomainException ex = new DomainException("领域规则违反");
        ResponseEntity<?> response = handler.handleBusiness(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).extracting("code").isEqualTo(400);
        assertThat(response.getBody()).extracting("message").isEqualTo("领域规则违反");
    }

    @Test
    void shouldReturnBadRequestWhenApplicationException() {
        ApplicationException ex = new ApplicationException("应用层错误");
        ResponseEntity<?> response = handler.handleBusiness(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).extracting("code").isEqualTo(400);
        assertThat(response.getBody()).extracting("message").isEqualTo("应用层错误");
    }

    @Test
    void shouldReturnInternalErrorWhenInfrastructureException() {
        InfrastructureException ex = new InfrastructureException("数据库连接失败");
        ResponseEntity<?> response = handler.handleInfrastructure(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).extracting("code").isEqualTo(500);
        assertThat(response.getBody()).extracting("message").isEqualTo("系统内部错误");
    }

    @Test
    void shouldReturnBadRequestWhenValidationFailed() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "name", "名称不能为空"));
        bindingResult.addError(new FieldError("request", "code", "代码不能为空"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<?> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).extracting("code").isEqualTo(400);
        assertThat(response.getBody()).extracting("message").asString().contains("名称不能为空", "代码不能为空");
    }

    @Test
    void shouldReturnInternalErrorWhenGenericException() {
        Exception ex = new RuntimeException("未预期错误");
        ResponseEntity<?> response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).extracting("code").isEqualTo(500);
        assertThat(response.getBody()).extracting("message").isEqualTo("系统内部错误");
    }
}
