package io.agora.iotlink.transport;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.android.service.MqttLog;
import org.eclipse.paho.android.service.MqttTraceHandler;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.R;
import io.agora.iotlink.base.BaseEvent;
import io.agora.iotlink.logger.ALog;


public class MqttSdkClient {

    /**
     * @brief MQTT回调接口
     */
    public static interface ICallback {
        /**
         * @brief 连接服务器完成事件
         * @param errCode : 错误码
         */
        default void onMqttConnectDone(int errCode) {}

        /**
         * @brief 重连服务器完成事件
         * @param errCode : 错误码
         */
        default void onMqttReconnectDone(int errCode) {}

        /**
         * @brief 订阅完成事件
         * @param errCode : 错误码
         */
        default void onMqttSubscribeDone(int errCode) {}

        /**
         * @brief 消息到来事件
         * @param transPacket : 消息数据
         */
        default void onMqttMsgReceived(final TransPacket transPacket) {}

        /**
         * @brief MQTT链接断开
         * @param cause : 断开原因
         */
        default void onMqttConnectionLost(final String cause) {}
    }

    /**
     * @brief MQTT 初始化参数
     */
    public static class InitParam {
        public Context mContext;
        public ICallback mCallback;
        public String mServerUrl;                   ///< 服务器地址
        public String mUserName;
        public String mPassword;
        public String mClientId;
        public boolean mHasCaCertify;

        @Override
        public String toString() {
            String infoText = "{ mServerUrl=" + mServerUrl
                    + ", mUserName=" + mUserName
                    + ", mPassword=" + mPassword
                    + ", mClientId=" + mClientId
                    + ", mHasCaCertify=" + mHasCaCertify + " }";
            return infoText;
        }
    }


    ////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////
    ////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/MQTTTSDK";
    protected static final long EVENT_WAIT_TIMEOUT = 5000;    ///< 事件等待5秒
    protected static final long DISCONNECT_TIMEOUT = 2000;


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private final Object mDataLock = new Object();
    private final BaseEvent mUnsubsribeEvent = new BaseEvent();
    private final BaseEvent mDisonnectEvent = new BaseEvent();

    private InitParam mInitParam;
    private MqttAndroidClient mMqttClient = null;
    private IMqttToken mMqttToken = null;
    private volatile boolean mSubscribed = false;           ///< 主题是否订阅成功了




    ////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////// Methods of Public /////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    /**
     * @brief 初始化MQTT，连接到服务器
     * @param initParam : 初始化参数
     * @return 返回错误码
     */
    public int initialize(final InitParam initParam) {
        if ((initParam.mCallback == null) || TextUtils.isEmpty(initParam.mServerUrl)) {
            ALog.getInstance().d(TAG, "<initialize> invalid parameter!");
            return ErrCode.XERR_INVALID_PARAM;
        }

        mInitParam = initParam;
        mMqttClient = new MqttAndroidClient(mInitParam.mContext, mInitParam.mServerUrl, mInitParam.mClientId);
        mMqttClient.setMqttLogger(new MqttLog.IMqttLogger() {
            @Override
            public void onMqttLogger(String tag, String content) {
                ALog.getInstance().d(tag, content);
            }
        });

        mMqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                ALog.getInstance().d(TAG, "<initialize.connectionLost> cause=" + cause);
                setSubscribed(false);       // 断开连接后，一定要重新订阅主题
                if (cause != null) {
                    mInitParam.mCallback.onMqttConnectionLost(cause.toString());
                } else {
                    mInitParam.mCallback.onMqttConnectionLost("None");
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                ALog.getInstance().d(TAG, "<initialize.messageArrived> topic=" + topic
                        + ", message=" + message.toString());

                TransPacket transPacket = new TransPacket();
                transPacket.mTopic = topic;
                transPacket.mMessageId = message.getId();
                transPacket.mContent = message.toString();
                mInitParam.mCallback.onMqttMsgReceived(transPacket);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                try {
                    MqttMessage mqttMessage = token.getMessage();
                    ALog.getInstance().d(TAG, "<initialize.deliveryComplete> message=" + mqttMessage.toString());

                } catch (MqttException mqttExp) {
                    mqttExp.printStackTrace();
                    ALog.getInstance().e(TAG, "<initialize.deliveryComplete> exp=" + mqttExp.toString());
                }
            }
        });

        mMqttClient.setTraceEnabled(true);
        mMqttClient.setTraceCallback(new MqttTraceHandler() {
            @Override
            public void traceDebug(String tag, String message) {
                ALog.getInstance().d(TAG, "[" + tag + "] " + message);
            }

            @Override
            public void traceError(String tag, String message) {
                ALog.getInstance().e(TAG, "[" + tag + "] " + message);
            }

            @Override
            public void traceException(String tag, String message, Exception e) {
                ALog.getInstance().e(TAG, "[" + tag + "] " + message);
            }
        });



        char[] pswd_bytes = mInitParam.mPassword.toCharArray();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(mInitParam.mUserName);
        options.setPassword(pswd_bytes);
        options.setAutomaticReconnect(false);        // 不进行自动重连
        options.setCleanSession(true);              // 配置每次连接都是新的，不需要旧数据

        if (mInitParam.mHasCaCertify) { // 设置 CA证书 对应的socketFactory
            options.setSocketFactory(getSslSocketFactory(mInitParam.mContext));
        }


        try {
            mMqttToken = mMqttClient.connect(options, mInitParam.mContext, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    ALog.getInstance().d(TAG, "<initialize.connect.onSuccess>");
                    mInitParam.mCallback.onMqttConnectDone(ErrCode.XOK);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    int reasonCode = asyncActionToken.getException().getReasonCode();
                    ALog.getInstance().d(TAG, "<initialize.connect.onFailure> reasonCode=" + reasonCode
                            + ", exception=" + exception.toString());

                    int errCode = ErrCode.XERR_NETWORK;
                    if (reasonCode == MqttException.REASON_CODE_NOT_AUTHORIZED) {
                        errCode = ErrCode.XERR_NOT_AUTHORIZED;
                    }
                    mInitParam.mCallback.onMqttConnectDone(errCode);
                }
            });

        } catch (MqttException mqttExp) {
            mqttExp.printStackTrace();
            ALog.getInstance().d(TAG, "<initialize.connect> connect, exp=" + mqttExp.toString());
            return ErrCode.XERR_SERVICE;
        }

        ALog.getInstance().d(TAG, "<initialize> done, mInitParam=" + mInitParam.toString());
        return ErrCode.XOK;
    }


    /**
     * @brief 同步释放MQTT客户端，阻塞等待
     * @return 返回错误码
     */
    public int release() {
        if (mMqttClient == null) {
            return ErrCode.XOK;
        }
        ALog.getInstance().d(TAG, "<release> BEGIN");

        try {
            ALog.getInstance().d(TAG, "<release> disconnecting...");
            mMqttClient.disconnect(DISCONNECT_TIMEOUT);

        } catch (MqttException mqttExp) {
            mqttExp.printStackTrace();
            ALog.getInstance().e(TAG, "<release> disconnect EXCEPTION, exp=" + mqttExp.toString());
        }

        // 关闭客户端并且完全释放
        ALog.getInstance().d(TAG, "<release> closing...");
        mMqttClient.close();
        mMqttClient = null;
        setSubscribed(false);

        int errCode = ErrCode.XOK;
        ALog.getInstance().d(TAG, "<release> END, errCode=" + errCode);
        return errCode;
    }

    /**
     * @brief 重连操作
     * @return 返回错误码
     */
    public int reconnect() {
        if (mMqttClient == null) {
            ALog.getInstance().d(TAG, "<reconnect> bad state!");
            return ErrCode.XERR_BAD_STATE;
        }

        char[] pswd_bytes = mInitParam.mPassword.toCharArray();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(mInitParam.mUserName);
        options.setPassword(pswd_bytes);
        options.setAutomaticReconnect(false);        // 不进行自动重连
        options.setCleanSession(true);              // 配置每次连接都是新的，不需要旧数据

        if (mInitParam.mHasCaCertify) { // 设置 CA证书 对应的socketFactory
            options.setSocketFactory(getSslSocketFactory(mInitParam.mContext));
        }

        try {
            mMqttToken = mMqttClient.connect(options, mInitParam.mContext, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    ALog.getInstance().d(TAG, "<reconnect.connect.onSuccess>");
                    mInitParam.mCallback.onMqttReconnectDone(ErrCode.XOK);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    int reasonCode = asyncActionToken.getException().getReasonCode();
                    ALog.getInstance().d(TAG, "<reconnect.connect.onFailure> reasonCode=" + reasonCode
                            + ", exception=" + exception.toString());

                    int errCode = ErrCode.XERR_NETWORK;
                    if (reasonCode == MqttException.REASON_CODE_NOT_AUTHORIZED) {
                        errCode = ErrCode.XERR_NOT_AUTHORIZED;
                    }

                    mInitParam.mCallback.onMqttReconnectDone(errCode);
                }
            });

        } catch (MqttException mqttExp) {
            mqttExp.printStackTrace();
            ALog.getInstance().d(TAG, "<reconnect.connect> connect, exp=" + mqttExp.toString());
            return ErrCode.XERR_SERVICE;
        }

        ALog.getInstance().d(TAG, "<reconnect> done, mInitParam=" + mInitParam.toString());
        return ErrCode.XOK;
    }



    /**
     * @brief MQTT订阅消息
     */
    public int subscribe(final String[] topicArray, final int[] qosArray) {
        if (mMqttClient == null) {
            ALog.getInstance().e(TAG, "<subscribe> bad state, mqtt already released");
            return ErrCode.XERR_BAD_STATE;
        }
        if (!isConnected()) {
            ALog.getInstance().e(TAG, "<subscribe> bad state, mqtt disconnected");
            return ErrCode.XERR_BAD_STATE;
        }

        try {
            mMqttClient.subscribe(topicArray, qosArray, mInitParam.mContext, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    ALog.getInstance().d(TAG, "<subscribe.onSuccess> ");
                    setSubscribed(true);
                    mInitParam.mCallback.onMqttSubscribeDone(ErrCode.XOK);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    ALog.getInstance().e(TAG, "<subscribe.onFailure> exception=" + exception.toString());
                    setSubscribed(false);
                    mInitParam.mCallback.onMqttSubscribeDone(ErrCode.XERR_NETWORK);
                }
            });


        } catch (MqttException mqttExp) {
            mqttExp.printStackTrace();
            ALog.getInstance().e(TAG, "<subscribe> exp=" + mqttExp.toString());
            return ErrCode.XERR_SERVICE;
        }

        ALog.getInstance().d(TAG, "<subscribe> done");
        return ErrCode.XOK;
    }

    /**
     * @brief 同步取消MQTT的消息订阅，阻塞等待
     */
    public int unsubscribe(final String[] topicArray) {
        if (mMqttClient == null) {
            ALog.getInstance().e(TAG, "<unsubscribe> bad state, mqtt already released");
            return ErrCode.XERR_BAD_STATE;
        }
        if (!isConnected()) {
            ALog.getInstance().e(TAG, "<unsubscribe> bad state, mqtt disconnected");
            return ErrCode.XERR_BAD_STATE;
        }

        try {
            mMqttClient.unsubscribe(topicArray, mInitParam.mContext, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    ALog.getInstance().d(TAG, "<unsubscribe.onSuccess> ");
                    setSubscribed(false);
                    mUnsubsribeEvent.setEvent(ErrCode.XOK);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    ALog.getInstance().e(TAG, "<unsubscribe.onFailure> exception=" + exception);
                    mUnsubsribeEvent.setEvent(ErrCode.XERR_NETWORK);
                }
            });

            mUnsubsribeEvent.waitEvent(EVENT_WAIT_TIMEOUT);

        } catch (MqttException mqttExp) {
            mqttExp.printStackTrace();
            ALog.getInstance().e(TAG, "<unsubscribe> [EXCEPTION] exp=" + mqttExp.toString());
            return ErrCode.XERR_SERVICE;
        }

        int errCode = mUnsubsribeEvent.getAttachValue();
        ALog.getInstance().d(TAG, "<unsubscribe> done, errCode=" + errCode);
        return errCode;
    }


    /**
     * @brief MQTT发送数据包
     * @param sendingPkt : 要发送的数据包
     */
    public int sendPacket(final TransPacket sendingPkt) {
        if (mMqttClient == null) {
            ALog.getInstance().e(TAG, "<sendPacket> bad state, mqtt already released");
            return ErrCode.XERR_BAD_STATE;
        }
        if (!isConnected()) {
            ALog.getInstance().e(TAG, "<sendPacket> bad state, mqtt disconnected");
            return ErrCode.XERR_BAD_STATE;
        }

        try {
            MqttMessage mqttMessage = new MqttMessage(sendingPkt.mContent.getBytes(StandardCharsets.UTF_8));
            mMqttClient.publish(sendingPkt.mTopic, mqttMessage, mInitParam.mContext, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    ALog.getInstance().d(TAG, "<sendPacket.onSuccess> ");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    ALog.getInstance().e(TAG, "<sendPacket.onFailure> exception=" + exception);
                }
            });

        } catch (MqttException mqttExp) {
            mqttExp.printStackTrace();
            ALog.getInstance().e(TAG, "<sendPacket> [EXCEPTION] exp=" + mqttExp.toString());
            return ErrCode.XERR_SERVICE;
        }

        ALog.getInstance().d(TAG, "<sendPacket> done, topic=" + sendingPkt.mTopic
                    + ", content=" + sendingPkt.mContent);
        return ErrCode.XOK;
    }

    /**
     * @brief 返回当前是否已经连接
     */
    public boolean isConnected() {
        if (mMqttClient == null) {
            return false;
        }

        boolean connected = mMqttClient.isConnected();
        return connected;
    }

    /**
     * @brief 设置主题是否已经订阅
     */
    void setSubscribed(boolean subed) {
        synchronized (mDataLock) {
            mSubscribed = subed;
        }
    }

    /**
     * @brief 返回主题是否已经订阅
     */
    boolean isSubscribed() {
        synchronized (mDataLock) {
            return mSubscribed;
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////// Methods of Certification ///////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    /**
     * @brief 根据 CA证书 获取相应的 SSL的 socketFactory
     */
    SSLSocketFactory getSslSocketFactory(final Context ctx) {
        try {
            // 打开 CA 资源文件
            InputStream caCrtFileInputStream = ctx.getResources().openRawResource(R.raw.mqttca);

            //Security.addProvider(new BouncyCastleProvider());
            X509Certificate caCert = null;
            BufferedInputStream bis = new BufferedInputStream(caCrtFileInputStream);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            while (bis.available() > 0) {
                caCert = (X509Certificate) cf.generateCertificate(bis);
            }

            KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
            caKs.load(null, null);
            caKs.setCertificateEntry("cert-certificate", caCert);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(caKs);

            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, tmf.getTrustManagers(), null);

            // 关闭 CA资源文件
            caCrtFileInputStream.close();

            // 获取要输出的 SSL的 socketFactory
            SSLSocketFactory socketFactory = sslContext.getSocketFactory();

            ALog.getInstance().d(TAG, "<getSslSocketFactory> done, socketFactory=" + socketFactory);
            return socketFactory;

        } catch (Resources.NotFoundException notFoundExp) {
            notFoundExp.printStackTrace();
            ALog.getInstance().e(TAG, "<getSslSocketFactory> [EXCEPTION] notFoundExp=" + notFoundExp);
            return null;

        } catch (CertificateException certificateExp) {
            certificateExp.printStackTrace();
            ALog.getInstance().e(TAG, "<getSslSocketFactory> [EXCEPTION] certificateExp=" + certificateExp);
            return null;

        } catch (IOException ioExp) {
            ioExp.printStackTrace();
            ALog.getInstance().e(TAG, "<getSslSocketFactory> [EXCEPTION] ioExp=" + ioExp);
            return null;

        } catch (KeyStoreException keyStoreExp) {
            keyStoreExp.printStackTrace();
            ALog.getInstance().e(TAG, "<getSslSocketFactory> [EXCEPTION] keyStoreExp=" + keyStoreExp);
            return null;

        } catch (NoSuchAlgorithmException noAlgorithmExp) {
            noAlgorithmExp.printStackTrace();
            ALog.getInstance().e(TAG, "<getSslSocketFactory> [EXCEPTION] noAlgorithmExp=" + noAlgorithmExp);
            return null;

        } catch (KeyManagementException keyMgrExp) {
            keyMgrExp.printStackTrace();
            ALog.getInstance().e(TAG, "<getSslSocketFactory> [EXCEPTION] keyMgrExp=" + keyMgrExp);
            return null;

        } catch (Exception exp) {
            exp.printStackTrace();
            ALog.getInstance().e(TAG, "<getSslSocketFactory> [EXCEPTION] exp=" + exp);
            return null;
        }

    }
}