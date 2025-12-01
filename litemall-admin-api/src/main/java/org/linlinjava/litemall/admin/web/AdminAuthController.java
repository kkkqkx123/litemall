package org.linlinjava.litemall.admin.web;

import com.google.code.kaptcha.Producer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.admin.service.LogHelper;
import org.linlinjava.litemall.admin.util.Permission;
import org.linlinjava.litemall.admin.util.PermissionUtil;
import org.linlinjava.litemall.core.util.IpUtil;
import org.linlinjava.litemall.core.util.JacksonUtil;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.db.domain.LitemallAdmin;
import org.linlinjava.litemall.db.service.LitemallAdminService;
import org.linlinjava.litemall.db.service.LitemallPermissionService;
import org.linlinjava.litemall.db.service.LitemallRoleService;
import org.linlinjava.litemall.admin.security.AdminUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static org.linlinjava.litemall.admin.util.AdminResponseCode.*;

@RestController
@RequestMapping("/admin/auth")
@Validated
public class AdminAuthController {
    private final Log logger = LogFactory.getLog(AdminAuthController.class);

    @Autowired
    private LitemallAdminService adminService;
    @Autowired
    private LitemallRoleService roleService;
    @Autowired
    private LitemallPermissionService permissionService;
    @Autowired
    private LogHelper logHelper;

    @Autowired
    private Producer kaptchaProducer;

    @Autowired
    private AuthenticationManager authenticationManager;

    @GetMapping("/kaptcha")
    public Object kaptcha(HttpServletRequest request) {
        String kaptcha = doKaptcha(request);
        if (kaptcha != null) {
            return ResponseUtil.ok(kaptcha);
        }
        return ResponseUtil.fail();
    }

    private String doKaptcha(HttpServletRequest request) {
        // 生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);
        HttpSession session = request.getSession();
        session.setAttribute("kaptcha", text);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "jpeg", outputStream);
            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return "data:image/jpeg;base64," + base64.replaceAll("\r\n", "");
        } catch (IOException e) {
            return null;
        }
    }

    /*
     *  { username : value, password : value }
     */
    @PostMapping("/login")
    public Object login(@RequestBody String body, HttpServletRequest request) {
        String username = JacksonUtil.parseString(body, "username");
        String password = JacksonUtil.parseString(body, "password");
//        String code = JacksonUtil.parseString(body, "code");

        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            return ResponseUtil.badArgument();
        }
//        if (StringUtils.isEmpty(code)) {
//            return ResponseUtil.fail(ADMIN_INVALID_KAPTCHA_REQUIRED, "验证码不能空");
//        }

//        HttpSession session = request.getSession();
//        String kaptcha = (String)session.getAttribute("kaptcha");
//        if (Objects.requireNonNull(code).compareToIgnoreCase(kaptcha) != 0) {
//            return ResponseUtil.fail(ADMIN_INVALID_KAPTCHA, "验证码不正确", doKaptcha(request));
//        }

        // 手动进行认证
        try {
            // 创建用户名密码认证token
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(username, password);
            
            // 使用认证管理器进行认证
            Authentication authentication = authenticationManager.authenticate(authToken);
            
            // 将认证信息设置到安全上下文中
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 获取认证后的用户信息
            AdminUserDetails userDetails = (AdminUserDetails) authentication.getPrincipal();
            LitemallAdmin admin = userDetails.getAdmin();
            admin.setLastLoginIp(IpUtil.getIpAddr(request));
            admin.setLastLoginTime(LocalDateTime.now());
            adminService.updateById(admin);

            logHelper.logAuthSucceed("登录");

            // 使用Session进行简单的认证管理
            HttpSession session = request.getSession(true);
            session.setAttribute("admin", admin);
            session.setAttribute("username", username);

            // userInfo
            Map<String, Object> adminInfo = new HashMap<String, Object>();
            adminInfo.put("nickName", admin.getUsername());
            adminInfo.put("avatar", admin.getAvatar());

            Map<Object, Object> result = new HashMap<Object, Object>();
            result.put("token", session.getId()); // 使用Session ID作为简单的token
            result.put("adminInfo", adminInfo);
            return ResponseUtil.ok(result);
            
        } catch (BadCredentialsException e) {
            logHelper.logAuthFail("登录", "用户帐号或密码不正确");
            return ResponseUtil.fail(ADMIN_INVALID_ACCOUNT, "用户帐号或密码不正确", doKaptcha(request));
        } catch (AuthenticationException e) {
            logHelper.logAuthFail("登录", "认证失败");
            return ResponseUtil.fail(ADMIN_INVALID_ACCOUNT, "用户帐号或密码不正确", doKaptcha(request));
        }
    }

    /*
     *
     */
    @PostMapping("/logout")
    public Object logout(HttpServletRequest request) {
        // 清除Session
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        
        // 清除安全上下文
        SecurityContextHolder.clearContext();

        logHelper.logAuthSucceed("退出");
        return ResponseUtil.ok();
    }


    @GetMapping("/info")
    public Object info(HttpServletRequest request) {
        // 首先尝试从Session获取用户信息
        HttpSession session = request.getSession(false);
        if (session != null) {
            LitemallAdmin admin = (LitemallAdmin) session.getAttribute("admin");
            if (admin != null) {
                Map<String, Object> data = new HashMap<>();
                data.put("name", admin.getUsername());
                data.put("avatar", admin.getAvatar());

                Integer[] roleIds = admin.getRoleIds();
                Set<String> roles = roleService.queryByIds(roleIds);
                Set<String> permissions = permissionService.queryByRoleIds(roleIds);
                data.put("roles", roles);
                // NOTE
                // 这里需要转换perms结构，因为对于前端来说API形式的权限更容易理解
                data.put("perms", toApi(permissions));
                return ResponseUtil.ok(data);
            }
        }
        
        // 如果Session中没有，尝试从SecurityContext获取
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseUtil.unlogin();
        }

        AdminUserDetails userDetails = (AdminUserDetails) authentication.getPrincipal();
        LitemallAdmin admin = userDetails.getAdmin();

        Map<String, Object> data = new HashMap<>();
        data.put("name", admin.getUsername());
        data.put("avatar", admin.getAvatar());

        Integer[] roleIds = admin.getRoleIds();
        Set<String> roles = roleService.queryByIds(roleIds);
        Set<String> permissions = permissionService.queryByRoleIds(roleIds);
        data.put("roles", roles);
        // NOTE
        // 这里需要转换perms结构，因为对于前端来说API形式的权限更容易理解
        data.put("perms", toApi(permissions));
        return ResponseUtil.ok(data);
    }

    @Autowired
    private ApplicationContext context;
    private HashMap<String, String> systemPermissionsMap = null;

    private Collection<String> toApi(Set<String> permissions) {
        if (systemPermissionsMap == null) {
            systemPermissionsMap = new HashMap<>();
            final String basicPackage = "org.linlinjava.litemall.admin";
            List<Permission> systemPermissions = PermissionUtil.listPermission(context, basicPackage);
            for (Permission permission : systemPermissions) {
                String perm = permission.getRequiresPermissions().value()[0];
                String api = permission.getApi();
                systemPermissionsMap.put(perm, api);
            }
        }

        Collection<String> apis = new HashSet<>();
        for (String perm : permissions) {
            String api = systemPermissionsMap.get(perm);
            apis.add(api);

            if (perm.equals("*")) {
                apis.clear();
                apis.add("*");
                return apis;
                //                return systemPermissionsMap.values();

            }
        }
        return apis;
    }

    @GetMapping("/401")
    public Object page401() {
        return ResponseUtil.unlogin();
    }

    @GetMapping("/index")
    public Object pageIndex() {
        return ResponseUtil.ok();
    }

    @GetMapping("/403")
    public Object page403() {
        return ResponseUtil.unauthz();
    }
}
