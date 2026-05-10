package com.cobre.notification.adapter.in.rest;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            String correlationId = Optional
                    .ofNullable(((HttpServletRequest) request).getHeader("X-Correlation-Id"))
                    .orElse(UUID.randomUUID().toString());

            MDC.put("correlationId", correlationId);
            MDC.put("service", "notification-service");

            ((HttpServletResponse) response).setHeader("X-Correlation-Id", correlationId);

            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
