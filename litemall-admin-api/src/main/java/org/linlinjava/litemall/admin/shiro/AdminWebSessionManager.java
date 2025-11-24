package org.linlinjava.litemall.admin.shiro;

import com.alibaba.druid.util.StringUtils;
import org.apache.shiro.web.servlet.ShiroHttpServletRequest;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.apache.shiro.web.util.WebUtils;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.Serializable;

public class AdminWebSessionManager extends DefaultWebSessionManager {

    public static final String LOGIN_TOKEN_KEY = "X-Litemall-Admin-Token";
    private static final String REFERENCED_SESSION_ID_SOURCE = "Stateless request";


    public AdminWebSessionManager() {
        super();
        setGlobalSessionTimeout(MILLIS_PER_HOUR * 6);
//        setSessionIdCookieEnabled(false);
        setSessionIdUrlRewritingEnabled(false);
    }

    @Override
    protected Serializable getSessionId(ServletRequest request, ServletResponse response) {
        String id = WebUtils.toHttp(request).getHeader(LOGIN_TOKEN_KEY);
        if (!StringUtils.isEmpty(id)) {
            request.setAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_ID_SOURCE, REFERENCED_SESSION_ID_SOURCE);
            request.setAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_ID, id);
            request.setAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_ID_IS_VALID, Boolean.TRUE);
            // 参考 https://blog.csdn.net/narutots/article/details/120363843
            request.setAttribute(ShiroHttpServletRequest.SESSION_ID_URL_REWRITING_ENABLED, this.isSessionIdUrlRewritingEnabled());
            return id;
        } else {
            // 修复首次登录时会话ID获取问题
            // 当没有自定义token时，确保能够正常创建新的会话
            Serializable sessionId = super.getSessionId(request, response);
            if (sessionId == null) {
                // 如果父类也无法获取会话ID，则直接返回null，让Shiro自动创建新会话
                // Shiro框架会在需要时自动创建新的会话
                return null;
            }
            return sessionId;
        }
    }
}
