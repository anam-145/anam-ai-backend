package anam_145.SpringBoot.Server.apiPayload.exception;

import anam_145.SpringBoot.Server.apiPayload.code.BaseErrorCode;

/**
 * 유효하지 않은 ZIP 파일에 대한 예외
 *
 * 업로드된 파일이 ZIP 형식이 아니거나, 비어있거나, 크기가 너무 큰 경우 등
 * 유효성 검사 단계에서 발견된 문제를 나타낸다.
 * 사용자가 올바른 파일을 업로드하도록 유도하기 위해 명확한 에러 메시지를 제공한다.
 */
public class InvalidZipFileException extends GeneralException {

    /**
     * 에러 코드를 받아 예외를 생성한다.
     *
     * @param errorCode 에러 코드 (CommonErrorStatus에 정의됨)
     */
    public InvalidZipFileException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
