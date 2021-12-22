package io.agora.fpa.example.bean;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class HttpChainInfo {

    @SerializedName("fallback")
    private boolean fallback;

    @SerializedName("chain_info")
    private List<ChainInfo> chainInfo;


    public boolean getFallback() {
        return fallback;
    }

    public List<ChainInfo> getChainInfo() {
        return chainInfo;
    }
}