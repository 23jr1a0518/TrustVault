package com.trustvault.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustvault.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public CustomAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String requestId = (String) request.getAttribute(CorrelationIdFilter.REQUEST_ID_ATTR);
        ApiErrorResponse error = new ApiErrorResponse(
            HttpServletResponse.SC_UNAUTHORIZED,
            "UNAUTHORIZED",
            "Full authentication is required to access this resource",
            request.getRequestURI(),
            requestId != null ? requestId : "unknown"
        );

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
