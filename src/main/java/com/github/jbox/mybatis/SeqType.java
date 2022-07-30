package com.github.jbox.mybatis;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2022/7/11 13:52.
 */
public enum SeqType {
    AUTO_INCREMENT, /* DB原生 */
    SEQUENCE_INSTANCE, /* 独立sequence实现 */
}
