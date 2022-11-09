package io.agora.iotlinkdemo.models.device.add;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.utils.ImageConvert;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.utils.ZXingUtils;


public class CameraPreview extends FrameLayout {

    public interface ICameraScanCallback {
        void onQRCodeParsed(final String textQRCode);
    }


    /**
     * @brief 当前使用Camera的属性
     */
    private static class CamProperty {
        public String  mCameraId;           ///< 相机Id
        public Size[]  mCamOutSizes;        ///< Camera支持的输出SurfaceTexture大小
        public boolean mTorchAvailable;     ///< 是否支持手电筒
        public Size    mPreviewSize;        ///< 预览大小

    }


    /**
     * @brief 当前使用Camera的属性
     */
    private static class CaptureData {
        public int mWidth;
        public int mHeight;

        public byte[] mYData;
        public byte[] mUData;
        public byte[] mVData;

        public int mYPxlStride;         ///< Y数据像素间隔
        public int mUPxlStride;         ///< U数据像素间隔
        public int mVPxlStride;         ///< V数据像素间隔

        boolean isValid() {
            if (mWidth <= 0 || mHeight <= 0) {
                return false;
            }
            if (mYData == null || mUData == null || mVData == null) {
                return false;
            }
            return true;
        }
    }



    ////////////////////////////////////////////////////////////////////
    ///////////////////////////// Constant /////////////////////////////
    ////////////////////////////////////////////////////////////////////
    private static final String TAG = "LINK/CAMView";

    //
    // message Id
    //
    public static final int MSGID_SURFACE_AVAILABLE = 0x1001;
    public static final int MSGID_CAMDEV_OPENED = 0x2001;
    public static final int MSGID_CAMDEV_CLOSED = 0x2002;
    public static final int MSGID_CAMDEV_DISCONNECTED = 0x2003;
    public static final int MSGID_CAMDEV_ERROR = 0x2004;
    public static final int MSGID_CAPSESSION_SUCCESS = 0x3001;
    public static final int MSGID_CAPSESSION_FAIL = 0x3002;

    public static final int MSGID_DETECT_DETECTING = 0x8001;
    public static final int MSGID_DETECT_EXIT = 0x8002;


    //////////////////////////////////////////////////////////////////////////////
    ///////////////////////////// Variable Definition /////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////
    private ICameraScanCallback mScanCallback;
    private Handler mMsgHandler = null;             ///< 主线程中的消息处理
    private volatile boolean mRunning = false;      ///< 当前控件是否运行

    private HandlerThread mDetectThread;            ///< 二维码检测线程
    private Handler mDetectHandler;                 ///< 检测线程线程处理器
    private final Object mDetectExitEvent = new Object();

    private TextureView mTextureView;
    private SurfaceTexture mSurfaceTexture;
    private Surface mPreviewSurface;
    private TextureView.SurfaceTextureListener mTextureListener;
    private CaptureData mCaptureData = new CaptureData();

    private CamProperty mCamProperty;                   ///< 当前相机信息
    private CameraDevice mCameraDevice;                 ///< 当前相机设备
    private CameraDevice.StateCallback mCamStateCallbk; ///< Camera状态回调
    private CameraCaptureSession mCaptureSession;       ///< 预览会话
    private CaptureRequest mPreviewReq;                 ///< 预览请求
    private ImageReader mImageReader;                   ///< 预览图像渲染器
    private CaptureRequest.Builder mPreviewReqBuilder;
    private boolean mTorchOpened = false;               ///< 手电筒是否打开


    ////////////////////////////////////////////////////////////////
    ///////////////////////// Public Methods //////////////////////
    ////////////////////////////////////////////////////////////////
    public CameraPreview(@NonNull Context context) {
        super(context);
        initView(context);
    }

    public CameraPreview(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public CameraPreview(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    public CameraPreview(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }

    /**
     * @brief 设置回调接口
     */
    public void setScanCallback(ICameraScanCallback callback) {
        mScanCallback = callback;
    }

    /**
     * @brief 查询支持的Camera设备及其属性
     */
    public boolean querySupportedCamera() {
        CameraManager cameraManager = (CameraManager)this.getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdList = cameraManager.getCameraIdList();
            for (String cameraId : cameraIdList) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                mCamProperty = new CamProperty();

                mCamProperty.mCameraId = cameraId;
                mCamProperty.mTorchAvailable = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                mCamProperty.mCamOutSizes = map.getOutputSizes(SurfaceTexture.class);

                Log.d(TAG, "<querySupportedCamera> done, mCameraId=" + mCamProperty.mCameraId
                        + ", mTorchAvaiable=" + mCamProperty.mTorchAvailable
                        + ", mCamOutSizes=" + mCamProperty.mCamOutSizes);
                return true;
            }
        } catch (CameraAccessException accessExp) {
            accessExp.printStackTrace();
            Log.e(TAG, "<queryMatchedCamera> accessExp=" + accessExp.toString());
        }

        return false;
    }

    /**
     * @brief 设置支持的预览大小
     */
    void setPreviewSize(Size previewSize) {
        mCamProperty.mPreviewSize = previewSize;

        mTextureView = (TextureView)findViewById(R.id.tvCameraPreview);
        FrameLayout.LayoutParams textureLayoutParam = (FrameLayout.LayoutParams)mTextureView.getLayoutParams();
        textureLayoutParam.width = previewSize.getHeight();  // 要注意旋转
        textureLayoutParam.height = previewSize.getWidth();
        mTextureView.setLayoutParams(textureLayoutParam);
    }


    /**
     * @brief 根据输入的显示大小，查找最匹配的Camera大小
     */
    public Size calculateMatchedSize(int width, int height) {
        boolean landscape = (width > height) ? true : false;
        int displayArea = width * height;
        int minAreaDiff = displayArea;
        Size minSize = mCamProperty.mCamOutSizes[0];

        for (Size size : mCamProperty.mCamOutSizes) {
            int camOutArea = size.getWidth() * size.getHeight();
            int areaDiff = Math.abs(camOutArea-displayArea);
            if (areaDiff < minAreaDiff) {
                minAreaDiff = areaDiff;
                minSize = size;
            }
        }

        return minSize;
    }



    /**
     * @brief 启动相机预览，并且开始扫描二维码
     */
    public void scaningStart() {
        Log.d(TAG, "<scaningStart>");
        synchronized (mCaptureData) {  // 这里仅做数据读取
            mCaptureData.mWidth = 0;
            mCaptureData.mHeight= 0;

            mCaptureData.mYData = null;
            mCaptureData.mUData = null;
            mCaptureData.mVData = null;

            mCaptureData.mYPxlStride = 0;
            mCaptureData.mUPxlStride = 0;
            mCaptureData.mVPxlStride = 0;
        }

        mMsgHandler = new Handler(this.getContext().getMainLooper())  {
            @SuppressLint("HandlerLeak")
            public void handleMessage(Message msg)
            {
                switch (msg.what)
                {
                    case MSGID_SURFACE_AVAILABLE:
                        onMsgSurfaceAvailable(msg.arg1, msg.arg2);
                        break;

                    case MSGID_CAMDEV_OPENED:
                        onMsgCameraOpened((CameraDevice)msg.obj);
                        break;
                    case MSGID_CAMDEV_CLOSED:
                        onMsgCameraClosed((CameraDevice)msg.obj);
                        break;
                    case MSGID_CAMDEV_DISCONNECTED:
                        onMsgCameraDisconnected((CameraDevice)msg.obj);
                        break;
                    case MSGID_CAMDEV_ERROR:
                        onMsgCameraError((CameraDevice)msg.obj);
                        break;

                    case MSGID_CAPSESSION_SUCCESS:
                        onMsgCaptureSessionSuccess((CameraCaptureSession)msg.obj);
                        break;

                    case MSGID_CAPSESSION_FAIL:
                        onMsgCaptureSessionFail((CameraCaptureSession)msg.obj);
                        break;

                    default:
                        break;
                }
            }
        };

        mRunning = true;
        if (mCamProperty != null) {
            boolean bRet = cameraDevOpen(mCamProperty.mCameraId);
        }
        detectThreadCreate();
    }

    /**
     * @brief 停止二维码扫描并且关闭相机
     */
    public void scaningStop() {
        Log.d(TAG, "<scaningStop>");
        mRunning = false;
        detectThreadDestroy();

        if (mMsgHandler != null) {
            mMsgHandler.removeMessages(MSGID_SURFACE_AVAILABLE);
            mMsgHandler.removeMessages(MSGID_CAMDEV_OPENED);
            mMsgHandler.removeMessages(MSGID_CAMDEV_CLOSED);
            mMsgHandler.removeMessages(MSGID_CAMDEV_DISCONNECTED);
            mMsgHandler.removeMessages(MSGID_CAMDEV_ERROR);
            mMsgHandler.removeMessages(MSGID_CAPSESSION_SUCCESS);
            mMsgHandler.removeMessages(MSGID_CAPSESSION_FAIL);
            mMsgHandler = null;
        }

        previewStop();
        cameraDevClose();
    }

    /**
     * @brief 打开或者关闭手电筒
     */
    public boolean turnTorch(boolean bOpen) {
        if (mCamProperty == null || (!mCamProperty.mTorchAvailable)) {
            Log.e(TAG, "<turnTorch> NOT support torch");
            return false;
        }
        if (mCameraDevice == null) {
            Log.e(TAG, "<turnTorch> Camera not opened");
            return false;
        }
        String cameraId = mCameraDevice.getId();

        scaningStop();
        try {
            Thread.sleep(200);
        } catch (InterruptedException interruptedExp) {
            interruptedExp.printStackTrace();
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                CameraManager cameraManager = (CameraManager)getContext().getSystemService(Context.CAMERA_SERVICE);
                cameraManager.setTorchMode(cameraId, bOpen);
                mTorchOpened = bOpen;
                Log.d(TAG, "<turnTorch> mTorchOpened=" + mTorchOpened);

                scaningStart();
                return true;
            }

        } catch (CameraAccessException accessExp) {
            accessExp.printStackTrace();
            Log.e(TAG, "<turnTorch> accessExp=" + accessExp.toString());
        }

        scaningStart();
        return false;
    }

    /**
     * @brief 返回手电筒是否已经打开
     */
    public boolean isTorchOpened() {
        return mTorchOpened;
    }


    ////////////////////////////////////////////////////////////////
    ///////////////////////// Internal Methods //////////////////////
    ////////////////////////////////////////////////////////////////
    private void initView(Context context) {
        View view = View.inflate(context, R.layout.view_camera_preview, this);
        mTextureView = (TextureView) view.findViewById(R.id.tvCameraPreview);
        mTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "<onSurfaceTextureAvailable> surface=" + surface
                        + ", width=" + width + ", height=" + height);
                if (mMsgHandler != null) {
                    Message msg = new Message();
                    msg.what = MSGID_SURFACE_AVAILABLE;
                    msg.arg1 = width;
                    msg.arg2 = height;
                    mMsgHandler.removeMessages(MSGID_SURFACE_AVAILABLE);
                    mMsgHandler.sendMessage(msg);
                }
            }
            //下面的方法可以先不看，我们先实现相机预览
            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "<onSurfaceTextureSizeChanged> surface=" + surface
                        + ", width=" + width + ", height=" + height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                Log.d(TAG, "<onSurfaceTextureDestroyed> surface=" + surface);
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                //Log.d(TAG, "<onSurfaceTextureUpdated> surface=" + surface);
            }
        };

        mTextureView.setSurfaceTextureListener(mTextureListener);
    }


    void onMsgSurfaceAvailable(int width, int height) {
        Log.d(TAG, "<onMsgSurfaceAvailable> width=" + width + ", height=" + height);

        if ((mCamProperty != null) && (mRunning)) {
            boolean bRet = cameraDevOpen(mCamProperty.mCameraId);
        }
    }

    void onMsgCameraOpened(CameraDevice cameraDev) {
        Log.d(TAG, "<onMsgCameraOpened> cameraDev=" + cameraDev);
        mCameraDevice = cameraDev;

        previewStart();
        detectTrigger();
    }

    void onMsgCameraClosed(CameraDevice cameraDev) {
        Log.d(TAG, "<onMsgCameraClosed> cameraDev=" + cameraDev);
    }

    void onMsgCameraDisconnected(CameraDevice cameraDev) {
        Log.d(TAG, "<onMsgCameraDisconnected> cameraDev=" + cameraDev);
        cameraDevClose();
    }

    void onMsgCameraError(CameraDevice cameraDev) {
        Log.d(TAG, "<onMsgCameraError> cameraDev=" + cameraDev);
        cameraDevClose();
    }

    void onMsgCaptureSessionSuccess(CameraCaptureSession captureSession) {
        Log.d(TAG, "<onMsgCaptureSessionSuccess> captureSession=" + captureSession);
        mCaptureSession = captureSession;
        previewRequire();
    }

    void onMsgCaptureSessionFail(CameraCaptureSession captureSession) {
        Log.d(TAG, "<onMsgCaptureSessionFail> captureSession=" + captureSession);
    }


    /////////////////////////////////////////////////////////////////////////
    ////////////////////////// Camera相应的操作 ///////////////////////////////
    /////////////////////////////////////////////////////////////////////////
    /**
     * @brief 打开相机设备
     */
    private boolean cameraDevOpen(final String cameraId) {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "<cameraDevOpen> no permission");
            return false;
        }
        if (mCamStateCallbk != null) {  // 相机已经打开
            return true;
        }

        // 创建Camera状态回调函数
        mCamStateCallbk = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                Log.d(TAG, "<cameraDevOpen.onOpened> camera=" + camera);
                if (mMsgHandler != null) {
                    Message msg = new Message();
                    msg.what = MSGID_CAMDEV_OPENED;
                    msg.obj = camera;
                    mMsgHandler.removeMessages(MSGID_CAMDEV_OPENED);
                    mMsgHandler.sendMessage(msg);
                }
            }

            @Override
            public void onClosed(@NonNull CameraDevice camera) {
                Log.d(TAG, "<cameraDevOpen.onClosed> camera=" + camera);
                if (mMsgHandler != null) {
                    Message msg = new Message();
                    msg.what = MSGID_CAMDEV_CLOSED;
                    msg.obj = camera;
                    mMsgHandler.removeMessages(MSGID_CAMDEV_CLOSED);
                    mMsgHandler.sendMessage(msg);
                }
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                // 相机设备不再可用，此时只能关闭相机
                Log.d(TAG, "<cameraDevOpen.onDisconnected> camera=" + camera);
                if (mMsgHandler != null) {
                    Message msg = new Message();
                    msg.what = MSGID_CAMDEV_DISCONNECTED;
                    msg.obj = camera;
                    mMsgHandler.removeMessages(MSGID_CAMDEV_DISCONNECTED);
                    mMsgHandler.sendMessage(msg);
                }
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                Log.d(TAG, "<cameraDevOpen.onError> camera=" + camera);
                if (mMsgHandler != null) {
                    Message msg = new Message();
                    msg.what = MSGID_CAMDEV_ERROR;
                    msg.arg1 = error;
                    msg.obj = camera;
                    mMsgHandler.removeMessages(MSGID_CAMDEV_ERROR);
                    mMsgHandler.sendMessage(msg);
                }
            }
        };

        try {
            CameraManager cameraManager = (CameraManager)getContext().getSystemService(Context.CAMERA_SERVICE);
            cameraManager.openCamera(cameraId, mCamStateCallbk, null);
            Log.d(TAG, "<cameraDevOpen> success, cameraId=" + cameraId);
            return true;

        } catch (CameraAccessException accessExp) {
            accessExp.printStackTrace();
            Log.e(TAG, "<cameraDevOpen> [EXCEPTION] exp=" + accessExp.toString());
        }

        return false;
    }

    /**
     * @brief 关闭相机设备
     */
    private void cameraDevClose() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
            Log.d(TAG, "<cameraDevClose> successful");
        }
        mCamStateCallbk = null;
    }


    /**
     * @brief 启动相机预览
     */
    private boolean previewStart() {
        if (mCameraDevice == null) {
            Log.d(TAG, "<previewStart> bad status");
            return false;
        }

        //
        // 设置预览图像回调渲染器，宽高限定为4像素对齐
        //
        int imgWidth = mCamProperty.mPreviewSize.getWidth();
        int imgHeight = mCamProperty.mPreviewSize.getHeight();
        mImageReader = ImageReader.newInstance(imgWidth, imgHeight, ImageFormat.YUV_420_888, 1);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                try {
                    Image image = reader.acquireNextImage();
                    if (image == null) {
                        return;
                    }
                    if (image.getFormat() != ImageFormat.YUV_420_888) {
                        return;
                    }

                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer yBuffer = planes[0].getBuffer();
                    ByteBuffer uBuffer = planes[1].getBuffer();
                    ByteBuffer vBuffer = planes[2].getBuffer();
                    int ySize = yBuffer.remaining();
                    int uSize = uBuffer.remaining();
                    int vSize = vBuffer.remaining();

                    synchronized (mCaptureData) {  // 这里仅做数据读取
                        mCaptureData.mWidth = image.getWidth();
                        mCaptureData.mHeight= image.getHeight();

                        // 提取Y数据
                        if ((mCaptureData.mYData == null) || (mCaptureData.mYData.length != ySize)) {
                            mCaptureData.mYData = new byte[ySize];
                        }
                        yBuffer.get(mCaptureData.mYData);
                        mCaptureData.mYPxlStride = planes[0].getPixelStride();

                        // 提取U数据
                        if ((mCaptureData.mUData == null) || (mCaptureData.mUData.length != uSize)) {
                            mCaptureData.mUData = new byte[uSize];
                        }
                        uBuffer.get(mCaptureData.mUData);
                        mCaptureData.mUPxlStride = planes[1].getPixelStride();

                        // 提取V数据
                        if ((mCaptureData.mVData == null) || (mCaptureData.mVData.length != vSize)) {
                            mCaptureData.mVData = new byte[vSize];
                        }
                        vBuffer.get(mCaptureData.mVData);
                        mCaptureData.mVPxlStride = planes[2].getPixelStride();
                    }
//                    Log.d(TAG, "<onImageAvailable> width=" + image.getWidth()
//                            + ", height=" + image.getHeight()
//                            + ", ySize=" + ySize + ", yStride=" + mCaptureData.mYPxlStride
//                            + ", uSize=" + uSize + ", uStride=" + mCaptureData.mUPxlStride
//                            + ", vSize=" + vSize + ", vStride=" + mCaptureData.mVPxlStride);

                    image.close();

                } catch (Exception exp) {
                    exp.printStackTrace();
                    Log.e(TAG, "<onImageAvailable> [EXCEPTION] exp=" + exp.toString());
                }
            }
        }, null);


        //
        // 设置预览回调渲染窗口
        //
        mSurfaceTexture = mTextureView.getSurfaceTexture();
        mSurfaceTexture.setDefaultBufferSize(mCamProperty.mPreviewSize.getWidth(), mCamProperty.mPreviewSize.getHeight());
        mPreviewSurface = new Surface(mSurfaceTexture);
        try {
            mPreviewReqBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewReqBuilder.addTarget(mPreviewSurface);
            mPreviewReqBuilder.addTarget(mImageReader.getSurface());


            // 使用闪光灯，必须保证 CONTROL_MODE = AUTO
            mPreviewReqBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);

            // 连续采集
            mPreviewReqBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            // 闪光灯设置
            if (mTorchOpened && mCamProperty.mTorchAvailable) {
                // 持续闪光灯
                //mPreviewReqBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                mPreviewReqBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                mPreviewReqBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
            } else {
                // 无手电筒
                mPreviewReqBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                mPreviewReqBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            }
            mPreviewReq = mPreviewReqBuilder.build();

            mCameraDevice.createCaptureSession(Arrays.asList(mPreviewSurface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "<previewStart.onConfigured> session=" + session);
                    if (mMsgHandler != null) {
                        Message msg = new Message();
                        msg.what = MSGID_CAPSESSION_SUCCESS;
                        msg.obj = session;
                        mMsgHandler.removeMessages(MSGID_CAPSESSION_SUCCESS);
                        mMsgHandler.sendMessage(msg);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "<previewStart.onConfigureFailed> session=" + session);
                    if (mMsgHandler != null) {
                        Message msg = new Message();
                        msg.what = MSGID_CAPSESSION_FAIL;
                        msg.obj = session;
                        mMsgHandler.removeMessages(MSGID_CAPSESSION_FAIL);
                        mMsgHandler.sendMessage(msg);
                    }
                }
            }, null);

            Log.d(TAG, "<previewStart> done");
            return true;

        } catch (CameraAccessException accessExp) {
            accessExp.printStackTrace();
            Log.e(TAG, "<previewStart> [EXCEPTION] exp=" + accessExp.toString());
        }

        return false;
    }


    /**
     * @brief 停止相机预览
     */
    private void previewStop() {
        if (mCaptureSession != null) {
            try {
                mCaptureSession.stopRepeating();
                mCaptureSession.close();

            } catch (CameraAccessException accessExp) {
                accessExp.printStackTrace();
                Log.e(TAG, "<previewStop> [EXCEPTION] exp=" + accessExp.toString());
            }

            mCaptureSession = null;
            Log.d(TAG, "<previewStop> done");
        }
    }

    private boolean previewRequire() {
        if (mPreviewReqBuilder == null) {
            Log.e(TAG, "<previewRequire> bad state");
            return false;
        }

        try {
            mPreviewReqBuilder.setTag(TAG);
            mPreviewReq = mPreviewReqBuilder.build();

            mCaptureSession.setRepeatingRequest(mPreviewReq, new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    //Log.d(TAG, "<onCaptureCompleted> request=" + request);
                }

            },null);

            Log.d(TAG, "<previewRequire> done");
            return true;

        } catch (CameraAccessException accessExp) {
            accessExp.printStackTrace();
            Log.e(TAG, "<previewRequire> [EXCEPTION] exp=" + accessExp.toString());
        }

        return false;
    }


    ///////////////////////////////////////////////////////
    ////////////////// 二维码检测处理 ///////////////////////
    ///////////////////////////////////////////////////////
    void detectThreadCreate() {
        if (mDetectThread != null) { // 线程已经创建
            return;
        }
        mDetectThread = new HandlerThread("QRCodeDetect");
        mDetectThread.start();
        mDetectHandler = new Handler(mDetectThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                switch (msg.what) {
                    case MSGID_DETECT_DETECTING: {
                        onMsgDoDetect();
                    } break;

                    case MSGID_DETECT_EXIT:  // 检测线程退出消息
                        synchronized (mDetectExitEvent) {
                            mDetectExitEvent.notify();    // 事件通知
                        }
                        break;
                }
            }
        };
        Log.d(TAG, "<detectThreadCreate> done");
    }

    void detectThreadDestroy() {
        if (mDetectHandler != null) {
            // 同步等待线程中所有任务处理完成后，才能正常退出线程
            mDetectHandler.sendEmptyMessage(MSGID_DETECT_EXIT);
            synchronized (mDetectExitEvent) {
                try {
                    mDetectExitEvent.wait(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    ALog.getInstance().e(TAG, "<detecthreadDestroy> exception=" + e.getMessage());
                }
            }

            mDetectHandler.removeMessages(MSGID_DETECT_EXIT);
            mDetectHandler.removeMessages(MSGID_DETECT_DETECTING);
            mDetectHandler = null;
        }

        if (mDetectThread != null) {
            mDetectThread.quit();
            mDetectThread = null;
            Log.d(TAG, "<detecthreadDestroy> done");
        }
    }

    void detectTrigger() {
        if ((mDetectHandler != null) && mRunning) {
            mDetectHandler.removeMessages(MSGID_DETECT_DETECTING);
            mDetectHandler.sendEmptyMessageDelayed(MSGID_DETECT_DETECTING, 100);
        }
    }

    void onMsgDoDetect() {
        int i;
        long t1 = System.currentTimeMillis();
        if (!mRunning) {
            return;
        }

        int width  = 0;
        int height = 0;
        byte[] yBytes = null;
        byte[] uBytes = null;
        byte[] vBytes = null;
        synchronized (mCaptureData) {  // 这里仅做数据读取
            if (!mCaptureData.isValid()) {
                detectTrigger();
                return;
            }
            width  = mCaptureData.mWidth;
            height = mCaptureData.mHeight;

            // 拷贝 Y数据
            yBytes = new byte[mCaptureData.mYData.length];
            System.arraycopy(mCaptureData.mYData, 0, yBytes, 0, mCaptureData.mYData.length);


            // 拷贝 U数据
            if (mCaptureData.mUPxlStride == 2) { // SP, 间隔跳过一个提取
                int cpyUSize = (mCaptureData.mUData.length+1) / 2;
                uBytes = new byte[cpyUSize];
                int index_u = 0;
                for(i = 0; i < mCaptureData.mUData.length; i++){
                    if (0 == (i%2)) {
                        uBytes[index_u] = mCaptureData.mUData[i];
                        index_u++;
                    }
                }

            } else  { // P，直接拷贝
                uBytes = new byte[mCaptureData.mUData.length];
                System.arraycopy(mCaptureData.mUData, 0, uBytes, 0, mCaptureData.mUData.length);
            }

            // 拷贝 V数据
            if (mCaptureData.mVPxlStride == 2) { // SP, 间隔跳过一个提取
                int cpyVSize = (mCaptureData.mVData.length+1) / 2;
                vBytes = new byte[cpyVSize];
                int index_v = 0;
                for(i = 0; i < mCaptureData.mVData.length; i++){
                    if (0 == (i%2)) {
                        vBytes[index_v] = mCaptureData.mVData[i];
                        index_v++;
                    }
                }

            } else  { // P，直接拷贝
                vBytes = new byte[mCaptureData.mVData.length];
                System.arraycopy(mCaptureData.mVData, 0, vBytes, 0, mCaptureData.mVData.length);
            }
        }


        if (!mRunning) {
            return;
        }
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int ret = ImageConvert.getInstance().I420ToRgba(yBytes, uBytes, vBytes, width, height, bmp);
        if (ret != 0) {
            Log.e(TAG, "<onMsgDoDetect> fail to convert YUV to RGBA, ret=" + ret);
            detectTrigger();
            return;
        }

        if (!mRunning) {
            return;
        }
        String qrCode = ZXingUtils.parseQRCodeByBmp(bmp);
        if (qrCode != null) {
            Log.d(TAG, "<onMsgDoDetect> qrCode=" + qrCode);
            if (mScanCallback != null) {
                mScanCallback.onQRCodeParsed(qrCode);
            }
        }

        long t2 = System.currentTimeMillis();
        //Log.d(TAG, "<onMsgDoDetect> constTime=" + ((t2-t1) / 1000));
        detectTrigger();
    }






}
