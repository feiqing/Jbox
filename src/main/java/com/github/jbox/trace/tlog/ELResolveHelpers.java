package com.github.jbox.trace.tlog;

import java.util.Map;

import com.github.jbox.trace.TraceException;
import com.github.jbox.utils.JboxUtils;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/10/20 13:17:00.
 */
class ELResolveHelpers {

    /**
     * 将ref引用的def替换为实际值
     *
     * @param original
     * @param definitions
     * @return
     */
    static String replaceRefToRealString(String original, Map<String, String> definitions) {
        int prefixIdx = original.indexOf(TlogConstants.DEF_PREFIX);
        int suffixIdx = original.indexOf(TlogConstants.DEF_SUFFIX);

        if (prefixIdx != -1 && suffixIdx != -1) {
            String ref = original.substring(prefixIdx, suffixIdx + TlogConstants.DEF_SUFFIX.length());
            String trimmedRef = JboxUtils.trimPrefixAndSuffix(ref, TlogConstants.DEF_PREFIX, TlogConstants.DEF_SUFFIX);
            String refValue = definitions.computeIfAbsent(trimmedRef, (k) -> {
                throw new TraceException("relative def '" + ref + "' is not defined.");
            });

            original = original.replace(ref, refValue);
        }

        return original;
    }
}
