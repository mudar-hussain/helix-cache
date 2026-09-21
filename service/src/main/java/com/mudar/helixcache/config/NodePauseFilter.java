package com.mudar.helixcache.config;

import com.mudar.helixcache.cluster.NodeStateManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class NodePauseFilter extends OncePerRequestFilter {

    private final NodeStateManager nodeStateManager;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if(!nodeStateManager.isPaused()) {
            //Node is healthy
            filterChain.doFilter(request, response);
            return;
        }

        String method = request.getMethod();
        String path = request.getRequestURI();

        // Always allow below api when paused:
        // 1. Options - browser CORS preflight, must succeed so the resume PUT can be sent
        // 2. PUT /admin/node/resume - the only way to unpause
        // 3. GET /actuator/** - keep health probes alive so the JVM isn't killed
        boolean isAllowed = "OPTIONS".equalsIgnoreCase(method)
                || ("PUT".equalsIgnoreCase(method) && path.equals("/admin/node/resume"))
                || path.startsWith("/actuator");

        if(isAllowed) {
            filterChain.doFilter(request, response);
            return;
        }

        //Block rest API calls with status 503 :)
        log.debug("Node is PAUSED - blocking {} {}", method, path);
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("application/json");
        String safePath = path.replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write(
                "{\"error\":\"Node is paused\",\"path\":\"" + safePath + "\"}"
        );
    }
}
