package io.agora.sdkwayang.protocol;


public class ApiResult {
    private Object apiResult = null;
    private long processTime = 0;

    public Object getApiResult() {
        return apiResult;
    }

    public void setApiResult(Object apiResult) {
        this.apiResult = apiResult;
    }

    public long getProcessTime() {
        return processTime;
    }

    public void setProcessTime(long processTime) {
        this.processTime = processTime;
    }

    public ApiResult(Object apiResult, long processTime) {
        this.apiResult = apiResult;
        this.processTime = processTime;
    }

    public ApiResult(Object apiResult) {
        this.apiResult = apiResult;
    }
}
