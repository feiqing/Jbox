package com.github.jbox.trace.tlog;

import com.alibaba.fastjson.serializer.SerializeFilter;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.jbox.trace.tlog.TlogConstants.*;

/**
 * TLogManager统一配置
 *
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.2
 * @since 2017/9/26 14:09:00.
 */
@Data
public abstract class AbstractTlogConfig implements Serializable {

    private static final long serialVersionUID = 5924881023295492855L;

    /**
     * class:method -> configs
     */
    private ConcurrentMap<String, List<ELConfig>> methodELMap = new ConcurrentHashMap<>();

    private AtomicReference<List<ELConfig>> templateEL = new AtomicReference<>(Collections.emptyList());

    private int minPoolSize = MIN_THREAD_POOL_SIZE;

    private int maxPoolSize = MAX_THREAD_POOL_SIZE;

    private int runnableQSize = DEFAULT_RUNNABLE_Q_SIZE;

    private int maxHistory = DEFAULT_MAX_HISTORY;

    private String placeHolder = PLACEHOLDER;

    private final Set<String> excludeArgs = new ConcurrentSkipListSet<>();

    private volatile SerializeFilter argFilter;

    private final Set<String> excludeResults = new ConcurrentSkipListSet<>();

    private volatile SerializeFilter resultFilter;

    /**
     * 为防止日志打串, 最好为每一个logger、appender定义一个独立的name
     * 默认为filePath
     */
    private String uniqueLoggerName;

    private List<TlogFilter> filters;

    private String charset = UTF_8;

    private String filePath;

    private long totalSizeCapKb = 0;

    public void setResource(Resource resource) throws IOException {
        String fileName = resource.getFilename();
        Preconditions.checkArgument(!Strings.isNullOrEmpty(fileName), "el resource config file name can't be empty");
        String fileType = fileName.substring(fileName.lastIndexOf('.'));
        Preconditions.checkArgument(StringUtils.equals(fileType, XML_FILE_SUFFIX), "config file is not xml format.");
        ELXmlResolver.resolveConfigFile(resource.getInputStream(), methodELMap, fileName);
    }

    @Data
    @ToString
    public static class ELConfig {

        private String paramEL;

        private List<String> inListParamEL;

        public ELConfig(String paramEL, List<String> inListParamEL) {
            this.paramEL = paramEL;
            this.inListParamEL = inListParamEL;
        }

        public boolean isMulti() {
            return this.inListParamEL != null;
        }
    }

    List<ELConfig> getTemplateEL() {
        return this.templateEL.get();
    }

    String getUniqueLoggerName() {
        if (Strings.isNullOrEmpty(this.uniqueLoggerName)) {
            String loggerName = this.filePath;
            int prefixIndex = loggerName.lastIndexOf(File.separator);
            if (prefixIndex != -1) {
                loggerName = loggerName.substring(prefixIndex + 1);
            }

            int suffixIndex = loggerName.lastIndexOf(LOG_SUFFIX);
            if (suffixIndex != -1) {
                loggerName = loggerName.substring(0, suffixIndex);
            }

            return loggerName;
        } else {
            return this.uniqueLoggerName;
        }
    }

    SerializeFilter getArgFilter() {
        // todo
//        if (this.argFilter == null) {
//            synchronized (excludeArgs) {
//                if (this.argFilter == null) {
//                    this.argFilter = (PropertyPreFilter) (serializer, object, name) -> !excludeArgs.contains(name);
//                }
//            }
//        }

        return this.argFilter;
    }

    SerializeFilter getResultFilter() {
        // todo
//        if (this.resultFilter == null) {
//            synchronized (excludeResults) {
//                this.resultFilter = (PropertyPreFilter) (serializer, object, name) -> !excludeResults.contains(name);
//            }
//        }

        return this.resultFilter;
    }
}
