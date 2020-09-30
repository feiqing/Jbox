package com.github.jbox.biz;

import com.alibaba.fastjson.annotation.JSONField;
import com.github.jbox.utils.DateUtils;
import com.github.jbox.utils.IPv4;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-07-25 10:30:00.
 */
@Data
public class ResultDO<T> implements Serializable {

    private static final long serialVersionUID = 4656077808890768073L;

    private T data;

    private boolean success;

    private String code;

    private String msg;

    private String traceId;

    @JSONField(name = "system_time")
    private String systemTime = DateUtils.timeFormat(new Date());

    @JSONField(name = "server_ip")
    private String serverIp = IPv4.getLocalIp();

    @JSONField(name = "stack_trace")
    private String stackTrace;

    public boolean isSuccess() {
        return success;
    }

    public static <T> ResultDO<T> errorOf(String code, String msg) {
        ResultDO<T> resultDO = new ResultDO<>();

        resultDO.setSuccess(false);
        resultDO.setCode(code);
        resultDO.setMsg(msg);

        return resultDO;
    }

    public static <T> ResultDO<T> errorOf(IErrorCode error) {
        return errorOf(error.getCode(), error.getMsg());
    }

    public static <T> ResultDO<T> errorOf(String msg) {
        return errorOf("500", msg);
    }

    public static <T> ResultDO<T> successOf(T data) {
        ResultDO<T> resultDO = new ResultDO<>();

        resultDO.setData(data);

        resultDO.setSuccess(true);
        resultDO.setCode("200");
        resultDO.setMsg(null);

        return resultDO;
    }

    public static <T> ResultDO<T> successOf() {
        return successOf(null);
    }
}