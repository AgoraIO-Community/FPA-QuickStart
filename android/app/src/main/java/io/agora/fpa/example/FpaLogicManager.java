package io.agora.fpa.example;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import io.agora.fpa.proxy.FpaChainInfo;
import io.agora.fpa.proxy.FpaHttpProxyChainConfig;
import io.agora.fpa.proxy.FpaProxyConnectionInfo;
import io.agora.fpa.proxy.FpaProxyService;
import io.agora.fpa.proxy.FpaProxyServiceConfig;
import io.agora.fpa.proxy.FpaProxyServiceDiagnosisInfo;
import io.agora.fpa.proxy.IFpaServiceListener;
import okhttp3.OkHttpClient;

public final class FpaLogicManager {
    private static final String TAG = "fpa.logic_manager";

    public static final String LOCAL_HOST = "127.0.0.1";

    private static final class InstanceHolder {
        final static FpaLogicManager sInstance = new FpaLogicManager();

        private InstanceHolder() {
        }
    }

    public static FpaLogicManager getInstance() {
        return InstanceHolder.sInstance;
    }


    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    private AbstractFpaSettings mSettings = null;
    private FpaProxyService mFpa = null;
    private FpaHttpProxyChainConfig.Builder mHttpConfigBuilder = null;

    private FpaLogicManager() {
    }

    private boolean initImpl(AbstractFpaSettings settings) {
        if (null == settings) {
            throw new RuntimeException("null object, call init(AbstractFpaSettings settings) first");
        }

        if (AbstractFpaSettings.APP_ID.equals(settings.getAppId())) {
            throw new RuntimeException("must have a application id: Not " + AbstractFpaSettings.APP_ID);
        }

        if (AbstractFpaSettings.LOG_FILE.equals(settings.getLogFile())) {
            throw new RuntimeException("must have a log file: Not " + AbstractFpaSettings.LOG_FILE);
        }

        Log.e(TAG, "SDK version: " + FpaProxyService.getSdkVersion());
        Log.e(TAG, "SDK   build: " + FpaProxyService.getSdkBuildInfo());

        FpaProxyServiceConfig.Builder builder = new FpaProxyServiceConfig.Builder(settings.getLogFile());
        builder.setAppId(settings.getAppId()).setToken(settings.getAppId());
        builder.setLogFileSizeKb(settings.getFileSizeInKb()).setLogLevel(settings.getLogLevel());

        try {
            int result = FpaProxyService.getInstance().start(builder.build());
            if (0 == result) {
                FpaProxyService.getInstance().setListener(mListener);
                mFpa = FpaProxyService.getInstance();
                Log.e(TAG, "start FPA service success");
                return true;
            }


        } catch (Exception e) {
            Log.e(TAG, "start FPA service failed", e);
        }
        return false;
    }

    private boolean reInit() {
        if (mFpa == null) {
            return initImpl(mSettings);
        }
        return true;
    }


    public synchronized int getHttpPort() {
        if (!reInit()) {
            return -1;
        }

        String strSDKInfo = "\t\n" +
                "********************************************************************************\n" +
                "   SDK Version: " + FpaProxyService.getSdkVersion() + "\n" +
                "SDK build Info: " + FpaProxyService.getSdkBuildInfo() + "\n" +
                "********************************************************************************\n";
        Log.e(TAG, strSDKInfo);

        if (null == mHttpConfigBuilder) {
            if (null == mSettings) {
                throw new RuntimeException("null object when get http port, call init(AbstractFpaSettings settings) first");
            }
            mHttpConfigBuilder = mSettings.getHttpConfig();
            if (null == mHttpConfigBuilder) {
                throw new RuntimeException("must set http chain config");
            }
            FpaProxyService.getInstance().setOrUpdateHttpProxyChainConfig(mHttpConfigBuilder.build());
        }

        int port = FpaProxyService.getInstance().getHttpProxyPort();
        if (port <= 0) {
            Log.e(TAG, "http proxy generate a bad port: " + port);
        }
        return port;
    }

    public synchronized int getTransparentPort(int chainId, String strDomain, int port, boolean fallback) {
        if (!reInit()) {
            return -1;
        }

        FpaChainInfo info = new FpaChainInfo(chainId, strDomain, port, fallback);
        int port_ = FpaProxyService.getInstance().getTransparentProxyPort(info);
        if (port_ <= 0) {
            Log.e(TAG, "transparent proxy generate a bad port: " + port);
        }
        return port_;
    }

    public synchronized OkHttpClient createHttpClient(String url, @Nullable OkHttpClient.Builder builder) {
        if (TextUtils.isEmpty(url)) {
            Log.e(TAG, "Empty http request url");
            return null;
        }

        if (null == builder) {
            builder = new OkHttpClient.Builder();
            builder.readTimeout(50, TimeUnit.SECONDS).writeTimeout(50, TimeUnit.SECONDS);
            builder.hostnameVerifier((s, sslSession) -> {
                return true; // 不可取, demo 这样写而已
            });
        }
        int port = getHttpPort();
        if (port > 0) {
            builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(LOCAL_HOST, port)));
            Log.i(TAG, "use http proxy port= " + port);
        }

        return builder.build();
    }


    public synchronized boolean init(AbstractFpaSettings settings) {
        if (settings == null) {
            return false;
        }

        mSettings = settings;
        return initImpl(settings);
    }

    public synchronized void stop() {
        if (mFpa != null) {
            mFpa = null;
        }
        if (mHttpConfigBuilder != null) {
            mHttpConfigBuilder = null;
        }
        FpaProxyService.getInstance().stop();
    }


    private final IFpaServiceListener mListener = new IFpaServiceListener() {
        @Override
        public void onProxyEvent(int eventCode, @Nullable FpaProxyConnectionInfo info, int errorCode) {
            Log.e(TAG, "event=" + eventCode + " info: " + info + " error=" + errorCode);
        }
    };


    public static abstract class Transparent {
        private final int mChainId;
        private final String mDomain;
        private final int mPort;
        private final boolean mFallback;

        public Transparent(int chainId, String domain, int port, boolean fallback) {
            this.mChainId = chainId;
            this.mDomain = domain;
            this.mPort = port;
            this.mFallback = fallback;
        }

        public abstract byte[] write();

        public abstract void read(byte[] data, int length);

        public final boolean action() {
            FpaProxyServiceDiagnosisInfo info = FpaProxyService.getInstance().getDiagnosisInfo();
            String actionLog = "\n\t\n";
            actionLog += "++++++++++ transparent action ++++++++++\n";
            actionLog += "  install  id: " + info.installId + "\n";
            actionLog += "  instance id: " + info.instanceId + "\n";
            actionLog += "++++++++++++++++++++++++++++++++++++++++\n\t";
            Log.e(TAG, actionLog);

            getInstance().reInit();

            int port_ = getInstance().getTransparentPort(mChainId, mDomain, mPort, mFallback);
            if (port_ <= 0) {
                Log.e(TAG, "create transparent port failed");
                return false;
            }
            Log.w(TAG, "transparent port= " + port_);

            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(LOCAL_HOST, port_));

                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();

                if (null == out || null == in) {
                    Log.e(TAG, "bad socket, no read or write ");
                    return false;
                }

                byte[] writeData = write();
                if (null != writeData && writeData.length > 0) {
                    out.write(writeData);
                    out.flush();
                }
                Log.e(TAG, "TCP data write");

                BufferedInputStream bin = new BufferedInputStream(in);
                byte[] readBuffer = new byte[5 * 1024];
                int readLength = -1;
                while ((readLength = bin.read(readBuffer)) != -1) {
                    read(readBuffer, readLength);
                }
                // read action
                read(null, -1);
                bin.close();

                Log.e(TAG, "TCP data read");

                try {
                    in.close();
                } catch (IOException ignore) {
                }

                try {
                    out.close();
                } catch (IOException ignore) {
                }

                try {
                    socket.close();
                    socket.shutdownInput();
                    socket.shutdownOutput();
                    Log.e(TAG, "socket close and shutdown");
                } catch (IOException ignore) {
                }

            } catch (Exception e) {
                Log.e(TAG, "transparent action failed", e);
            }

            return true;
        }
    }


}
