package com.foodcommunity.security;

import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.foodcommunity.common.LoginUser;
import com.foodcommunity.common.Result;
import com.foodcommunity.common.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 一体化鉴权拦截器（作用域 /api/**）。
 *
 * 规则：
 *  - 白名单（无需任何凭证）：/api/auth/login、/api/auth/register
 *  - /api/admin/**：一律需要有效 JWT 且 role=1（管理员），否则 401 / 403
 *  - /api/my/**：私人数据（我的菜谱/收藏/评论/预约等），GET 也一律需登录
 *  - 其余路径：GET 放行（游客可浏览），写操作（POST/PUT/DELETE）需有效 JWT
 *  - 携带有效 token 的请求会把登录身份写入 UserContext，请求结束自动清理
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register"
    );

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();
        if (PUBLIC_PATHS.contains(uri)) {
            return true;
        }

        boolean isAdminApi = uri.startsWith("/api/admin/");
        // 私人数据前缀：无论 GET/POST 都需登录（我的内容 + 聊天会话/历史/未读）
        boolean isPrivateApi = uri.startsWith("/api/my/") || uri.startsWith("/api/chat/");

        // 尝试解析 token（游客可能不带，也可能带无效 token——无效一律视为未登录）
        LoginUser loginUser = resolveLoginUser(request);

        if (isAdminApi) {
            if (loginUser == null) {
                return reject(response, Result.CODE_UNAUTHORIZED, "未登录或登录已过期");
            }
            if (loginUser.role() == null || loginUser.role() != 1) {
                return reject(response, Result.CODE_FORBIDDEN, "无管理员权限");
            }
            UserContext.set(loginUser);
            return true;
        }

        // 私人数据接口：游客一律 401
        if (isPrivateApi && loginUser == null) {
            return reject(response, Result.CODE_UNAUTHORIZED, "未登录或登录已过期");
        }
        // 其余写操作必须登录；GET 游客放行
        if (!"GET".equalsIgnoreCase(request.getMethod()) && loginUser == null) {
            return reject(response, Result.CODE_UNAUTHORIZED, "未登录或登录已过期");
        }
        if (loginUser != null) {
            UserContext.set(loginUser);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    private LoginUser resolveLoginUser(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (StrUtil.isBlank(header) || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        try {
            DecodedJWT jwt = jwtUtil.verify(header.substring(BEARER_PREFIX.length()));
            Long userId = Long.valueOf(jwt.getSubject());
            Integer role = jwt.getClaim("role").asInt();
            return new LoginUser(userId, role);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean reject(HttpServletResponse response, int code, String msg) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK); // 业务码走 body，HTTP 恒 200，便于前端统一处理
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(code, msg)));
        return false;
    }
}
