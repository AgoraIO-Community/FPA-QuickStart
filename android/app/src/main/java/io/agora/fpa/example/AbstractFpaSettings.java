package io.agora.fpa.example;

import android.content.Context;

import io.agora.fpa.proxy.FpaHttpProxyChainConfig;
import io.agora.fpa.proxy.LogLevel;

public abstract class AbstractFpaSettings {
    final static String TAG = "fpa.settings";

    static final String APP_ID = "Your App ID";
    static final String LOG_FILE = "Your Log File";
    static final String NO_URL = "No Url";

    protected final Context mContext;

    public AbstractFpaSettings(Context context) {
        mContext = context.getApplicationContext();
    }

    public final Context getContext() {
        return mContext;
    }

    public abstract String getAppId();

    public abstract String getLogFile();

    public LogLevel getLogLevel() {
        // LOG_INFO    级别日志很多，可做前提调试
        // LOG_WARNING 建议
        return LogLevel.LOG_WARNING;
    }

    public int getFileSizeInKb() {
        return 1024;
    }

    public String getRequestHost() {
        return NO_URL;
    }

    public String getRequestIp() {
        return NO_URL;
    }

    public abstract FpaHttpProxyChainConfig.Builder getHttpConfig();
}
