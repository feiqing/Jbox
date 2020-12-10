//package com.github.jbox.trace.tasks;
//
//import com.github.jbox.job.JobTask;
//import com.github.jbox.trace.TraceJobContext;
//import com.google.common.base.Strings;
//import com.taobao.eagleeye.EagleEye;
//import org.slf4j.MDC;
//
//
///**
// * @author jifang.zjf@alibaba-inc.com (FeiQing)
// * @version 1.0
// * @since 1.0 - put traceId
// * @since 2018-07-26 07:36:00.
// */
//public class EagleEyeTask implements JobTask<TraceJobContext> {
//
//    private static final long serialVersionUID = 5505706595178320282L;
//
//    /**
//     * append {@code %X{traceId}} in logback/log4j MDC.
//     */
//    private static final String TRACE_ID = "traceId";
//
//    @Override
//    public void invoke(TraceJobContext context) throws Throwable {
//        String traceId = EagleEye.getTraceId();
//        if (Strings.isNullOrEmpty(traceId)) {
//            traceId = "-";
//        }
//
//        MDC.put(TRACE_ID, traceId);
//        try {
//            context.next();
//        } finally {
//            MDC.remove(TRACE_ID);
//        }
//    }
//}
