package io.agora.fpa.example;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import io.agora.fpa.proxy.FpaHttpProxyChainConfig;

public class AgoraDemoFpaSettings extends AbstractFpaSettings {
    private String mLogPath = null;

    public AgoraDemoFpaSettings(Context context) {
        super(context);

        mLogPath = null;
        try {
            File file = new File(context.getCacheDir() + "/agora");
            file.mkdirs();

            file = new File(file.getAbsoluteFile() + "/fpa_log_sdk.log");
            if (!file.exists()) {
                boolean result = file.createNewFile();
                if (result) {
                    Log.w(TAG, "crated file: fpa_log_sdk.log");
                }
            }

            if (file.canWrite()) {
                Log.w(TAG, "file exist and can write");
                mLogPath = file.getAbsolutePath();
            } else if (file.canRead()) {
                Log.w(TAG, "file exist and can read");
            } else {
                Log.w(TAG, "file exist can't read or write");
            }
        } catch (IOException e) {
            Log.e(TAG, "FPA log file failed", e);
        }
    }

    @Override
    public String getAppId() {
        return APP_ID;
    }

    @Override
    public String getLogFile() {
        return mLogPath;
    }

    @Override
    public FpaHttpProxyChainConfig.Builder getHttpConfig() {
        return null;
    }
}

