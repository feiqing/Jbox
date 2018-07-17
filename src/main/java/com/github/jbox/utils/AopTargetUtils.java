package com.github.jbox.utils;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.support.AopUtils;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2016/8/22 15:47:00.
 */
public class AopTargetUtils {

    private static final long EXPIRE_DURATION = 30;

    private static LoadingCache<Object, Object> targetCache = CacheBuilder.newBuilder()
        .expireAfterAccess(EXPIRE_DURATION, TimeUnit.MINUTES)
        .recordStats()
        .build(new CacheLoader<Object, Object>() {
            @Override
            public Object load(Object proxy) throws Exception {
                if (proxy == null) {
                    return null;
                }

                try {
                    // not aop proxy
                    if (!AopUtils.isAopProxy(proxy)) {
                        return proxy;
                    }

                    if (AopUtils.isCglibProxy(proxy)) {
                        return getCglibProxyTarget(proxy);
                    } else if (AopUtils.isJdkDynamicProxy(proxy)) {
                        return getJDKProxyTarget(proxy);
                    } else {
                        return null;
                    }

                } catch (Exception e) {
                    LoggerUtils.error("proxy: {}, getAopProxyTarget error, use default null", proxy, e);
                    return null;
                }
            }
        });

    public static Object getAopTarget(Object bean) {
        return targetCache.getUnchecked(bean);
    }

    private static final String JDK_CALLBACK = "h";

    private static final String JDK_ADVISED = "advised";

    private static Object getJDKProxyTarget(Object proxy) throws Exception {
        AdvisedSupport advisedSupport = (AdvisedSupport)JboxUtils.getFieldValue(proxy, JDK_CALLBACK, JDK_ADVISED);

        if (advisedSupport == null) {
            return null;
        } else {
            return advisedSupport.getTargetSource().getTarget();
        }
    }

    private static final String CGLIB_CALLBACK = "CGLIB$CALLBACK_0";

    private static final String CGLIB_ADVISED = "advised";

    private static Object getCglibProxyTarget(Object proxy) throws Exception {
        AdvisedSupport advisedSupport = (AdvisedSupport)JboxUtils.getFieldValue(proxy, CGLIB_CALLBACK, CGLIB_ADVISED);

        if (advisedSupport == null) {
            return null;
        } else {
            return advisedSupport.getTargetSource().getTarget();
        }
    }
}

