package org.linlinjava.litemall.admin.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    static {
        logger.info("*** JWT FILTER CLASS LOADED ***");
    }

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private AdminUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        System.out.println("🚀 JWT FILTER WAS CALLED FOR URI: " + request.getRequestURI());
        logger.info("*** JWT FILTER WAS CALLED FOR URI: " + request.getRequestURI() + " ***");
        
        logger.info("*** ABOUT TO ENTER TRY BLOCK ***");
        try {
            logger.info("*** INSIDE TRY BLOCK ***");
            // 跳过LLM调试接口的JWT验证
            String requestURI = request.getRequestURI();
            String contextPath = request.getContextPath();
            logger.info("*** CHECKING URI FOR SKIP: " + requestURI + " ***");
            logger.info("*** CONTEXT PATH: " + contextPath + " ***");
            logger.info("*** REQUEST URL: " + request.getRequestURL() + " ***");
            logger.info("*** SERVLET PATH: " + request.getServletPath() + " ***");
            
            // 检查多种可能的URI格式
            if (requestURI != null) {
                logger.info("*** URI CHECK: equals('/admin/llm/debug/test-call') = " + requestURI.equals("/admin/llm/debug/test-call"));
                logger.info("*** URI CHECK: contains('/admin/llm/debug/test-call') = " + requestURI.contains("/admin/llm/debug/test-call"));
                logger.info("*** URI CHECK: endsWith('/test-call') = " + requestURI.endsWith("/test-call"));
            }
            
            if (requestURI != null && requestURI.equals("/admin/llm/debug/test-call")) {
                logger.info("*** SKIPPING JWT FILTER FOR LLM DEBUG ENDPOINT ***");
                // 确保SecurityContext中有一个匿名认证，避免后续的权限检查失败
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 创建匿名认证对象
                    UsernamePasswordAuthenticationToken anonymousAuth = 
                        new UsernamePasswordAuthenticationToken("anonymous", null, 
                            java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ANONYMOUS")));
                    SecurityContextHolder.getContext().setAuthentication(anonymousAuth);
                    logger.info("*** SET ANONYMOUS AUTHENTICATION FOR LLM DEBUG ENDPOINT ***");
                }
                filterChain.doFilter(request, response);
                return;
            }
        } catch (Exception e) {
            logger.error("*** ERROR IN URI CHECK: " + e.getMessage(), e);
        }
        
        // 使用logger.info确保输出可见
        logger.info("*** JWT FILTER CALLED *** URI: " + request.getRequestURI());
        logger.info("*** Authorization: " + request.getHeader("Authorization"));
        logger.info("*** X-Litemall-Admin-Token: " + request.getHeader("X-Litemall-Admin-Token"));
        
        try {
            String jwt = getJwtFromRequest(request);
            logger.info("*** JWT EXTRACTED: " + (jwt != null ? (jwt.length() > 20 ? jwt.substring(0, 20) + "..." : jwt) : "null"));

            if (StringUtils.hasText(jwt)) {
                logger.info("*** JWT VALIDATION START ***");
                boolean isValid = tokenProvider.validateToken(jwt);
                logger.info("*** VALIDATE TOKEN RESULT: " + isValid + " ***");
                
                if (isValid) {
                    logger.info("*** JWT VALIDATION SUCCESSFUL ***");
                    String username = tokenProvider.getUsernameFromToken(jwt);
                    logger.info("*** USERNAME FROM TOKEN: " + username + " ***");

                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    logger.info("*** USER DETAILS LOADED: " + userDetails.getUsername() + " ***");
                    logger.info("*** USER AUTHORITIES: " + userDetails.getAuthorities() + " ***");

                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.info("*** AUTHENTICATION SET IN SECURITY CONTEXT ***");
                    logger.info("*** CURRENT AUTHENTICATION: " + SecurityContextHolder.getContext().getAuthentication() + " ***");
                } else {
                    logger.info("*** JWT VALIDATION FAILED ***");
                    // 尝试获取更多错误信息
                    try {
                        String username = tokenProvider.getUsernameFromToken(jwt);
                        logger.info("*** USERNAME FROM INVALID TOKEN: " + username + " ***");
                    } catch (Exception e) {
                        logger.error("*** ERROR GETTING USERNAME FROM INVALID TOKEN: " + e.getMessage(), e);
                    }
                }
            } else {
                logger.info("*** NO JWT FOUND ***");
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        logger.info("*** PARSING JWT START ***");
        
        // 首先检查 Authorization 头部
        String bearerToken = request.getHeader("Authorization");
        logger.info("*** Authorization: " + bearerToken);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            logger.info("*** FROM AUTH: " + (token.length() > 20 ? token.substring(0, 20) + "..." : token));
            return token;
        }
        
        // 如果 Authorization 头部没有找到，再检查 X-Litemall-Admin-Token 头部
        String customToken = request.getHeader("X-Litemall-Admin-Token");
        logger.info("*** X-Litemall-Admin-Token: " + customToken);
        if (StringUtils.hasText(customToken)) {
            logger.info("*** FROM CUSTOM: " + (customToken.length() > 20 ? customToken.substring(0, 20) + "..." : customToken));
            return customToken;
        }
        
        logger.info("*** NO JWT FOUND ***");
        return null;
    }
}