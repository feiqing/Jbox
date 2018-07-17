package com.github.jbox.websocket;

import com.alibaba.fastjson.JSONObject;
import com.github.jbox.executor.AsyncContext;
import com.github.jbox.executor.AsyncRunnable;
import com.github.jbox.executor.ExecutorManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static com.github.jbox.websocket.SystemSocketHandler.SESSIONS;


/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-03-13 18:55:00.
 */
@Slf4j
public class WebSocketMsgProducer {

    private static final ExecutorService executor = ExecutorManager.newFixedMinMaxThreadPool("com.github.jbox.websocket:WebSocketMsgProducer", 5, 20, 1024);


    public static void sendMessage(SocketKey key, List<Object> objs) {
        for (Object obj : objs) {
            sendMessage(key, obj);
        }
    }

    public static void sendMessage(SocketKey key, Object o) {
        JSONObject objectWrapper = new JSONObject(2);
        objectWrapper.put("key", key);
        objectWrapper.put("object", o);

        String content = objectWrapper.toJSONString();
        List<WebSocketSession> sessions = SESSIONS.getOrDefault(key, Collections.emptyList());
        for (WebSocketSession session : sessions) {
            sendMessage(session, content);
        }
    }

    private static void sendMessage(WebSocketSession session, String message) {
        executor.submit(new AsyncRunnable() {
            @Override
            public void execute(AsyncContext context) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(message));
                    } catch (IOException e) {
                        log.error("send message error, id:{}, message:{}", session.getId(), message, e);
                    }
                }
            }

            @Override
            public String taskInfo() {
                return String.format("id:[%s], message:[%s]", session.getId(), message.substring(0, 10));
            }
        });
    }
}
