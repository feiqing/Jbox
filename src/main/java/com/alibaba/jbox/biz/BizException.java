package com.alibaba.jbox.biz;

import com.alibaba.jbox.utils.Collections3;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.slf4j.helpers.MessageFormatter;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-03-01 16:51:00.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BizException extends RuntimeException implements IErrorCode {

    private static final long serialVersionUID = 3646294986600769469L;

    private String code;

    private String msg;

    public BizException(IErrorCode error) {
        this(error.getCode(), error.getMsg());
    }

    public BizException(String code, String msg) {
        super(String.format("[%s]:%s", code, msg));
        this.code = code;
        this.msg = msg;
    }

    public BizException(String code, Throwable cause) {
        super(String.format("[%s]", code), cause);
        this.code = code;
    }

    public static BizException bizException(IErrorCode errorCode) {
        return new BizException(errorCode);
    }

    public static BizException bizException(IErrorCode errorCode, Throwable throwable) {
        return new BizException(errorCode.getCode(), throwable);
    }

    public static BizException bizException(IErrorCode errorCode, String msgTemplate, Object... args) {
        return new BizException(errorCode.getCode(), formatMessage(msgTemplate, args));
    }

    public static BizException bizException(String errorCode, String msgTemplate, Object... args) {
        return new BizException(errorCode, formatMessage(msgTemplate, args));
    }

    private static String formatMessage(String msgTemplate, Object... args) {
        if (Collections3.isEmpty(args)) {
            return msgTemplate;
        }
        return MessageFormatter.format(msgTemplate, args).getMessage();
    }
}
