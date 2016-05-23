package com.kfpanda.logengine.entity.log;


public class LogContent {
    private String requestId;
    
    private String modualId;
    
    private String userName;
    
    private String param;
    
    private String ip;
    
    private String logTime;
    
    private String requestUrl;

    
    public String getRequestId() {
        return requestId;
    }

    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    
    public String getModualId() {
        return modualId;
    }

    
    public void setModualId(String modualId) {
        this.modualId = modualId;
    }

    
    public String getUserName() {
        return userName;
    }

    
    public void setUserName(String userName) {
        this.userName = userName;
    }

    
    public String getParam() {
        return param;
    }

    
    public void setParam(String param) {
        this.param = param;
    }

    
    public String getIp() {
        return ip;
    }

    
    public void setIp(String ip) {
        this.ip = ip;
    }

    
    public String getLogTime() {
        return logTime;
    }

    
    public void setLogTime(String logTime) {
        this.logTime = logTime;
    }

    
    public String getRequestUrl() {
        return requestUrl;
    }

    
    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }
    
    
}
