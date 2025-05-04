package com.spacedataarchive.validation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class ClientConnectionTrackingFilter implements Filter {
    @Autowired
    private ClientConnectionTracker tracker;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String clientIp = request instanceof HttpServletRequest ? ((HttpServletRequest) request).getRemoteAddr() : "unknown";
        tracker.clientConnected(clientIp);
        try {
            chain.doFilter(request, response);
        } finally {
            // Optionally, you can remove the client here if you want per-request tracking
            // tracker.clientDisconnected(clientIp);
        }
    }
} 