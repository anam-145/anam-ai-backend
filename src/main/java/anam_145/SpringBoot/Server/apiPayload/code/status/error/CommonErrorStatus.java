package anam_145.SpringBoot.Server.apiPayload.code.status.error;

import anam_145.SpringBoot.Server.apiPayload.code.BaseErrorCode;
import anam_145.SpringBoot.Server.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
@AllArgsConstructor
public enum CommonErrorStatus implements BaseErrorCode {
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 에러, 관리자에게 문의 바랍니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_401", "인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_403", "금지된 요청입니다."),

    // ZIP 파일 처리 관련 에러
    ZIP_FILE_EMPTY(HttpStatus.BAD_REQUEST, "COMMON4001", "업로드된 ZIP 파일이 비어있습니다."),
    ZIP_FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "COMMON4002", "ZIP 파일 크기가 너무 큽니다. 최대 50MB까지 업로드 가능합니다."),
    ZIP_FILE_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "COMMON4003", "올바른 ZIP 파일 형식이 아닙니다."),
    ZIP_EXTRACTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON5001", "ZIP 파일 추출 중 오류가 발생했습니다."),
    NO_KOTLIN_FILES_FOUND(HttpStatus.BAD_REQUEST, "COMMON4004", "ZIP 파일 내에 Kotlin 파일(.kt)이 없습니다."),
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
