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
     * @param body 包含问题的请求体
     * @return 问答响应
     */
    @PostMapping("/ask")
    @RequiresPermissionsDesc(menu = {"AI助手", "AI问答"}, button = "提问")
    public Object ask(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        String sessionId = body.get("sessionId");
        
        if (question == null || question.trim().isEmpty()) {
            return ResponseUtil.badArgument();
        }
        
        try {
            GoodsQARequest request = new GoodsQARequest();
            request.setQuestion(question);
            request.setSessionId(sessionId);
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
     * @param body 包含会话标题的请求体
     * @return 会话ID
     */
    @PostMapping("/session/create")
    @RequiresPermissionsDesc(menu = {"AI助手", "AI问答"}, button = "创建会话")
    public Object createSession(@RequestBody Map<String, String> body) {
        String title = body.get("title");
        if (title == null || title.trim().isEmpty()) {
            return ResponseUtil.badArgument();
        }
        
        try {
            String sessionId = llmqaService.createSession(Integer.valueOf(title));
            return ResponseUtil.ok(Map.of("sessionId", sessionId));
            
        } catch (Exception e) {
            logger.error("创建问答会话失败", e);
            return ResponseUtil.fail(500, "创建会话失败：" + e.getMessage());
        }
    }

    /**
     * 获取会话历史记录
     *
     * @param page 页码
     * @param limit 每页数量
     * @return 会话历史
     */
    @GetMapping("/session/{sessionId}/history")
    @RequiresPermissionsDesc(menu = {"AI助手", "AI问答"}, button = "历史记录")
    public Object getHistory(@PathVariable String sessionId,
                             @RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "10") Integer limit) {
        try {
            Object history = llmqaService.getSessionHistory(sessionId);
            return ResponseUtil.ok(history);
        } catch (Exception e) {
            logger.error("获取历史记录失败", e);
            return ResponseUtil.fail(503, "获取历史记录失败");
        }
    }

    /**
     * 销毁会话
     *
     * @param sessionId 会话ID
     * @return 操作结果
     */
    @DeleteMapping("/session/{sessionId}")
    @RequiresPermissionsDesc(menu = {"AI助手", "AI问答"}, button = "销毁会话")
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
    @RequiresPermissionsDesc(menu = {"AI助手", "AI问答"}, button = "服务状态")
    public Object getStatus() {
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
     * @param days 统计天数
     * @return 会话统计
     */
    @GetMapping("/session/{sessionId}/statistics")
    @RequiresPermissionsDesc(menu = {"AI助手", "AI问答"}, button = "使用统计")
    public Object getSessionStatistics(@PathVariable(required = false) String sessionId,
                                      @RequestParam(defaultValue = "7") Integer days) {
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
    @RequiresPermissionsDesc(menu = {"AI助手", "AI问答"}, button = "配置信息")
    public Object getConfig() {
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
     * @param request 包含测试问题的请求体
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