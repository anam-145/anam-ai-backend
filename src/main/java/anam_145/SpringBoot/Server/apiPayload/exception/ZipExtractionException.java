package anam_145.SpringBoot.Server.apiPayload.exception;

import anam_145.SpringBoot.Server.apiPayload.code.BaseErrorCode;

/**
 * ZIP 파일 추출 과정에서 발생하는 예외
 *
 * ZIP 파일을 처리하는 중 I/O 오류, 파일 읽기 실패 등의 문제가 발생했을 때 던져진다.
 * 이 예외는 일반적으로 복구 불가능한 오류를 나타낸다.
 */
public class ZipExtractionException extends GeneralException {

    /**
     * 에러 코드를 받아 예외를 생성한다.
     *
     * @param errorCode 에러 코드 (CommonErrorStatus에 정의됨)
     */
    public ZipExtractionException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
