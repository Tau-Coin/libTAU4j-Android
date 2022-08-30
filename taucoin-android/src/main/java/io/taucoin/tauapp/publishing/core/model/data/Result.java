package io.taucoin.tauapp.publishing.core.model.data;

public class Result {
    private boolean success = true;     // 是否成功
    private String msg;                 // 消息
    private String key;                 // 唯一key
    private boolean isExist;            // 消息

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setFailMsg(String msg) {
        this.success = false;
        this.msg = msg;
    }

    public boolean isExist() {
        return isExist;
    }

    public void setExist(boolean exist) {
        isExist = exist;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
