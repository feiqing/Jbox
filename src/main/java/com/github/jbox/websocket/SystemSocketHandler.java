package com.github.jbox.websocket;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-03-13 15:57:00.
 */
@Slf4j
public class SystemSocketHandler extends TextWebSocketHandler {

    @Getter
    static final ConcurrentMap<SocketKey, List<WebSocketSession>> SESSIONS = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        String msg = MessageFormatter.format("Session:[{}] connect to SocketServer success!", sessionId).getMessage();

        session.sendMessage(new TextMessage(msg));
        log.info("{}", msg);
    }

    @Override
    protected void handleTextMessage(WebSocketSession webSocketSession, TextMessage message) throws Exception {
        String initJsonMsg = message.getPayload();
        if (Strings.isNullOrEmpty(initJsonMsg)) {
            return;
        }

        SocketKey key;
        try {
            key = JSON.parseObject(initJsonMsg, SocketKey.class);
        } catch (Throwable e) {
            String msg = MessageFormatter.format("init error, message:[{}] is not json format", initJsonMsg).getMessage();
            webSocketSession.sendMessage(new TextMessage(msg));
            log.error("{}", msg);
            return;
        }

        if (Strings.isNullOrEmpty(key.getGroup()) || Strings.isNullOrEmpty(key.getTopic())) {
            String msg = MessageFormatter.format("Session:[{}] SocketKey's group & topic can not be empty, {}!", webSocketSession.getId(), initJsonMsg).getMessage();
            webSocketSession.sendMessage(new TextMessage(msg));
            log.error("{}", msg);
            return;
        }

        boolean exits = false;
        List<WebSocketSession> sessions = SESSIONS.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());
        for (WebSocketSession session : sessions) {
            if (StringUtils.equals(session.getId(), webSocketSession.getId())) {
                exits = true;
                break;
            }
        }

        if (!exits) {
            SESSIONS.get(key).add(webSocketSession);
        }

        String msg = MessageFormatter.format("Session:[{}] init:[{}] Success!", webSocketSession.getId(), initJsonMsg).getMessage();
        webSocketSession.sendMessage(new TextMessage(msg));
        log.info("{}", msg);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        handleDestroy(session);
        log.error("Session:[{}] transport occur error.", session.getId(), exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        handleDestroy(session);
        log.warn("Session:[{}] connection closed, status:[{}].", session.getId(), closeStatus);
    }

    private void handleDestroy(WebSocketSession session) throws IOException {
        if (session == null) {
            return;
        }

        try {
            if (session.isOpen()) {
                session.close();
            }
        } finally {
            for (List<WebSocketSession> sessions : SESSIONS.values()) {
                sessions.removeIf(session1 -> StringUtils.equals(session1.getId(), session.getId()));
            }
        }

    }
}
