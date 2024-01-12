package com.yupi.yupaobackend.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.HashMap;
import java.util.Map;

/**
 * @description:
 * @author: yumo
 * @create: 2024-01-12 00:55
 **/
@Component
@ServerEndpoint("/ws/{sid}")
@Slf4j
public class WebSocketServer {

    private static final Map<String, Session> sessionMap = new HashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        log.info("[onOpen]: sid = {}", sid);
        sessionMap.put(sid, session);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        this.sendToAllClient(message);
        log.info("收到消息[onMessage]: sid = {}, message = {}", sid, message);
    }

    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        log.info("[onClose]: sid = {}", sid);
        sessionMap.remove(sid);
    }

    public void sendToAllClient(String message) {
        sessionMap.values().forEach(session -> {
            try {
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
