package org.linlinjava.litemall.admin.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.admin.annotation.RequiresPermissions;
import org.linlinjava.litemall.admin.annotation.RequiresPermissionsDesc;
import org.linlinjava.litemall.core.llm.model.GoodsQARequest;
import org.linlinjava.litemall.core.llm.model.GoodsQAResponse;
import org.linlinjava.litemall.core.llm.service.LLMQAService;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 大模型商品问答管理控制器
 * 提供商品问答功能的后台管理接口
 */
@RestController
@RequestMapping("/admin/llm/qa")
public class AdminLLMQAController {

    private final Log logger = LogFactory.getLog(AdminLLMQAController.class);

    @Autowired
    private LLMQAService llmqaService;

    /**
     * 处理商品问答请求
     *
     * @param request 问答请求
     * @return 问答响应
     */
    @PostMapping("")
    @PreAuthorize("hasAuthority('admin:llm:qa:ask')")
    @RequiresPermissions("admin:llm:qa:ask")
    @RequiresPermissionsDesc(menu = {"智能问答", "商品问答"}, button = "提问")
    public Object askQuestion(@Valid @RequestBody GoodsQARequest request) {
        try {
            // 处理问答请求
            GoodsQAResponse response = llmqaService.processQuestion(request);
            
            if (response.getCode() == 200) {
                return ResponseUtil.ok(response);
            } else {
                return ResponseUtil.fail(response.getCode(), response.getMessage());
            }
            
        } catch (Exception e) {
            logger.error("处理商品问答请求失败", e);
            return ResponseUtil.fail(500, "处理请求时发生错误：" + e.getMessage());
        }
    }

    /**
     * 创建新的问答会话
     *
     * @param userId 用户ID（可选）
     * @return 会话ID
     */
    @PostMapping("/session")
    @PreAuthorize("hasAuthority('admin:llm:qa:createSession')")
    @RequiresPermissions("admin:llm:qa:createSession")
    @RequiresPermissionsDesc(menu = {"智能问答", "商品问答"}, button = "创建会话")
    public Object createSession(@RequestParam(value = "userId", required = false) Integer userId) {
        try {
            String sessionId = llmqaService.createSession(userId);
            return ResponseUtil.ok(Map.of("sessionId", sessionId));
            
        } catch (Exception e) {
            logger.error("创建问答会话失败", e);
            return ResponseUtil.fail(500, "创建会话失败：" + e.getMessage());
        }
    }

    /**
     * 获取会话历史记录
     *
     * @param sessionId 会话ID
     * @return 会话历史
     */
    @GetMapping("/session/{sessionId}/history")
    @PreAuthorize("hasAuthority('admin:llm:qa:history')")
    @RequiresPermissions("admin:llm:qa:history")
    @RequiresPermissionsDesc(menu = {"智能问答", "商品问答"}, button = "查看历史")
    public Object getSessionHistory(@PathVariable String sessionId) {
        try {
            Object history = llmqaService.getSessionHistory(sessionId);
            return ResponseUtil.ok(history);
            
        } catch (Exception e) {
            logger.error("获取会话历史失败", e);
            return ResponseUtil.fail(500, "获取历史记录失败：" + e.getMessage());
        }
    }

    /**
     * 销毁会话
     *
     * @param sessionId 会话ID
     * @return 操作结果
     */
    @DeleteMapping("/session/{sessionId}")
    @PreAuthorize("hasAuthority('admin:llm:qa:destroySession')")
    @RequiresPermissions("admin:llm:qa:destroySession")
    @RequiresPermissionsDesc(menu = {"智能问答", "商品问答"}, button = "销毁会话")
    public Object destroySession(@PathVariable String sessionId) {
        try {
            llmqaService.destroySession(sessionId);
            return ResponseUtil.ok();
            
        } catch (Exception e) {
            logger.error("销毁会话失败", e);
            return ResponseUtil.fail(500, "销毁会话失败：" + e.getMessage());
        }
    }

    /**
     * 获取服务状态信息
     *
     * @return 服务状态
     */
    @GetMapping("/status")
    @PreAuthorize("hasAuthority('admin:llm:qa:status')")
    @RequiresPermissions("admin:llm:qa:status")
    @RequiresPermissionsDesc(menu = {"智能问答", "商品问答"}, button = "查看状态")
    public Object getServiceStatus() {
        try {
            Map<String, Object> status = llmqaService.getServiceStatus();
            return ResponseUtil.ok(status);
            
        } catch (Exception e) {
            logger.error("获取服务状态失败", e);
            return ResponseUtil.fail(500, "获取服务状态失败：" + e.getMessage());
        }
    }

    /**
     * 获取会话统计信息
     *
     * @return 会话统计
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('admin:llm:qa:statistics')")
    @RequiresPermissions("admin:llm:qa:statistics")
    @RequiresPermissionsDesc(menu = {"智能问答", "商品问答"}, button = "查看统计")
    public Object getSessionStatistics() {
        try {
            Map<String, Object> statistics = llmqaService.getSessionStatistics();
            return ResponseUtil.ok(statistics);
            
        } catch (Exception e) {
            logger.error("获取会话统计失败", e);
            return ResponseUtil.fail(500, "获取统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 调试接口 - 获取LLM服务配置信息
     * 用于调试API调用问题
     *
     * @return 服务配置信息
     */
    @GetMapping("/debug/config")
    @PreAuthorize("hasAuthority('admin:llm:qa:status')")
    @RequiresPermissions("admin:llm:qa:status")
    @RequiresPermissionsDesc(menu = {"智能问答", "商品问答"}, button = "调试配置")
    public Object getDebugConfig() {
        try {
            Map<String, Object> status = llmqaService.getServiceStatus();
            
            // 添加额外的调试信息
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("serviceStatus", status);
            debugInfo.put("timestamp", System.currentTimeMillis());
            debugInfo.put("systemProperties", Map.of(
                "java.version", System.getProperty("java.version"),
                "os.name", System.getProperty("os.name"),
                "user.timezone", System.getProperty("user.timezone")
            ));
            
            return ResponseUtil.ok(debugInfo);
            
        } catch (Exception e) {
            logger.error("获取调试配置失败", e);
            return ResponseUtil.fail(500, "获取调试配置失败：" + e.getMessage());
        }
    }

    /**
     * 调试接口 - 测试LLM API调用
     * 用于调试实际的API调用过程
     *
     * @param testQuestion 测试问题
     * @return 详细的调试信息
     */
    @PostMapping("/debug/test-call")
    public Object testLLMApiCall(@RequestBody Map<String, String> request) {
        try {
            String testQuestion = request.get("question");
            if (testQuestion == null || testQuestion.trim().isEmpty()) {
                return ResponseUtil.fail(400, "测试问题不能为空");
            }
            
            // 创建测试请求
            GoodsQARequest qaRequest = new GoodsQARequest();
            qaRequest.setQuestion(testQuestion);
            qaRequest.setSessionId("debug-session-" + System.currentTimeMillis());
            
            // 调用服务并捕获详细日志
            logger.info("=== 开始调试LLM API调用 ===");
            logger.info("测试问题：" + testQuestion);
            
            GoodsQAResponse response = llmqaService.processQuestion(qaRequest);
            
            logger.info("调用LLM服务成功，回答：" + response.getAnswer());
            
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("success", response.getCode() == 200);
            debugInfo.put("answer", response.getAnswer());
            debugInfo.put("goodsCount", response.getGoods() != null ? response.getGoods().size() : 0);
            debugInfo.put("sessionId", response.getSessionId());
            debugInfo.put("queryIntent", response.getQueryIntent());
            debugInfo.put("queryTime", response.getQueryTime());
            
            return ResponseUtil.ok(debugInfo);
            
        } catch (Exception e) {
            logger.error("调试API调用失败", e);
            
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getMessage());
            errorInfo.put("errorType", e.getClass().getSimpleName());
            errorInfo.put("stackTrace", Arrays.toString(e.getStackTrace()).substring(0, 1000));
            
            return ResponseUtil.fail(500, "调试API调用失败：" + e.getMessage());
        }
    }
}