package io.agora.fpa.example;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "fpa-MainActivity";

    private final ThreadPoolExecutor threadPoolExecutor =
            new ThreadPoolExecutor(4, 10, 20, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10));

    private final HandlerThread handlerThread = new HandlerThread("workThread");
    private Handler mH = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();

        handlerThread.start();
        mH = new Handler(handlerThread.getLooper());

        if (BuildConfig.is_agora_demo) {
            mH.post(mTmpFileGetRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mH = null;
        handlerThread.quitSafely();
    }

    private final View.OnClickListener viewClick = view -> {
        switch (view.getId()) {
            case R.id.httpDownload: {
                doHttpDownload();
                break;
            }

            case R.id.httpUpload: {
                doHttpUpload();
                break;
            }

            case R.id.transparent: {
                doTransparent();
                break;
            }
        }
    };

    private void initViews() {
        findViewById(R.id.httpDownload).setOnClickListener(viewClick);
        findViewById(R.id.httpUpload).setOnClickListener(viewClick);
        findViewById(R.id.transparent).setOnClickListener(viewClick);
    }


    private void doHttpDownload() {
        if (BuildConfig.is_agora_demo) {
            mH.post(mHttpDownload);
        }
    }

    private void doHttpUpload() {
        if (BuildConfig.is_agora_demo) {
            mH.post(mHttpUpload);
        }
    }

    private void doTransparent() {
        if (BuildConfig.is_agora_demo) {
            mH.post(mTransparent);
        }
    }

    private final Runnable mTransparent = () -> {
        AbstractFpaSettings settings = Application.getApp().getFpaSettings();
        if (null == settings) {
            Log.e(TAG, "null object of AbstractFpaSettings@upload");
            return;
        }

        String strDomainIp = settings.getRequestIp();
        String strDomainHost = settings.getRequestHost();

        threadPoolExecutor.execute(() -> transparentDoAction(204, strDomainIp, 30103, true));
        threadPoolExecutor.execute(() -> transparentDoAction(204, strDomainHost, 30103, false));
        threadPoolExecutor.execute(() -> transparentDoAction(254, strDomainIp, 30113, false));
        threadPoolExecutor.execute(() -> transparentDoAction(254, strDomainHost, 30113, true));
    };

    private void transparentDoAction(int chainId, String domain, int port, boolean fallback) {
        FpaLogicManager.Transparent transparent = new FpaLogicManager.Transparent(chainId, domain, port, fallback) {
            @Override
            public byte[] write() {
                String strRequest = "GET /1KB.txt? HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n";
                return strRequest.getBytes();
            }

            @Override
            public void read(byte[] data, int length) {
                if (null != data && length > 0) {
                    String strReadData = new String(data, 0, length);
                    Log.i(TAG, "transparent@domain data:\n\t" + strReadData);
                }
            }
        };

        transparent.action();
    }

    private final Runnable mHttpUpload = () -> {
        AbstractFpaSettings settings = Application.getApp().getFpaSettings();
        if (null == settings) {
            Log.e(TAG, "null object of AbstractFpaSettings@upload");
            return;
        }
        File file = new File(getCacheDir() + "/agora_simple_demo_tmp_file.txt");
        String strDomainIp = settings.getRequestIp();
        String strDomainHost = settings.getRequestHost();
        String[] urls = new String[]{
                "http://" + strDomainIp + ":30103/upload",
                "http://" + strDomainHost + ":30103/upload",
                "https://" + strDomainHost + ":30113/upload",
                "https://" + strDomainIp + ":30113/upload",
        };

        for (int i = 0; i < urls.length; i++) {
            int finalI = i;
            mH.postDelayed(() -> {
                Downloader downloader = new Downloader();
                downloader.httpUploadFile(true, file.getAbsolutePath(), urls[finalI], null, null);
            }, 600 * finalI);
        }
    };

    private final Runnable mHttpDownload = () -> {
        AbstractFpaSettings settings = Application.getApp().getFpaSettings();
        if (null == settings) {
            Log.e(TAG, "null object of AbstractFpaSettings@download");
            return;
        }

        long now = System.currentTimeMillis();
        String strBaseUrl = getCacheDir() + "/agora_http_download@" + now;
        String strDomainIp = settings.getRequestIp();
        String strDomainHost = settings.getRequestHost();

        String[] urls = new String[]{
                "http://" + strDomainIp + ":30103/100KB.txt",
                "http://" + strDomainHost + ":30103/100KB.txt",
                "https://" + strDomainHost + ":30113/100KB.txt",
                "https://" + strDomainIp + ":30113/100KB.txt",
        };

        for (int i = 0; i < urls.length; i++) {
            int finalI = i;
            mH.postDelayed(() -> {
                String saveFile = strBaseUrl + "_" + finalI + ".txt";
                Downloader downloader = new Downloader();
                downloader.httpDownloadFile(true, urls[finalI], null, saveFile);
            }, finalI * 500);
        }
    };


    private final Runnable mTmpFileGetRunnable = () -> {
        File file = new File(getCacheDir() + "/agora_simple_demo_tmp_file.txt");
        if (file.exists()) {
            return;
        }

        AbstractFpaSettings settings = Application.getApp().getFpaSettings();
        if (null == settings) {
            Log.e(TAG, "null object of AbstractFpaSettings");
            return;
        }


        String strUrl = "http://" + settings.getRequestHost() + ":30103/100KB.txt";
        Downloader downloader = new Downloader();
        downloader.httpDownloadFile(false, strUrl, null, file.getAbsolutePath());
    };


}
