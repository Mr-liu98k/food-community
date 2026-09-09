package com.foodcommunity.config;

import com.foodcommunity.security.JwtUtil;
import com.foodcommunity.ws.ChatWebSocketHandler;
import com.foodcommunity.ws.WsSessionRegistry;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 配置：/api/ws/chat?token=xxx
 * 握手阶段用 JWT 校验身份，userId 存入 session attributes 供 Handler 使用。
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    public static final String WS_PATH = "/api/ws/chat";
    public static final String ATTR_USER_ID = "userId";

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final WsSessionRegistry wsSessionRegistry;
    private final JwtUtil jwtUtil;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, WS_PATH)
                .addInterceptors(new AuthHandshakeInterceptor())
                .setAllowedOriginPatterns("*");
    }

    /** 握手拦截器：query 中 token 校验通过才允许建立连接 */
    private class AuthHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                       @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
            String query = request.getURI().getQuery();
            if (query == null) {
                return false;
            }
            String token = null;
            for (String pair : query.split("&")) {
                if (pair.startsWith("token=")) {
                    token = pair.substring(6);
                    break;
                }
            }
            if (token == null || token.isBlank()) {
                return false;
            }
            try {
                DecodedJWT jwt = jwtUtil.verify(token);
                attributes.put(ATTR_USER_ID, Long.valueOf(jwt.getSubject()));
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler, Exception exception) {
            // no-op
        }
    }
}
