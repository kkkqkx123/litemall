package org.linlinjava.litemall.admin.config;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.Collection;

@Component
@Order(1)
public class CharsetResponseFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // 设置请求字符编码
        httpRequest.setCharacterEncoding("UTF-8");
        
        // 使用包装器来确保响应包含charset
        CharsetResponseWrapper wrappedResponse = new CharsetResponseWrapper(httpResponse);
        
        chain.doFilter(httpRequest, wrappedResponse);
        
        // 确保Content-Type包含charset
        String contentType = wrappedResponse.getContentType();
        if (contentType != null && (contentType.contains("application/json") || contentType.contains("text/"))) {
            if (!contentType.contains("charset")) {
                wrappedResponse.setContentType(contentType + ";charset=UTF-8");
            }
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void destroy() {}

    private static class CharsetResponseWrapper extends HttpServletResponseWrapper {
        public CharsetResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setContentType(String type) {
            if (type != null && (type.contains("application/json") || type.contains("text/"))) {
                if (!type.contains("charset")) {
                    type = type + ";charset=UTF-8";
                }
            }
            super.setContentType(type);
        }

        @Override
        public void setHeader(String name, String value) {
            if ("Content-Type".equalsIgnoreCase(name) && value != null) {
                if ((value.contains("application/json") || value.contains("text/")) && !value.contains("charset")) {
                    value = value + ";charset=UTF-8";
                }
            }
            super.setHeader(name, value);
        }

        @Override
        public void addHeader(String name, String value) {
            if ("Content-Type".equalsIgnoreCase(name) && value != null) {
                if ((value.contains("application/json") || value.contains("text/")) && !value.contains("charset")) {
                    value = value + ";charset=UTF-8";
                }
            }
            super.addHeader(name, value);
        }
    }
}