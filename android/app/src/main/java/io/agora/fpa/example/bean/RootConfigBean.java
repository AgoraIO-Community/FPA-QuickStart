package io.agora.fpa.example.bean;


import com.google.gson.annotations.SerializedName;

public class RootConfigBean {

    @SerializedName("app_id")
    private String appId;

    @SerializedName("http_chain_info")
    private HttpChainInfo httpChainInfo;

    @SerializedName("http_request_host")
    private String requestHost;

    @SerializedName("http_request_ip")
    private String requestIp;


    public String getAppId() {
        return appId;
    }

    public HttpChainInfo getHttpChainInfo() {
        return httpChainInfo;
    }

    public String getRequestHost() {
        return requestHost;
    }

    public String getRequestIp() {
        return requestIp;
    }

}