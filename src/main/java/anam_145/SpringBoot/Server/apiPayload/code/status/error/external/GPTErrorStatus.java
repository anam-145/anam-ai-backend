package anam_145.SpringBoot.Server.apiPayload.code.status.error.external;

import anam_145.SpringBoot.Server.apiPayload.code.BaseErrorCode;
import anam_145.SpringBoot.Server.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
@AllArgsConstructor
public enum GPTErrorStatus implements BaseErrorCode {
    GPT_RESPONSE_EMPTY(HttpStatus.INTERNAL_SERVER_ERROR, "GPT_500_01", "GPT 응답이 비어있습니다. 다시 시도해 주세요."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .build();
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build();
    }
}
