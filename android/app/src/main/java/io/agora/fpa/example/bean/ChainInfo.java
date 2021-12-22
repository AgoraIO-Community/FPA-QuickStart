package io.agora.fpa.example.bean;

import com.google.gson.annotations.SerializedName;

public class ChainInfo {

    @SerializedName("chain_id")
    private int chainId;

    @SerializedName("domain")
    private String domain;

    @SerializedName("port")
    private int port;

    @SerializedName("fallback")
    private boolean fallback;


    public int getChainId() {
        return chainId;
    }

    public String getDomain() {
        return domain;
    }

    public int getPort() {
        return port;
    }

    public boolean getFallback() {
        return fallback;
    }

}