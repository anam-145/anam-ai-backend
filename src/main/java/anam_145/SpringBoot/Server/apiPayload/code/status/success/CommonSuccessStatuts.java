package anam_145.SpringBoot.Server.apiPayload.code.status.success;

import anam_145.SpringBoot.Server.apiPayload.code.BaseCode;
import anam_145.SpringBoot.Server.apiPayload.code.ReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CommonSuccessStatuts implements BaseCode {

    _OK(HttpStatus.OK, "COMMON200", "성공입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;


    @Override
    public ReasonDTO getReason() { //비즈니스 로직에서 responseBody에 사용 -> HttpStaus코드 필요 x
        return ReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(true)
                .build();
    }

    @Override
    public ReasonDTO getReasonHttpStatus() {
        return ReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(true)
                .httpStatus(httpStatus)
                .build();
    }
}
