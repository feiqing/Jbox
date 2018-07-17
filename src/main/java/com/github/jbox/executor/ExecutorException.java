package com.github.jbox.executor;

/**
 * @author jifang.zjf
 * @since 2017/7/14 上午10:57.
 */
class ExecutorException extends RuntimeException {
    public ExecutorException(Throwable cause) {
        super(cause);
    }

    public ExecutorException(String message) {
        super(message);
    }
}
