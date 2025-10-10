package anam_145.SpringBoot.Server.apiPayload.code.status;

import anam_145.SpringBoot.Server.apiPayload.code.BaseErrorCode;
import anam_145.SpringBoot.Server.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {

    // 가장 일반적인 응답
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 에러, 관리자에게 문의 바랍니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_401", "인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_403", "금지된 요청입니다."),

    // 인증 관련 에러
    AUTH_CODE_NOT_FOUND(HttpStatus.BAD_REQUEST, "AUTH_400_03", "유효한 인증번호가 없습니다."),
    AUTH_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_400_04", "인증번호가 올바르지 않습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "USER_401_01", "이메일 또는 비밀번호가 일치하지 않습니다."),

    // 토큰 관련 에러
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_01", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_02", "만료된 토큰입니다. 토큰의 만료 시간이 지나 더 이상 유효하지 않습니다."),
    UNSUPPORTED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_03", "지원하지 않는 토큰입니다. 서버가 처리할 수 없는 형식의 토큰입니다."),
    MALFORMED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_04", "손상된 토큰입니다. 토큰 구조가 올바르지 않거나, 일부 데이터가 손실되었습니다."),
    MISSING_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_05", "토큰이 제공되지 않았습니다."),
    INVALID_TOKEN_FORMAT(HttpStatus.UNAUTHORIZED, "AUTH_401_06", "잘못된 토큰 형식입니다. 토큰이 Base64 URL 인코딩 규칙을 따르지 않거나, 토큰 내부 JSON 구조가 잘못 되었습니다."),
    INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED, "AUTH_401_07", "토큰의 서명이 올바르지 않습니다. 서명이 서버에서 사용하는 비밀키와 일치하지 않습니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_404_01", "데이터베이스에서 refreshToken을 찾을 수 없습니다."),


    INVALID_AUTHORIZATION_CODE(HttpStatus.BAD_REQUEST, "AUTH_400_05", "유효하지 않은 인가 코드입니다."),
    APPLE_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH_500_04", "ID Token 또는 JWKs 파싱에 실패했습니다."),

    // 약관 관련 에러
    MANDATORY_TERMS_REQUIRED(HttpStatus.BAD_REQUEST, "TERM_400_01", "필수 약관에 반드시 동의해야 합니다."),
    ALL_TERMS_REQUIRED(HttpStatus.BAD_REQUEST, "TERM_400_02", "전체 약관 정보를 주세요."),
    TERM_NOT_FOUND(HttpStatus.NOT_FOUND, "TERM_404_01", "존재하지 않는 약관을 요청했습니다."),
    INVALID_TERM(HttpStatus.BAD_REQUEST, "TERM_400_03", "유효하지 않은 약관입니다."),

    // 비밀번호 에러
    PASSWORD_NOT_MATCH(HttpStatus.BAD_REQUEST, "PWD_400_01", "비밀번호 확인이 일치하지 않습니다."), // UserErrorStatus 로 이동
    PASSWORD_UPDATE_NO_CHANGE(HttpStatus.BAD_REQUEST, "PWD_400_02", "새로운 비밀번호를 입력해주세요."),
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
