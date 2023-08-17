package io.agora.sdkwayang;

import io.agora.sdkwayang.util.EnumClass;



public class ErrorData {
    private EnumClass.ErrorType errorType;
    private Object errorReason;

    public EnumClass.ErrorType getErrorType() {
        return errorType;
    }

    public void setErrorType(EnumClass.ErrorType errorType) {
        this.errorType = errorType;
    }

    public Object getErrorReason() {
        return errorReason;
    }

    public void setErrorReason(Object errorReason) {
        this.errorReason = errorReason;
    }
}
