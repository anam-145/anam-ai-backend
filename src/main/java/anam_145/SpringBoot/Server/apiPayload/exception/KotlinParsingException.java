package anam_145.SpringBoot.Server.apiPayload.exception;

import anam_145.SpringBoot.Server.apiPayload.code.BaseErrorCode;

/**
 * Kotlin 소스 코드 파싱 과정에서 발생하는 예외
 */
public class KotlinParsingException extends GeneralException {

    public KotlinParsingException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
