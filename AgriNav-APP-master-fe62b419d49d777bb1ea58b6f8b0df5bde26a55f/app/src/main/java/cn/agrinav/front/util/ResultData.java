package cn.agrinav.front.util;

public class ResultData<T> {
    private Integer code;
    private String msg;
    private Integer total;
    private T data;

    public ResultData(Integer code, String msg, Integer title) {
        this(code, msg, title, null);
    }

    public ResultData(Integer code, String msg, Integer total, T data) {
        this.code = code;
        this.msg = msg;
        this.total = total;
        this.data = data;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(int title) {
        this.total = title;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }


    @Override
    public String toString() {
        return "ResultData{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", total=" + total +
                ", data=" + data +
                '}';
    }
}
