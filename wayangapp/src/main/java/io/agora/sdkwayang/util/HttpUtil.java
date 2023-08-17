package io.agora.sdkwayang.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import io.agora.sdkwayang.logger.WLog;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpUtil {
    final static String TAG = "IOTWY/HttpUtil";

    private static final String PROTOCOL_HTTPS = "https://";
    private static final String BECKON_API = "api.beckon.cc";
    private static final int DEFAULT_TIMEOUT_MS = 5000;

    private final float DEFAULT_PRIORITY = Math.round(DEFAULT_TIMEOUT_MS / 1000f / 2);
    private final String[] newWebUrls = {
            PROTOCOL_HTTPS + BECKON_API,
            "https://221.228.78.124",
            "https://168.63.200.91",
            "https://54.183.49.216",
            "https://52.28.113.113"};

    private final ArrayList<IPDescData> serviceUriList = new ArrayList<>();
    private boolean ipListInitialized = false;
    private MediaType MEDIA_TYPE_ZIP = MediaType.parse("application/zip, application/octet-stream");
    private volatile OkHttpClient okHttpClient;

    public OkHttpClient getOkHttpClient(Context context) {
        if (okHttpClient == null) {
            synchronized (HttpUtil.class) {
                if (okHttpClient == null) {
                    SSLParams sslParams = getSslSocketFactory(context, null, 0, null);
                    okHttpClient = new OkHttpClient.Builder()
                            .connectTimeout(10, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager)
                            .hostnameVerifier(getHostnameVerifier())
                            .build();
                }
            }
        }
        return okHttpClient;
    }

    public static HttpUtil getInstance() {
        return HttpUtilHolder.INSTANCE;
    }

    private HttpUtil() {
    }

    private static class HttpUtilHolder {
        private static final HttpUtil INSTANCE = new HttpUtil();
    }

    public synchronized void parseIpFromHost(Boolean forceInitialize) {
        if (ipListInitialized && !forceInitialize) {
            return;
        }
        String expectedIp = null;
        try {
            InetAddress address = InetAddress.getByName(BECKON_API);
            if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                expectedIp = address.getHostAddress();
            }
        } catch (UnknownHostException e) {
            WLog.getInstance().e(TAG, "parse default service url failed \n" + Log.getStackTraceString(e));
        }

        serviceUriList.clear();

        if (expectedIp == null) {
            for (int i = 1; i < newWebUrls.length; i++) {
                String webUrl = newWebUrls[i];
                serviceUriList.add(new IPDescData(webUrl));
            }
        } else {
            serviceUriList.add(new IPDescData(PROTOCOL_HTTPS + expectedIp, (DEFAULT_PRIORITY - 2)));
            for (int i = 1; i < newWebUrls.length; i++) {
                String ip = newWebUrls[i];
                if (ip.endsWith(expectedIp)) {
                    continue;
                }
                serviceUriList.add(new IPDescData(newWebUrls[i]));
            }
        }
        ipListInitialized = true;
        WLog.getInstance().d(TAG, "parseIpFromHost $serviceUriList $forceInitialize");
    }

    synchronized String getServiceUrl(int idx) {
        if (serviceUriList.isEmpty()) {
            return newWebUrls[0];
        }
        return serviceUriList.get(idx).ip;
    }

    SSLParams getSslSocketFactory(Context context, @RawRes int[] certificatesId, @RawRes int bksFileId, String password) {
        SSLParams sslParams = new SSLParams();
        try {
            TrustManager[] trustManagers = prepareTrustManager(context, certificatesId);
            KeyManager[] keyManagers = prepareKeyManager(context, bksFileId, password);

            SSLContext sslContext;
            try {
                sslContext = SSLContext.getInstance("TLS");
            } catch (NoSuchAlgorithmException e) {
                WLog.getInstance().e(TAG, Log.getStackTraceString(e));
                return sslParams;
//                throw new ProtocolException(Log.getStackTraceString(e));
            }

            X509TrustManager[] x509TrustManager = new X509TrustManager[1];
            if (trustManagers != null) {
                x509TrustManager[0] = new MyTrustManager(chooseTrustManager(trustManagers));
            } else {
                x509TrustManager[0] = new UnSafeTrustManager();
            }
            sslContext.init(keyManagers, x509TrustManager, null);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                sslParams.sSLSocketFactory = new Tls12SocketFactory(sslContext.getSocketFactory());
                sslParams.trustManager = x509TrustManager[0];
                return sslParams;
            }

            sslParams.sSLSocketFactory = sslContext.getSocketFactory();
            sslParams.trustManager = x509TrustManager[0];
            return sslParams;
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        } catch (KeyManagementException e) {
            throw new AssertionError(e);
        } catch (KeyStoreException e) {
            throw new AssertionError(e);
        }
    }

    HostnameVerifier getHostnameVerifier() {
        return new HostnameVerifier() {

            @Override
            public boolean verify(String hostname, SSLSession session) {
                if (hostname == null)
                    return session.getPeerHost() == null;

                return hostname.equalsIgnoreCase(session.getPeerHost());
            }
        };
    }

    private TrustManager[] prepareTrustManager(Context context, int[] certificatesId) {
        if (certificatesId == null || certificatesId.length == 0) {
            return null;
        }

        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);

            for (int index = 0; index < certificatesId.length; index++) {
                int certificateId = certificatesId[index];
                InputStream cerInputStream = context.getResources().openRawResource(certificateId);
                String certificateAlias = Integer.toString(index);

                Certificate certificate = certificateFactory.generateCertificate(cerInputStream);
                keyStore.setCertificateEntry(certificateAlias, certificate);

                try {
                    cerInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            return trustManagerFactory.getTrustManagers();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private KeyManager[] prepareKeyManager(Context context, @RawRes int bksFileId, String password) {
        if (bksFileId == 0 || password == null)
            return null;

        try {
            KeyStore clientKeyStore = KeyStore.getInstance("BKS");
            clientKeyStore.load(context.getResources().openRawResource(bksFileId), password.toCharArray());
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(clientKeyStore, password.toCharArray());
            return keyManagerFactory.getKeyManagers();
        } catch (KeyStoreException e) {
            WLog.getInstance().e(TAG, Log.getStackTraceString(e));
        } catch (NoSuchAlgorithmException e) {
            WLog.getInstance().e(TAG, Log.getStackTraceString(e));
        } catch (UnrecoverableKeyException e) {
            WLog.getInstance().e(TAG, Log.getStackTraceString(e));
        } catch (CertificateException e) {
            WLog.getInstance().e(TAG, Log.getStackTraceString(e));
        } catch (IOException e) {
            WLog.getInstance().e(TAG, Log.getStackTraceString(e));
        }

        return null;
    }

    private X509TrustManager chooseTrustManager(TrustManager[] trustManagers) {
        for (TrustManager trustManager : trustManagers) {
            if (trustManager instanceof X509TrustManager) {
                return (X509TrustManager) trustManager;
            }
        }
        return null;
    }

    public HashMap<String, String> buildUploadExtraValues(int uid, int network, int signalLevel) {
        HashMap<String, String> values = new HashMap<>();
        values.put(Protocols.UID, String.valueOf(uid & 0xFFFFFFFFL));
        values.put(Protocols.APP_ID, "io.agora.falcondemo");
        values.put(Protocols.APP_VERSION, "2.0.0.2");
        values.put(Protocols.OS_VERSION, android.os.Build.VERSION.RELEASE);
        values.put(Protocols.NETWORK, String.valueOf(network));
        values.put(Protocols.SIGNAL_LEVEL, String.valueOf(signalLevel));
        values.put(Protocols.PLATFORM, String.valueOf(Protocols.PLATFORM_Android));
        return values;
    }

    public boolean feedback(Context context, String comment, HashMap<String, String> values) {
        OkHttpClient client = getOkHttpClient(context);

        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart(Protocols.TEXT, comment);

        if (values != null && values.size() > 0) {
            Set<Map.Entry<String, String>> valuesEntrys = values.entrySet();
            for (Map.Entry<String, String> entry : valuesEntrys) {
                builder.addFormDataPart(entry.getKey(), entry.getValue());
            }
        }

        MultipartBody requestBody = builder.build();

        boolean retry;
        int targetHostIdx = 0;
        String targetUrl;
        do {
            retry = false;
            targetUrl = getServiceUrl(targetHostIdx);
            String url = targetUrl + "/feedback/uploadFeedback";

            Request request = new Request.Builder().url(url)
                    .post(requestBody).build();

            Response response = null;
            try {
                response = client.newCall(request).execute();
            } catch (IOException e) {
                WLog.getInstance().w(TAG, Log.getStackTraceString(e));
            }

            if (response == null || !response.isSuccessful()) {
                targetHostIdx++;
                if (targetHostIdx < serviceUriList.size()) {
                    retry = true;
                }
            } else {
                return true;
            }
        } while (retry);

        return false;
    }

    boolean uploadZipFile(Context context, File file, String fileName, HashMap<String, String> values) {
        SSLParams sslParams = getSslSocketFactory(context, null, 0, null);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(2, TimeUnit.MINUTES)
                .sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager)
                .hostnameVerifier(getHostnameVerifier())
                .build();

        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart(Protocols.FILE, fileName, RequestBody.create(MEDIA_TYPE_ZIP, file));

        if (values != null && values.size() > 0) {
            Set<Map.Entry<String, String>> valuesEntrys = values.entrySet();
            for (Map.Entry<String, String> entry : valuesEntrys) {
                builder.addFormDataPart(entry.getKey(), entry.getValue());
            }
        }

        MultipartBody requestBody = builder.build();

        boolean retry;
        int targetHostIdx = 0;
        String targetUrl;
        do {
            retry = false;
            targetUrl = getServiceUrl(targetHostIdx);
            String url = targetUrl + "/feedback/uploadFeedback";

            Request request = new Request.Builder().url(url)
                    .post(requestBody).build();

            Response response = null;
            try {
                response = client.newCall(request).execute();
            } catch (IOException e) {
                WLog.getInstance().e(TAG, Log.getStackTraceString(e));
            }

            if (response == null || !response.isSuccessful()) {
                targetHostIdx++;
                if (targetHostIdx < serviceUriList.size()) {
                    retry = true;
                }
            } else {
                return true;
            }
        } while (retry);

        return false;
    }

    public boolean compressPCMsAndUpload(Context context, HashMap<String, String> values, String file) {
        boolean succeed = false;

        String targetPCMFile;
        if (file == null) {
            targetPCMFile = FileUtil.compressPCMs(context);
        } else {
            targetPCMFile = file;
        }

        WLog.getInstance().d(TAG,"compressPCMsAndUpload $file $targetPCMFile");
        if (targetPCMFile != null) {
            File zip = new File(targetPCMFile);
            if (AppUtil.isWifiConnected(context) || zip.length() < 20 * 1024 * 1024) { // double check if it is wifi
                succeed = uploadZipFile(context, zip, targetPCMFile, values);
            }
            zip.delete();
        }
        return succeed;
    }

    public boolean compressLogsAndUpload(Context context, HashMap<String, String> values, String file) {
        boolean succeed = false;

        String targetLogFile;
        if (file == null) {
            targetLogFile = FileUtil.compressLogs(context);
        } else {
            targetLogFile = file;
        }

        WLog.getInstance().d(TAG,"compressLogsAndUpload $file $targetLogFile");
        if (targetLogFile != null) {
            File zip = new File(targetLogFile);
            if (AppUtil.isWifiConnected(context) || zip.length() < 20 * 1024 * 1024) { // double check if it is wifi
                succeed = uploadZipFile(context, zip, targetLogFile, values);
            }
            zip.delete();
        }
        return succeed;
    }

    interface Protocols {
        String UID = "uid";
        String APP_ID = "appName";
        String APP_VERSION = "appVersion";
        String OS_VERSION = "osVersion";
        String NETWORK = "network";
        String SIGNAL_LEVEL = "signalLevel";
        String PLATFORM = "platform";
        String FILE = "log";
        String FILE_IMAGE = "image";
        String TEXT = "text";

        int PLATFORM_Android = 1;
        int PLATFORM_iOS = 2;
    }

    private class UnSafeTrustManager implements X509TrustManager {

        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    private class MyTrustManager implements X509TrustManager {/*@Throws(NoSuchAlgorithmException::class, KeyStoreException::class)*/
        private X509TrustManager defaultTrustManager;
        private X509TrustManager localTrustManager;

        public MyTrustManager(X509TrustManager localTrustManager) throws KeyStoreException, NoSuchAlgorithmException {
            this.localTrustManager = localTrustManager;
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            defaultTrustManager = chooseTrustManager(trustManagerFactory.getTrustManagers());
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                defaultTrustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException ce) {
                localTrustManager.checkServerTrusted(chain, authType);
            }

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    /**
     * Enables TLS v1.2 when creating SSLSockets.
     * <p>
     * <p>
     * For some reason, android supports TLS v1.2 from API 16, but enables it by
     * default only from API 20.
     *
     * @link https://developer.android.com/reference/javax/net/ssl/SSLSocket.html
     * *
     * @see SSLSocketFactory
     */
    private class Tls12SocketFactory extends SSLSocketFactory {

        private final String[] TLS_SUPPORT_VERSION = {"TLSv1.1", "TLSv1.2"};

        private SSLSocketFactory delegate;

        public Tls12SocketFactory(SSLSocketFactory delegate) {
            super();
            this.delegate = delegate;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            return patch(delegate.createSocket(s, host, port, autoClose));
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
            return patch(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
            return patch(delegate.createSocket(host, port, localHost, localPort));
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return patch(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            return patch(delegate.createSocket(address, port, localAddress, localPort));
        }

        private Socket patch(Socket s) {
            if (s instanceof SSLSocket) {
                ((SSLSocket) s).setEnabledProtocols(TLS_SUPPORT_VERSION);
            }
            return s;
        }

    }

    class SSLParams {
        SSLSocketFactory sSLSocketFactory;
        X509TrustManager trustManager;

        @Override
        public String toString() {
            return "{sSLSocketFactory=$sSLSocketFactory, trustManager=$trustManager}";
        }
    }

    private class IPDescData implements Comparable<IPDescData> {

        String ip;
        float priority;

        public IPDescData(String ip, float priority) {
            this.ip = ip;
            this.priority = priority;
        }

        public IPDescData(String ip) {
            this.ip = ip;
            this.priority = DEFAULT_PRIORITY;
        }

        @Override
        public int compareTo(@NonNull IPDescData other) {
            return Float.compare(this.priority, other.priority);
        }
    }


    //downLoad
    public void downloadFile(final String url, final String saveDir, final OnDownloadListener listener) {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder().url(url).build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                listener.onDownloadFailed();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    listener.onDownloadFailed();
                    return;
                }
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fos = null;
                String savePath = isExistDir(saveDir);
                try {
                    is = response.body().byteStream();
                    long total = response.body().contentLength();
                    File file = new File(savePath, getNameFromUrl(url));
                    if (file.exists()) {
                        file.delete();
                    }
                    fos = new FileOutputStream(file);
                    long sum = 0;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        sum += len;
                        int progress = (int) (sum * 1.0f / total * 100);
                        listener.onDownloading(progress);
                    }
                    fos.flush();
                    listener.onDownloadSuccess(savePath, getNameFromUrl(url));
                } catch (Exception e) {
                    listener.onDownloadFailed();
                } finally {
                    try {
                        if (is != null)
                            is.close();
                    } catch (IOException e) {
                    }
                    try {
                        if (fos != null)
                            fos.close();
                    } catch (IOException e) {
                    }
                }
            }
        });
    }

    private String isExistDir(String saveDir) throws IOException {
        File downloadFile = new File(Environment.getExternalStorageDirectory(), saveDir);
        if (!downloadFile.mkdirs()) {
            downloadFile.createNewFile();
        }
        String savePath = downloadFile.getAbsolutePath();
        return savePath;
    }

    @NonNull
    private String getNameFromUrl(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }

    public interface OnDownloadListener {
        void onDownloadSuccess(String savePath, String fileName);

        void onDownloading(int progress);

        void onDownloadFailed();
    }
    //end downLoad

}
