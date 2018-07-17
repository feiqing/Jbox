package com.github.jbox.trace.tlog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.PropertyPreFilter;
import com.alibaba.fastjson.serializer.SerializeFilter;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import org.springframework.core.io.Resource;

import static com.github.jbox.trace.tlog.TLogConstants.DEFAULT_MAX_HISTORY;
import static com.github.jbox.trace.tlog.TLogConstants.DEFAULT_RUNNABLE_Q_SIZE;
import static com.github.jbox.trace.tlog.TLogConstants.GROOVY_FILE_SUFFIX;
import static com.github.jbox.trace.tlog.TLogConstants.JSON_FILE_SUFFIX;
import static com.github.jbox.trace.tlog.TLogConstants.LOG_SUFFIX;
import static com.github.jbox.trace.tlog.TLogConstants.MAX_THREAD_POOL_SIZE;
import static com.github.jbox.trace.tlog.TLogConstants.MIN_THREAD_POOL_SIZE;
import static com.github.jbox.trace.tlog.TLogConstants.PLACEHOLDER;
import static com.github.jbox.trace.tlog.TLogConstants.UTF_8;
import static com.github.jbox.trace.tlog.TLogConstants.XML_FILE_SUFFIX;

/**
 * TLogManager统一配置
 *
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.2
 * @since 2017/9/26 14:09:00.
 */
public abstract class AbstractTLogConfig implements Serializable {

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

    /**
     * 为防止日志打串, 最好为每一个logger、appender定义一个独立的name
     * 默认为filePath
     */
    private String uniqueLoggerName;

    private List<TLogFilter> filters;

    private String charset = UTF_8;

    private boolean sync = false;

    private String filePath;

    private long totalSizeCapKb = 0;

    /**
     * 将方法入参序列化时(JSONObject::toJSONString)排除的属性
     */
    private final Set<String> excludeArgs = new ConcurrentSkipListSet<>();

    private volatile SerializeFilter argFilter;

    /**
     * 将方法返回值序列化时(JSONObject::toJSONString)排除的属性
     */
    private final Set<String> excludeResults = new ConcurrentSkipListSet<>();

    private volatile SerializeFilter resultFilter;

    public void setResource(Resource resource) throws IOException {
        String fileName = resource.getFilename();
        Preconditions.checkArgument(!Strings.isNullOrEmpty(fileName), "el resource config file name can't be empty");
        if (fileName.endsWith(JSON_FILE_SUFFIX)) {
            ELJsonResolver.resolve(readConfig(resource), methodELMap);
        } else if (fileName.endsWith(XML_FILE_SUFFIX)) {
            ELXmlResolver.resolve(fileName, resource.getInputStream(), methodELMap, templateEL);
        } else if (fileName.endsWith(GROOVY_FILE_SUFFIX)) {
            ELGroovyResolver.resolve(fileName, resource.getInputStream(), methodELMap);
        }
    }

    public void setMethodELMap(Map<String, List<ELConfig>> methodELMap) {
        this.methodELMap.putAll(methodELMap);
    }

    public void addFilter(TLogFilter filter) {
        if (this.filters == null) {
            this.filters = new LinkedList<>();
        }
        this.filters.add(filter);
    }

    public String getUniqueLoggerName() {
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

    private String readConfig(Resource resource) throws IOException {
        try (InputStream is = resource.getInputStream();
             Reader reader = new InputStreamReader(is)) {
            return CharStreams.toString(reader);
        }
    }

    public static class ELConfig {

        private String paramEL;

        private List<String> inListParamEL;

        public ELConfig(String paramEL, List<String> inListParamEL) {
            this.paramEL = paramEL;
            this.inListParamEL = inListParamEL;
        }

        public static List<ELConfig> parseEntriesFromJsonArray(JSONArray array) {
            List<ELConfig> configs = new ArrayList<>(array.size());
            for (Object jsonEntry : array) {
                String paramEL = null;
                List<String> inListParamEL = null;

                if (jsonEntry instanceof String) {
                    paramEL = (String)jsonEntry;
                } else if (jsonEntry instanceof JSONObject) {
                    JSONObject jsonObject = (JSONObject)jsonEntry;
                    Preconditions.checkArgument(jsonObject.size() == 1);

                    Entry<String, Object> next = jsonObject.entrySet().iterator().next();
                    Preconditions.checkArgument(next.getValue() instanceof JSONArray);
                    paramEL = next.getKey();
                    inListParamEL = ((JSONArray)next.getValue()).stream().map(Object::toString).collect(
                        Collectors.toList());
                }

                configs.add(new ELConfig(paramEL, inListParamEL));
            }

            return configs;
        }

        public boolean isMulti() {
            return this.inListParamEL != null;
        }

        public String getParamEL() {
            return paramEL;
        }

        public void setParamEL(String paramEL) {
            this.paramEL = paramEL;
        }

        public List<String> getInListParamEL() {
            return inListParamEL;
        }

        public void setInListParamEL(List<String> inListParamEL) {
            this.inListParamEL = inListParamEL;
        }

        @Override
        public String toString() {
            return "ELConfig{" +
                "paramEL='" + paramEL + '\'' +
                ", inListParamEL=" + inListParamEL +
                '}';
        }
    }

    public ConcurrentMap<String, List<ELConfig>> getMethodELMap() {
        return methodELMap;
    }

    public List<ELConfig> getTemplateEL() {
        return this.templateEL.get();
    }

    public void setMethodELMap(ConcurrentMap<String, List<ELConfig>> methodELMap) {
        this.methodELMap = methodELMap;
    }

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getRunnableQSize() {
        return runnableQSize;
    }

    public void setRunnableQSize(int runnableQSize) {
        this.runnableQSize = runnableQSize;
    }

    public int getMaxHistory() {
        return maxHistory;
    }

    public void setMaxHistory(int maxHistory) {
        this.maxHistory = maxHistory;
    }

    public String getPlaceHolder() {
        return placeHolder;
    }

    public void setPlaceHolder(String placeHolder) {
        this.placeHolder = placeHolder;
    }

    public void setUniqueLoggerName(String uniqueLoggerName) {
        this.uniqueLoggerName = uniqueLoggerName;
    }

    public List<TLogFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<TLogFilter> filters) {
        this.filters = filters;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public boolean isSync() {
        return sync;
    }

    public void setSync(boolean sync) {
        this.sync = sync;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getTotalSizeCapKb() {
        return totalSizeCapKb;
    }

    public void setTotalSizeCapKb(long totalSizeCapKb) {
        this.totalSizeCapKb = totalSizeCapKb;
    }

    public void setExcludeArgs(Set<String> excludeArgs) {
        if (excludeArgs != null && !excludeArgs.isEmpty()) {
            this.excludeArgs.addAll(excludeArgs);
        }
    }

    public void setExcludeResults(Set<String> excludeResults) {
        if (excludeResults != null && !excludeResults.isEmpty()) {
            this.excludeResults.addAll(excludeResults);
        }
    }

    protected SerializeFilter getArgFilter() {
        if (this.argFilter == null) {
            synchronized (excludeArgs) {
                if (this.argFilter == null) {
                    this.argFilter = (PropertyPreFilter)(serializer, object, name) -> !excludeArgs.contains(name);
                }
            }
        }

        return this.argFilter;
    }

    protected SerializeFilter getResultFilter() {
        if (this.resultFilter == null) {
            synchronized (excludeResults) {
                this.resultFilter = (PropertyPreFilter)(serializer, object, name) -> !excludeResults.contains(name);
            }
        }

        return this.resultFilter;
    }
}
