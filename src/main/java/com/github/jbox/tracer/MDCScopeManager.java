/*
 * Copyright (c) 2020, The Jaeger Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.github.jbox.tracer;

import io.jaegertracing.internal.JaegerSpanContext;
import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.util.ThreadLocalScopeManager;
import org.slf4j.MDC;

class MDCScopeManager implements ScopeManager {

    private static final String mdcTraceIdKey = "traceId";

    private static final String eagleEyeTraceIdKey = "EAGLEEYE_TRACE_ID";

    private static final String mdcSpanIdKey = "spanId";

    private static final String mdcSampledKey = "sampled";

    private final ScopeManager wrappedScopeManager = new ThreadLocalScopeManager();

    @Override
    public Scope activate(Span span) {
        return new MDCScope(wrappedScopeManager.activate(span), span);
    }

    @Override
    public Span activeSpan() {
        return wrappedScopeManager.activeSpan();
    }

    private static class MDCScope implements Scope {
        private final Scope wrappedScope;
        private final String previousTraceId;
        private final String previousSpanId;
        private final String previousSampled;


        MDCScope(Scope scope, Span span) {
            this.wrappedScope = scope;
            this.previousTraceId = MDC.get(mdcTraceIdKey);
            this.previousSpanId = MDC.get(mdcSpanIdKey);
            this.previousSampled = MDC.get(mdcSampledKey);

            if (span.context() instanceof JaegerSpanContext) {
                putContext((JaegerSpanContext) span.context());
            }
        }

        protected void putContext(JaegerSpanContext spanContext) {
            replace(mdcTraceIdKey, spanContext.toTraceId());
            replace(eagleEyeTraceIdKey, spanContext.toTraceId());
            replace(mdcSpanIdKey, spanContext.toSpanId());
            replace(mdcSampledKey, String.valueOf(spanContext.isSampled()));
        }

        private void replace(String key, String value) {
            if (value == null) {
                MDC.remove(key);
            } else {
                MDC.put(key, value);
            }
        }

        @Override
        public void close() {
            wrappedScope.close();
            replace(mdcTraceIdKey, previousTraceId);
            replace(eagleEyeTraceIdKey, previousTraceId);
            replace(mdcSpanIdKey, previousSpanId);
            replace(mdcSampledKey, previousSampled);
        }
    }
}
