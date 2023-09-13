package com.alibaba.jbox.trace.tlog;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/11/11 17:05:00.
 */
interface TlogConstants {

    String SEPARATOR = "|";

    String PLACEHOLDER = "";

    String UTF_8 = "UTF-8";

    String KEY_ARGS = "args";

    String KEY_RESULT = "result";

    String KEY_PLACEHOLDER = "placeholder";

    String LOG_SUFFIX = ".log";

    int DEFAULT_MAX_HISTORY = 15;

    int MAX_THREAD_POOL_SIZE = 12;

    int MIN_THREAD_POOL_SIZE = 3;

    int DEFAULT_RUNNABLE_Q_SIZE = 1024;

    /**
     * Abbreviations for placeholder.
     */
    String KEY_PH = "ph";

    String USER_DEF = "def:";

    String DEF_PREFIX = "${";

    String DEF_SUFFIX = "}";

    String REF_PREFIX = "#{";

    String REF_SUFFIX = "}";

    String XML_FILE_SUFFIX = ".xml";

    String LOGGER_FILE_PATTERN = ".%d{yyyy-MM-dd}.gz";
}
