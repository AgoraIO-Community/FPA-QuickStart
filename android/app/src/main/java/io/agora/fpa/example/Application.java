package io.agora.fpa.example;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import io.agora.fpa.example.bean.ChainInfo;
import io.agora.fpa.example.bean.HttpChainInfo;
import io.agora.fpa.example.bean.RootConfigBean;
import io.agora.fpa.proxy.FpaHttpProxyChainConfig;
import io.agora.fpa.proxy.FpaProxyService;

public class Application extends android.app.Application {
    private final static String TAG = "fpa-Application";

    private AbstractFpaSettings mFpaSettings = null;
    private static Application _instance = null;

    public static Application getApp() {
        return _instance;
    }

    @Nullable
    public AbstractFpaSettings getFpaSettings() {
        return mFpaSettings;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        _instance = this;

        if (BuildConfig.is_agora_demo) {
            makeFpaSettings();
        }

        if (null != mFpaSettings) {
            FpaLogicManager.getInstance().init(mFpaSettings);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        FpaProxyService.getInstance().stop();
    }

    private void makeFpaSettings() {
        InputStream is = null;
        try {
            is = getAssets().open("settings.json");
            if (null == is) {
                Log.e(TAG, "asset open file failed");
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int length = -1;
            byte[] readBuffer = new byte[4 * 1024];
            while ((length = is.read(readBuffer)) != -1) {
                baos.write(readBuffer, 0, length);
            }
            is.close();

            String json = baos.toString();
            baos.close();

            Gson gson = new Gson();
            RootConfigBean root = gson.fromJson(json, RootConfigBean.class);
            if (null == root) {
                return;
            }
            HttpChainInfo info = root.getHttpChainInfo();
            if (info == null || info.getChainInfo() == null || info.getChainInfo().isEmpty()) {
                return;
            }

            mFpaSettings = new AgoraDemoFpaSettings(this) {
                private FpaHttpProxyChainConfig.Builder mBuilder = null;

                @Override
                public FpaHttpProxyChainConfig.Builder getHttpConfig() {
                    if (mBuilder != null) {
                        return mBuilder;
                    }
                    mBuilder = new FpaHttpProxyChainConfig.Builder();
                    mBuilder.fallbackWhenNoChainAvailable(info.getFallback());
                    for (ChainInfo item : info.getChainInfo()) {
                        mBuilder.addChainInfo(item.getChainId(), item.getDomain(), item.getPort(), item.getFallback());
                    }
                    return mBuilder;
                }

                @Override
                public String getAppId() {
                    return root.getAppId();
                }

                @Override
                public String getRequestHost() {
                    return root.getRequestHost();
                }

                @Override
                public String getRequestIp() {
                    return root.getRequestIp();
                }
            };


        } catch (Exception e) {
            Log.e(TAG, "makeFpaSettings failed", e);
        }

    }
}

