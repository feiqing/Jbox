package com.github.jbox.tracer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;
import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.Configuration.SenderConfiguration;
import io.jaegertracing.internal.propagation.TextMapCodec;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * todo: 先放在这里, 后面有机会放到infra里面
 *
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2022/8/2 10:10.
 */
@Slf4j
public class Tracers {

    private static final String envKey = "EAGLEEYE_TRACE_CONFIG";

    private static final String contextKey = "eagleeye-trace-id";

    private static final String bagPrefix = "eagleeye-ctx-";


    private static final ConcurrentMap<String, Tracer> tracer = new ConcurrentHashMap<>();

    public static Tracer tracer(String app) {
        return tracer.computeIfAbsent(app, _K -> {
            String config = System.getenv(envKey);
            log.info("loaded app:[{}] tracer config: {}", app, config);
            if (!Strings.isNullOrEmpty(config)) {

                JSONObject json = JSON.parseObject(config);


                SenderConfiguration sender = new SenderConfiguration().withEndpoint(json.getString("endpoint"));

                // todo: app-name从配置项里读
                Configuration configuration = new Configuration(app);
                configuration.withTraceId128Bit(true);

                // todo: 配置值从common-envs里读
                configuration.withSampler(new SamplerConfiguration().withType("const").withParam(1));
                configuration.withReporter(new ReporterConfiguration().withSender(sender).withMaxQueueSize(10000));

                return configuration.getTracerBuilder()
                        .registerInjector(Format.Builtin.TEXT_MAP, TextMapCodec.builder().withSpanContextKey(contextKey).withBaggagePrefix(bagPrefix).withUrlEncoding(true).build())
                        .registerExtractor(Format.Builtin.TEXT_MAP, TextMapCodec.builder().withSpanContextKey(contextKey).withBaggagePrefix(bagPrefix).withUrlEncoding(true).build())
                        .withScopeManager(new MDCScopeManager())
                        .build();
            } else {
                return GlobalTracer.get();
            }
        });
    }


    public static void main(String[] args) throws InterruptedException {
        Tracer tracer = Tracers.tracer("xswitch");

        Span span = tracer
                .buildSpan("Esl::BridgeCall")
                .withTag(Tags.COMPONENT.getKey(), "FreeSWITCH")
                .start();

        try (Scope ignored = tracer.scopeManager().activate(span)) {
            System.out.println(span.context().toTraceId());
            System.out.println(bridgeCall());
        } finally {
            span.finish();
        }

        Thread.sleep(1000 * 10000);
    }

    private static Map<String, String> bridgeCall() {
        final Tracer tracer = Tracers.tracer("xswitch");
        Span span = tracer.activeSpan();

        Map<String, String> context = new HashMap<>();

        tracer.inject(span.context(), Format.Builtin.TEXT_MAP, new TextMap() {
            @Override
            public void put(String key, String value) {
                context.put(key, value);
            }

            @Override
            public Iterator<Map.Entry<String, String>> iterator() {
                throw new UnsupportedOperationException("TextMapInjectAdapter should only be used with Tracer.inject()");
            }
        });

        return context;
    }

}
