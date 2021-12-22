package io.agora.fpa.example;

public interface IUploadListener {
    void onSuccess(String url, long times);

    void onFailed(String url);

    void onProgress(String url, int process);
}
