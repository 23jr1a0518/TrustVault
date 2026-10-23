package com.trustvault.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustvault.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public CustomAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String requestId = (String) request.getAttribute(CorrelationIdFilter.REQUEST_ID_ATTR);
        ApiErrorResponse error = new ApiErrorResponse(
            HttpServletResponse.SC_FORBIDDEN,
            "FORBIDDEN",
            "Access is denied: insufficient role privileges or CSRF token mismatch",
            request.getRequestURI(),
            requestId != null ? requestId : "unknown"
        );

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
