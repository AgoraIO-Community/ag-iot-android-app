package io.agora.sdkwayang.logger.aspectJ;

import android.util.Log;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import java.util.Arrays;
import io.agora.sdkwayang.logger.WLog;
import io.agora.sdkwayang.protocol.ApiResult;

/**
 * Created by yong on 2018/6/15.
 */

@Aspect
public class AspectJInject {
    private final static String TAG = "IOTWY/AspectJInj";


    //excute function
//    @Before("execution(* io.agora.wayang.control.ServerCommandExecute.apis.*.API*(..))")
//    public void  reflectionMethodPrint(JoinPoint point) throws Throwable {
//       log.info(" @Before reflectionMethodPrint method：" +
//                point.getSignature().getDeclaringTypeName() +
//                "." + point.getSignature().getName());
//    }

    //count compute time
    @Around("execution(* io.agora.wayang.control.ServerCommandExecute.WorkerThread*.API*(..))")
    public Object onApiTimeCompute(ProceedingJoinPoint point) throws Throwable {
        WLog.getInstance().d(TAG, "@Before WorkerThread execute：" + point.getSignature().getDeclaringTypeName() + "." + point.getSignature().getName());
        long startTime = System.currentTimeMillis();
        ApiResult returnValue = (ApiResult) point.proceed();
        long endTime = System.currentTimeMillis();
        long processTime = endTime - startTime;
        WLog.getInstance().d(TAG, "@After WorkerThread execute:" + point.getSignature().getDeclaringTypeName() + "." + point.getSignature().getName() + "." + processTime + "ms");
        returnValue.setProcessTime(processTime);
        return returnValue;
    }


    //callback function
    @Before("execution(* io.agora.wayang.control.rtcEngineControl.rtcCallback.RtcEngineEventHandler*.on*(..))")
    public void callbackMethodPrint(JoinPoint point) throws Throwable {
        WLog.getInstance().d(TAG,"@Before callbackMethodPrint:" + point.getSignature().getName() +
                " " + Arrays.toString(point.getArgs()));
    }

    //sdk execute function
    @Around("execution(* io.agora.wayang.control.ServerCommandExecute.apis.SdkApis*.API*(..))")
    public Object callSdkApis(ProceedingJoinPoint point) throws Throwable {
        WLog.getInstance().d(TAG,"@Before callSdkApis execution:" + point.getSignature().getName() +
                " " + Arrays.toString(point.getArgs()));
        ApiResult returnValue = (ApiResult) point.proceed();
        WLog.getInstance().d(TAG,"@After callSdkApis execution:" + point.getSignature().getName() +
                " over");
        return returnValue;
    }

    //other api execute function
    @Around("execution(* io.agora.wayang.control.ServerCommandExecute.apis.OtherActionApis*.API*(..))")
    public Object otherApis(ProceedingJoinPoint point) throws Throwable {
        WLog.getInstance().d(TAG,"@Before otherApis execution:" + point.getSignature().getName() +
                " " + Arrays.toString(point.getArgs()));
        ApiResult returnValue = (ApiResult) point.proceed();
        WLog.getInstance().d(TAG,"@After otherApis execution:" + point.getSignature().getName() +
                " over");
        return returnValue;
    }

    //complexScence api execute function
    @Around("execution(* io.agora.wayang.control.ServerCommandExecute.apis.ComplexScenceApis*.API*(..))")
    public Object complexScenceApis(ProceedingJoinPoint point) throws Throwable {
        WLog.getInstance().d(TAG,"@Before complexScenceApis execution:" + point.getSignature().getName() +
                " " + Arrays.toString(point.getArgs()));
        ApiResult returnValue = (ApiResult) point.proceed();
        WLog.getInstance().d(TAG,"@After complexScenceApis execution:" + point.getSignature().getName() +
                " over");
        return returnValue;
    }


    //CallReportSpecialApis api execute function
    @Around("execution(* io.agora.wayang.control.ServerCommandExecute.apis.CallReportSpecialApis*.API*(..))")
    public Object callReportSpecialApis(ProceedingJoinPoint point) throws Throwable {
        WLog.getInstance().d(TAG,"@Before callReportSpecialApis execution:" + point.getSignature().getName() +
                " " + Arrays.toString(point.getArgs()));
        ApiResult returnValue = (ApiResult) point.proceed();
        WLog.getInstance().d(TAG,"@After callReportSpecialApis execution:" + point.getSignature().getName() +
                " over");
        return returnValue;
    }


}
