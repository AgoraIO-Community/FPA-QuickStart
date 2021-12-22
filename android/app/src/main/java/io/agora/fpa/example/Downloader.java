package io.agora.fpa.example;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class Downloader {
    private final static String TAG = "fpa-Downloader";

    private IHttpListener mHttpDownloadListener = null;
    private IHttpListener mHttpUploadListener = null;

    public synchronized void setHttpDownloadListener(IHttpListener listener) {
        mHttpDownloadListener = listener;
    }

    public synchronized void setHttpUploadListener(IHttpListener listener) {
        mHttpUploadListener = listener;
    }


    private final static int LISTENER_DOWNLOAD = 1;
    private final static int LISTENER_UPLOAD = 2;

    private static class HttpBasicListener implements IHttpListener {
        private final Downloader outer;
        private final int mode;

        HttpBasicListener(Downloader outer, int mode) {
            this.outer = outer;
            this.mode = mode;
        }

        private IHttpListener getListener() {
            IHttpListener basic = null;
            if (mode == LISTENER_DOWNLOAD) {
                basic = outer.mHttpDownloadListener;
            } else if (mode == LISTENER_UPLOAD) {
                basic = outer.mHttpUploadListener;
            }
            return basic;
        }


        @Override
        public void onSuccess(String url, long times) {
            IHttpListener basic = getListener();
            if (null != basic) {
                basic.onSuccess(url, times);
            }
        }

        @Override
        public void onFailed(String url) {
            IHttpListener basic = getListener();
            if (null != basic) {
                basic.onFailed(url);
            }
        }

        @Override
        public void onProgress(String url, int process) {
            IHttpListener basic = getListener();
            if (null != basic) {
                basic.onProgress(url, process);
            }
        }
    }


    private final IHttpListener httpDownloadListener = new HttpBasicListener(this, LISTENER_DOWNLOAD);
    private final IHttpListener httpUploadListener = new HttpBasicListener(this, LISTENER_UPLOAD);


    public void httpUploadFile(boolean useProxy, String file, String url, Map<String, String> headers, RequestBody body) {
        if (TextUtils.isEmpty(file)) {
            Log.e(TAG, "no src file");
            return;
        }

        if (TextUtils.isEmpty(url)) {
            Log.e(TAG, "no upload service url");
            return;
        }

        OkHttpClient client = null;
        if (useProxy) {
            client = FpaLogicManager.getInstance().createHttpClient(url, null);
            if (null == client) {
                Log.e(TAG, "use FPA create OkHttpClient failed, will use normal");
            }
        }

        if (null == client) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.readTimeout(50, TimeUnit.SECONDS).writeTimeout(50, TimeUnit.SECONDS);
            client = builder.build();
        }

        Request.Builder builder = new Request.Builder();
        builder.url(url);
        if (null != headers) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        if (body == null) {
            File uploadFile = new File(file);
            body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", uploadFile.getName(), new RequestBody() {
                        @Nullable
                        @Override
                        public MediaType contentType() {
                            return MediaType.parse("multipart/form-data");
                        }

                        @Override
                        public long contentLength() throws IOException {
                            File myFile = new File(file);
                            return myFile.length();
                        }

                        @Override
                        public void writeTo(BufferedSink sink) throws IOException {
                            Source source;
                            try {
                                source = Okio.source(new File(file));
                                Buffer buf = new Buffer();
                                long remaining = contentLength();
                                long sum = 0;
                                int prevProgress = 0;
                                for (long readCount; (readCount = source.read(buf, 65535)) != -1; ) {
                                    sink.write(buf, readCount);
                                    sum += readCount;
                                    int progress = (int) (sum * 1.0f / remaining * 100);
                                    if ((progress - prevProgress) >= 1) {
                                        httpUploadListener.onProgress(null, progress);
                                        prevProgress = progress;
                                    }
                                }
                                Log.e(TAG, "upload file success sum:" + sum);
                            } catch (Exception e) {
                                Log.e(TAG, "some error happen when upload", e);
                            }
                        }
                    }).build();
        }

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        try {
            long startTime = System.currentTimeMillis();
            Response response = client.newCall(request).execute();
            long takeTime = System.currentTimeMillis() - startTime;
            if (null == response || !response.isSuccessful()) {
                httpUploadListener.onFailed(url);
                Log.e(TAG, "upload response failed, take " + takeTime + "ms");
            } else {
                httpUploadListener.onSuccess(url, takeTime);
                Log.e(TAG, "upload success take time= " + takeTime + "ms, code=" + response.code() + " body=" + (response.body() != null ? response.body().toString() : ""));
            }
        } catch (Exception ioe) {
            httpUploadListener.onFailed(url);
            Log.e(TAG, "upload failed", ioe);
        }
    }


    public void httpDownloadFile(boolean useProxy, String url, Map<String, String> headers, String saveDir) {
        if (TextUtils.isEmpty(url)) {
            Log.e(TAG, "bad http download url");
            return;
        }

        OkHttpClient client = null;
        if (useProxy) {
            client = FpaLogicManager.getInstance().createHttpClient(url, null);
            if (null == client) {
                Log.e(TAG, "use FPA create OkHttpClient failed, will use normal");
            }
        }

        if (null == client) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.readTimeout(50, TimeUnit.SECONDS).writeTimeout(50, TimeUnit.SECONDS);
            builder.hostnameVerifier((s, sslSession) -> {
                return true; // 不可取, demo 这样写而已
            });
            client = builder.build();
        }

        Request.Builder builder = new Request.Builder();
        builder.url(url);
        if (null != headers) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        Log.e(TAG, "start http download now: " + System.currentTimeMillis());
        client.newCall(builder.build()).enqueue(new HttpCallback(saveDir, httpDownloadListener));
    }


    private static class HttpCallback implements Callback {
        private final IHttpListener listener;
        private final String dir;
        private final long mStartTime;

        HttpCallback(String dir, IHttpListener listener) {
            this.dir = dir;
            this.listener = listener;
            mStartTime = System.currentTimeMillis();
        }

        @Override
        public void onFailure(Call call, IOException e) {
            if (null == listener) {
                Log.e(TAG, "okhttp3.Callback.onFailure: e=", e);
                return;
            }

            try {
                listener.onFailed(call.request().url().toString());
            } catch (Exception ee) {
                Log.e(TAG, "okhttp3.Callback.onFailure: ee=", ee);
            }

            listener.onFailed(null);
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            String url = null;
            try {
                url = call.request().url().toString();
            } catch (Exception ignore) {
            }

            if (response == null || !response.isSuccessful()) {
                Log.e(TAG, "okhttp3.Callback.onResponse: bad Response code=" + (response != null ? response.code() : "unknown") + " url=" + url);
                if (null != listener) {
                    listener.onFailed(url);
                    return;
                }
            }

            InputStream is = null;
            byte[] buf = new byte[2048];
            int len = 0;
            FileOutputStream fos = null;
            try {
                is = response.body().byteStream();
                long total = response.body().contentLength();
                File file = new File(dir);
                fos = new FileOutputStream(file);
                long sum = 0;
                int prevProgress = 0;
                while ((len = is.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                    sum += len;
                    int progress = (int) (sum * 1.0f / total * 100);
                    if ((progress - prevProgress) >= 1) {
                        listener.onProgress(url, progress);
                        prevProgress = progress;
                    }
                }
                fos.flush();

                long takeTime = System.currentTimeMillis() - mStartTime;
                listener.onSuccess(url, takeTime);
                Log.e(TAG, "http download take " + takeTime + "ms, file size=" + sum + " save in " + dir);

            } catch (Exception e) {
                if (null != listener) {
                    listener.onFailed(url);
                }
                Log.e(TAG, "okhttp3.Callback.onResponse: download failed at " + url, e);
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
            response.close();
        }
    }
}
