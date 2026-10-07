package com.evops.aqua.staff.config;

import com.evops.aqua.staff.service.StaffAccessService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 现场人员接口权限实时拦截：
 * 现场 App 经 X-Staff-No 访问受管业务接口时，按 StaffAccessService 唯一口径裁决——
 * 人员冻结/离场、证书过期、临时授权到期或不在岗位接口范围内，立即 403 收回接口权限，
 * 不依赖定时扫描先跑（扫描只负责台账状态与留证）。
 *
 * 管理端（会话 Cookie、不带 X-Staff-No）不受本过滤器限制；
 * 资质授权管理面 /api/aqua/staff/** 与健康/登录面放行，避免现场头误伤管理操作。
 */
@Configuration
public class StaffAccessFilterConfig {

    private static final String STAFF_NO_HEADER = "X-Staff-No";

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> staffAccessFilter(
            StaffAccessService accessService) {
        OncePerRequestFilter filter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain) throws ServletException, IOException {
                String staffNo = request.getHeader(STAFF_NO_HEADER);
                String uri = request.getRequestURI();
                if (staffNo != null && !staffNo.trim().isEmpty() && isManagedBizUri(uri)) {
                    StaffAccessService.AccessProfile profile;
                    try {
                        profile = accessService.checkApi(staffNo.trim(), uri, LocalDateTime.now());
                    } catch (IllegalArgumentException ex) {
                        writeForbidden(response, ex.getMessage());
                        return;
                    }
                    if (!profile.isAccessActive()) {
                        writeForbidden(response,
                                profile.getDenyReason() == null ? "人员无该接口操作权限" : profile.getDenyReason());
                        return;
                    }
                }
                chain.doFilter(request, response);
            }
        };
        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/api/aqua/*");
        registration.setName("staffAccessFilter");
        registration.setOrder(20);
        return registration;
    }

    /** 仅拦截业务作业面；资质授权管理面、登录、健康检查不做现场头裁决。 */
    private boolean isManagedBizUri(String uri) {
        if (uri == null) {
            return false;
        }
        if (uri.startsWith("/api/aqua/staff")) {
            return false;
        }
        return uri.startsWith("/api/aqua/");
    }

    private void writeForbidden(HttpServletResponse response, String reason) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        String body = "{\"success\":false,\"message\":\"人员资质/岗位授权拦截："
                + reason.replace("\"", "'") + "\"}";
        response.getWriter().write(new String(body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
    }
}
