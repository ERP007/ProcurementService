package org.fallguys.procurementservice.adapter.inbound.web;

import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private MethodArgumentNotValidException exceptionWith(FieldError... errors) {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        for (FieldError error : errors) {
            bindingResult.addError(error);
        }
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        given(ex.getBindingResult()).willReturn(bindingResult);
        return ex;
    }

    @Test
    void 검증_실패시_필드명_없이_첫_번째_메시지만_detail로_노출() {
        MethodArgumentNotValidException ex = exceptionWith(
                new FieldError("request", "vendorCode", "공급사 코드는 필수입니다."),
                new FieldError("request", "warehouseCode", "창고 코드는 필수입니다.")
        );

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, null);

        ProblemDetail pd = (ProblemDetail) response.getBody();
        assertThat(pd).isNotNull();
        assertThat(pd.getDetail()).isEqualTo("공급사 코드는 필수입니다.");
        assertThat(pd.getDetail()).doesNotContain("vendorCode");
        assertThat(pd.getProperties()).containsEntry("errorCode", ProcurementErrorCode.VALIDATION_FAILED.getCode());
    }

    @Test
    void 메시지가_없거나_공백인_필드는_건너뛰고_유효한_첫_메시지를_사용() {
        MethodArgumentNotValidException ex = exceptionWith(
                new FieldError("request", "a", null),
                new FieldError("request", "b", "  "),
                new FieldError("request", "c", "수량은 1 이상이어야 합니다.")
        );

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, null);

        ProblemDetail pd = (ProblemDetail) response.getBody();
        assertThat(pd).isNotNull();
        assertThat(pd.getDetail()).isEqualTo("수량은 1 이상이어야 합니다.");
    }

    @Test
    void 필드_에러가_없으면_기본_검증_메시지를_사용() {
        MethodArgumentNotValidException ex = exceptionWith();

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, null);

        ProblemDetail pd = (ProblemDetail) response.getBody();
        assertThat(pd).isNotNull();
        assertThat(pd.getDetail()).isEqualTo(ProcurementErrorCode.VALIDATION_FAILED.getMessage());
    }
}
