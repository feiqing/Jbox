package com.alibaba.jbox.trace.tlog;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.jbox.trace.TraceException;
import com.alibaba.jbox.utils.T;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import lombok.NonNull;
import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.stream.Collectors.toList;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.2
 * @since 2017/9/22 15:50:00.
 */
public class TlogManager extends AbstractTlogConfig implements InitializingBean {

    private static final long serialVersionUID = -4553832981389212025L;

    private static ExecutorService executor;

    private Logger tLogger;

    public TlogManager() {
        super();
    }

    @Override
    public void afterPropertiesSet() {
        // init executor
        if (executor == null) {
            // todo
            executor = Executors.newFixedThreadPool(getMinPoolSize());
        }

        // init tLogger
        if (tLogger == null) {
            tLogger = LogBackHelper.initTLogger(
                    getUniqueLoggerName(), getFilePath(),
                    getCharset(), "%m%n%n", getMaxHistory(), getTotalSizeCapKb(),
                    getFilters());
        }
    }

    @NonNull
    public void postLogEvent(LogEvent event) {
        executor.submit(new LogEventParser(event));
    }

    protected final class LogEventParser implements Runnable {

        private final LogEvent event;

        LogEventParser(LogEvent event) {
            this.event = event;
        }

        @Override
        public void run() {
            List<Object> logEntities = new LinkedList<>();
            logEntities.add(T.millisFormat(event.getStartTime()));
            logEntities.add(event.getInvokeThread());
            logEntities.add(event.getRt());
            logEntities.add(event.getClassName());
            logEntities.add(event.getMethodName());

            logEntities.add(ifNotNull(event.getArgs(), (args) -> JSONObject.toJSONString(args, getArgFilter())));           // nullable
            logEntities.add(ifNotNull(event.getResult(), (result) -> JSONObject.toJSONString(result, getResultFilter())));  // nullable
            logEntities.add(ifNotNull(event.getException(), JSONObject::toJSONString));                                     // nullable
            logEntities.add(event.getServerIp());
            logEntities.add(event.getTraceId());                                                                            // nullable
            logEntities.add(event.getClientName());                                                                         // nullable
            logEntities.add(event.getClientIp());                                                                           // nullable

            // get config from specified configs map
            List<ELConfig> configs = getMethodELMap().getOrDefault(
                    event.getConfigKey(), getTemplateEL()
            );

            // check is multi config or not
            Object[] multi = getMultiConfig(configs);
            int multiIdx = (int) multi[0];
            ELConfig multiCfg = (ELConfig) multi[1];
            List<ELConfig> notMultiConfigs = (List<ELConfig>) multi[2];

            if (multiIdx == -1) {   // do as single config
                logAsSingle(event, logEntities, notMultiConfigs);
            } else {                // do as multi config
                logAsMulti(event, logEntities, notMultiConfigs, multiIdx, multiCfg);
            }
        }

        private Object[] getMultiConfig(List<ELConfig> configs) {
            List<ELConfig> notMultiConfigs = new ArrayList<>(configs.size());

            int multiIdx = -1;
            ELConfig multiCfg = null;
            for (int i = 0; i < configs.size(); ++i) {
                ELConfig config = configs.get(i);

                if (config.isMulti()) {
                    multiIdx = i;
                    multiCfg = config;
                } else {
                    notMultiConfigs.add(config);
                }
            }

            return new Object[]{multiIdx, multiCfg, notMultiConfigs};
        }

        private void logAsSingle(LogEvent logEvent, List<Object> logEntities, List<ELConfig> notMultiCfgs) {

            // not-multi-config内只有paramEL有效
            List<String> paramELs = notMultiCfgs.stream().map(ELConfig::getParamEL).collect(toList());

            // append到从TraceAspect默认采集的MetaData后面
            List<Object> evalResults = SpELHelpers.evalSpelWithEvent(logEvent, paramELs, getPlaceHolder());
            logEntities.addAll(evalResults);

            doLogger(logEntities);
        }

        private void logAsMulti(LogEvent logEvent, List<Object> logEntities, List<ELConfig> notMultiELConfigs,
                                int multiIdx, ELConfig multiCfg) {
            // 0. 首先将not-multi的config表达式计算出占位值(not-multi-config内只有paramEL有效)
            List<String> notMultiParamELs = notMultiELConfigs.stream().map(ELConfig::getParamEL).collect(toList());
            List<Object> notMultiEvalResults = SpELHelpers.evalSpelWithEvent(logEvent, notMultiParamELs,
                    getPlaceHolder());

            // 1. 将multi指定的参数转换为list
            List listArg = evalMultiValue(logEvent, multiCfg);

            // 2. 遍历list, 将其与not-multi-value & TraceAspect采集到的MetaData合并
            for (Object listArgEntry : listArg) {

                // 2.1)
                List<Object> multiEvalResults;
                if (multiCfg.getInListParamEL().isEmpty()) {                    // 没有field(inner), 如'List<String>'
                    multiEvalResults = Collections.singletonList(listArgEntry);
                } else {                                                        // 有filed(inner), 如'List<User>'
                    multiEvalResults = SpELHelpers.evalSpelWithObject(listArgEntry, multiCfg.getInListParamEL(), getPlaceHolder());
                }

                // 2.2) 将trace-metadata、not-multi-value、multi-value三者合并
                List<Object> logEntitiesCopy = mergeEvalResult(notMultiEvalResults, multiEvalResults, logEntities, multiIdx,
                        notMultiELConfigs.size() + 1);

                doLogger(logEntitiesCopy);
            }
        }

        /**
         * 使用multi-config指定的paramEL对event求值, 并将值转换为List
         *
         * @param logEvent
         * @param multiCfg
         * @return
         */
        private List evalMultiValue(LogEvent logEvent, ELConfig multiCfg) {
            List<Object> multiArgs = SpELHelpers.evalSpelWithEvent(logEvent, Collections.singletonList(multiCfg.getParamEL()),
                    getPlaceHolder());
            Preconditions.checkState(multiArgs.size() == 1);

            return transMultiArgToList(multiArgs.get(0));
        }

        @SuppressWarnings("unchecked")
        private List transMultiArgToList(Object multiArg) {
            if (multiArg == null) {
                return Collections.emptyList();
            } else if (multiArg instanceof ArrayList) {
                return (List) multiArg;
            } else if (multiArg instanceof Collections) {
                return new ArrayList((Collection) multiArg);
            } else if (multiArg.getClass().isArray()) {
                return Arrays.stream((Object[]) multiArg).collect(toList());
            } else {
                throw new TraceException("argument [" + multiArg + "] neither array nor collection instance.");
            }
        }

        private List<Object> mergeEvalResult(List<Object> notMultiEvalResults, List<Object> multiEvalResults,
                                             List<Object> logEntities, int multiIdx, int paramCount) {
            List<Object> logEntitiesCopy = new ArrayList<>(logEntities);
            for (int paramIdx = 0, notMultiResultIdx = 0; paramIdx < paramCount; ++paramIdx) {
                if (paramIdx == multiIdx) {                 // is multi arg
                    logEntitiesCopy.addAll(multiEvalResults);
                } else {
                    logEntitiesCopy.add(notMultiEvalResults.get(notMultiResultIdx));
                    notMultiResultIdx++;
                }
            }

            return logEntitiesCopy;
        }

        private void doLogger(List<Object> logEntities) {
            String content = Joiner.on(TlogConstants.SEPARATOR).useForNull(getPlaceHolder()).join(logEntities);
            tLogger.trace("{}", content);
        }
    }

    private String ifNotNull(Object nullableObj, Function<Object, String> processor) {
        return nullableObj == null ? null : processor.apply(nullableObj);
    }
}
