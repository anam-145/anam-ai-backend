package anam_145.SpringBoot.Server.apiPayload.exception.handler;

import anam_145.SpringBoot.Server.apiPayload.code.BaseErrorCode;
import anam_145.SpringBoot.Server.apiPayload.exception.GeneralException;

/**
 * HTML 파일 파싱 과정에서 발생하는 예외
 */
public class HtmlParsingException extends GeneralException {

    public HtmlParsingException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
