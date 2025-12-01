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
        
        // 使用logger.info确保输出可见
        logger.info("*** JWT FILTER CALLED *** URI: " + request.getRequestURI());
        logger.info("*** Authorization: " + request.getHeader("Authorization"));
        logger.info("*** X-Litemall-Admin-Token: " + request.getHeader("X-Litemall-Admin-Token"));
        
        try {
            String jwt = getJwtFromRequest(request);
            logger.info("*** JWT EXTRACTED: " + (jwt != null ? (jwt.length() > 20 ? jwt.substring(0, 20) + "..." : jwt) : "null"));

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromToken(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
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