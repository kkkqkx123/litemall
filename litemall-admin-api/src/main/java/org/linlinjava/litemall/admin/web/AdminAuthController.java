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
import org.linlinjava.litemall.admin.security.JwtTokenProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private PasswordEncoder passwordEncoder;
    @Autowired
    private Producer kaptchaProducer;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

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

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
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

        // 直接进行用户认证，而不是依赖Spring Security的自动认证
        List<LitemallAdmin> adminList = adminService.findAdmin(username);
        if (adminList.isEmpty()) {
            logHelper.logAuthFail("登录", "用户帐号或密码不正确");
            return ResponseUtil.fail(ADMIN_INVALID_ACCOUNT, "用户帐号或密码不正确");
        }

        LitemallAdmin admin = adminList.get(0);
        
        // 验证密码
        if (!passwordEncoder.matches(password, admin.getPassword())) {
            logHelper.logAuthFail("登录", "用户帐号或密码不正确");
            return ResponseUtil.fail(ADMIN_INVALID_ACCOUNT, "用户帐号或密码不正确");
        }

        // 更新最后登录信息
        admin.setLastLoginIp(IpUtil.getIpAddr(request));
        admin.setLastLoginTime(LocalDateTime.now());
        adminService.updateById(admin);

        logHelper.logAuthSucceed("登录");

        // 创建认证对象并生成JWT令牌
        AdminUserDetails userDetails = new AdminUserDetails(admin, new ArrayList<>());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities());
        String token = jwtTokenProvider.generateToken(authentication);

        // userInfo
        Map<String, Object> adminInfo = new HashMap<String, Object>();
        adminInfo.put("nickName", admin.getUsername());
        adminInfo.put("avatar", admin.getAvatar());

        Map<Object, Object> result = new HashMap<Object, Object>();
        result.put("token", token);
        result.put("adminInfo", adminInfo);
        return ResponseUtil.ok(result);
    }

    /*
     *
     */
    @PostMapping("/logout")
    public Object logout() {
        // Spring Security会自动处理登出，这里只需要清除认证上下文
        SecurityContextHolder.clearContext();

        logHelper.logAuthSucceed("退出");
        return ResponseUtil.ok();
    }

    @GetMapping("/info")
    public Object info() {
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
        // 这里需要转换perms结构，因为对于前端而已API形式的权限更容易理解
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
            Set<String> systemPermissions = PermissionUtil.listPermissionString(PermissionUtil.listPermission(context, basicPackage));
            for (String perm : systemPermissions) {
                String api = perm; // 权限字符串即为API权限
                systemPermissionsMap.put(perm, api);
            }
        }

        Collection<String> apis = new HashSet<>();
        
        // 检查是否有通配符权限
        if (permissions.contains("*")) {
            // 超级管理员权限：返回所有API权限
            apis.addAll(systemPermissionsMap.values());
            // 同时添加通配符标识，确保前端能识别超级管理员
            apis.add("*");
            return apis;
        }
        
        // 普通权限：只返回对应的API权限
        for (String perm : permissions) {
            String api = systemPermissionsMap.get(perm);
            if (api != null) {
                apis.add(api);
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
