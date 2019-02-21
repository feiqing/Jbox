package com.github.jbox.websocket;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.jbox.scheduler.ScheduleTask;
import com.github.jbox.utils.AopTargetUtils;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-03-13 21:11:00.
 */
public class MessageProducerRegister implements ScheduleTask, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(MessageProducerRegister.class);

    private static final ConcurrentMap<String, MethodEntry> method2Bean = new ConcurrentHashMap<>();

    private static final String CLASS = "class";

    private static final String METHOD = "method";

    private static final String CONVERTER = "converter";

    private Resource configResource;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        try {
            JSONArray configs = JSONObject.parseArray(readConfig(configResource));
            for (int i = 0; i < configs.size(); ++i) {
                JSONObject config = configs.getJSONObject(i);
                Class<?> clazz = Class.forName(getNotEmptyValue(config, CLASS));
                Object producerBean = applicationContext.getBean(clazz);
                if (producerBean == null) {
                    continue;
                }

                Object producer = AopTargetUtils.getAopTarget(producerBean);
                Method method = getMethod(clazz, getNotEmptyValue(config, METHOD));
                Function<String, Object[]> converter = getConverter(getNotEmptyValue(config, CONVERTER));
                method2Bean.put(method.getName(), new MethodEntry(method, producer, converter));
            }
        } catch (IOException e) {
            logger.error("could not fond WebSocketProducer config file:[{}]", configResource.getFilename());
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void setConfig(Resource configResource) {
        this.configResource = configResource;
    }

    private String readConfig(Resource resource) throws IOException {
        try (InputStream is = resource.getInputStream();
             Reader reader = new InputStreamReader(is)) {
            return CharStreams.toString(reader);
        }
    }

    private String getNotEmptyValue(JSONObject json, String key) {
        String value = json.getString(key);
        if (Strings.isNullOrEmpty(value)) {
            throw new RuntimeException(String.format("key:[%s] related value is empty", key));
        }

        return value;
    }

    private Method getMethod(Class<?> clazz, String methodName) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (StringUtils.equals(methodName, method.getName())) {
                ReflectionUtils.makeAccessible(method);
                return method;
            }
        }

        throw new RuntimeException(String.format("could not find method:[%s] in class:[%s]", methodName, clazz.getName()));
    }

    @SuppressWarnings("unchecked")
    private Function<String, Object[]> getConverter(String converterName) throws ClassNotFoundException {
        int index = converterName.lastIndexOf(".");
        String className = converterName.substring(0, index);
        String converterFieldName = converterName.substring(index + 1);

        AtomicReference<Function<String, Object[]>> resultRef = new AtomicReference<>();
        ReflectionUtils.doWithFields(
                Class.forName(className),
                field -> {
                    ReflectionUtils.makeAccessible(field);
                    resultRef.set((Function<String, Object[]>) field.get(null));
                },
                field -> field.getName().equals(converterFieldName)
        );

        return resultRef.get();
    }

    @Override
    public void schedule() throws Exception {
        ConcurrentMap<SocketKey, List<WebSocketSession>> sessions = SystemSocketHandler.getSESSIONS();
        for (Map.Entry<SocketKey, List<WebSocketSession>> entry : sessions.entrySet()) {
            if (CollectionUtils.isEmpty(entry.getValue())) {
                continue;
            }

            SocketKey socketKey = entry.getKey();
            String methodName = socketKey.getGroup();
            String argStr = socketKey.getTopic();

            MethodEntry methodEntry = method2Bean.get(methodName);
            if (methodEntry == null) {
                continue;
            }

            Object result = methodEntry.getMethod().invoke(methodEntry.getTarget(), methodEntry.getArgsConverter().apply(argStr));
            WebSocketMsgProducer.sendMessage(socketKey, result);
        }
    }

    @Override
    public long period() {
        return _10S_INTERVAL;
    }

    @Data
    @AllArgsConstructor
    private static class MethodEntry {
        private Method method;

        private Object target;

        private Function<String, Object[]> argsConverter;
    }
}
