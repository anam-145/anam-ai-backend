package anam_145.SpringBoot.Server.apiPayload.code;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@Builder
@AllArgsConstructor
public class ReasonDTO {
    private final HttpStatus httpStatus;
    private final boolean isSuccess;
    private final String code;
    private final String message;

    public boolean getIsSuccess(){return isSuccess;}
}
