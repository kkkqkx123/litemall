package org.linlinjava.litemall.admin.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.core.llm.model.GoodsQARequest;
import org.linlinjava.litemall.core.llm.model.GoodsQAResponse;
import org.linlinjava.litemall.core.llm.service.LLMQAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
    public Map<String, Object> ask(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        String sessionId = body.get("sessionId");
        
        if (question == null || question.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("errno", 400);
            errorResponse.put("errmsg", "问题不能为空");
            errorResponse.put("data", null);
            return errorResponse;
        }
        
        try {
            GoodsQARequest request = new GoodsQARequest();
            request.setQuestion(question);
            request.setSessionId(sessionId);
            GoodsQAResponse response = llmqaService.processQuestion(request);
            
            // 检查服务响应是否成功
            if (response.getErrno() != 0) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("errno", response.getErrno());
                errorResponse.put("errmsg", response.getErrmsg());
                errorResponse.put("data", null);
                return errorResponse;
            }
            
            // 直接返回业务数据，不再嵌套
            Map<String, Object> result = new HashMap<>();
            result.put("errno", 0);
            result.put("errmsg", "success");
            result.put("data", Map.of(
                "answer", response.getAnswer(),
                "goods", response.getGoods() != null ? response.getGoods() : List.of(),
                "sessionId", response.getSessionId(),
                "queryTime", response.getQueryTime(),
                "fromCache", response.isFromCache(),
                "timestamp", response.getTimestamp()
            ));
            return result;
            
        } catch (Exception e) {
            logger.error("处理商品问答请求失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("errno", 502);
            errorResponse.put("errmsg", "处理请求时发生错误：" + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }

    /**
     * 创建新的问答会话
     *
     * @param body 包含用户ID的请求体
     * @return 会话ID
     */
    @PostMapping("/session/create")
    public Map<String, Object> createSession(@RequestBody Map<String, String> body) {
        String userIdStr = body.get("userId");
        if (userIdStr == null || userIdStr.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("errno", 400);
            errorResponse.put("errmsg", "用户ID不能为空");
            return errorResponse;
        }
        
        try {
            Integer userId = Integer.valueOf(userIdStr);
            String sessionId = llmqaService.createSession(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("errno", 0);
            response.put("errmsg", "会话创建成功");
            response.put("data", Map.of("sessionId", sessionId));
            return response;
            
        } catch (NumberFormatException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("errno", 400);
            errorResponse.put("errmsg", "用户ID格式不正确");
            return errorResponse;
        } catch (Exception e) {
            logger.error("创建问答会话失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("errno", 502);
            errorResponse.put("errmsg", "创建会话失败：" + e.getMessage());
            return errorResponse;
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
    public Map<String, Object> getHistory(@PathVariable String sessionId,
                             @RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "10") Integer limit) {
        try {
            Object history = llmqaService.getSessionHistory(sessionId);
            Map<String, Object> response = new HashMap<>();
            response.put("errno", 0);
            response.put("errmsg", "获取历史记录成功");
            response.put("data", history);
            return response;
        } catch (Exception e) {
            logger.error("获取历史记录失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("errno", 503);
            errorResponse.put("errmsg", "获取历史记录失败");
            return errorResponse;
        }
    }

    /**
     * 销毁会话
     *
     * @param sessionId 会话ID
     * @return 操作结果
     */
    @DeleteMapping("/session/{sessionId}")
    public Map<String, Object> destroySession(@PathVariable String sessionId) {
        try {
            llmqaService.destroySession(sessionId);
            Map<String, Object> response = new HashMap<>();
            response.put("errno", 0);
            response.put("errmsg", "会话销毁成功");
            response.put("data", null);
            return response;
            
        } catch (Exception e) {
            logger.error("销毁会话失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("errno", 502);
            errorResponse.put("errmsg", "销毁会话失败：" + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * 获取服务状态信息
     *
     * @return 服务状态
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        try {
            Map<String, Object> status = llmqaService.getServiceStatus();
            Map<String, Object> response = new HashMap<>();
            response.put("errno", 0);
            response.put("errmsg", "获取服务状态成功");
            response.put("data", status);
            return response;
            
        } catch (Exception e) {
            logger.error("获取服务状态失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("errno", 502);
            errorResponse.put("errmsg", "获取服务状态失败：" + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * 获取会话统计信息
     *
     * @param days 统计天数
     * @return 会话统计
     */
    @GetMapping("/session/{sessionId}/statistics")
    public Map<String, Object> getSessionStatistics(@PathVariable(required = false) String sessionId,
                                      @RequestParam(defaultValue = "7") Integer days) {
        try {
            Map<String, Object> statistics = llmqaService.getSessionStatistics();
            Map<String, Object> response = new HashMap<>();
            response.put("errno", 0);
            response.put("errmsg", "获取统计信息成功");
            response.put("data", statistics);
            return response;
            
        } catch (Exception e) {
            logger.error("获取会话统计失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("errno", 502);
            errorResponse.put("errmsg", "获取统计信息失败：" + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * 调试接口 - 获取LLM服务配置信息
     * 用于调试API调用问题
     *
     * @return 服务配置信息
     */
    @GetMapping("/debug/config")
    public Map<String, Object> getConfig() {
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
            
            Map<String, Object> response = new HashMap<>();
            response.put("errno", 0);
            response.put("errmsg", "获取调试配置成功");
            response.put("data", debugInfo);
            return response;
            
        } catch (Exception e) {
            logger.error("获取调试配置失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("errno", 502);
            errorResponse.put("errmsg", "获取调试配置失败：" + e.getMessage());
            return errorResponse;
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
    public Map<String, Object> testLLMApiCall(@RequestBody Map<String, String> request) {
        try {
            String testQuestion = request.get("question");
            if (testQuestion == null || testQuestion.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("errno", 400);
                errorResponse.put("errmsg", "测试问题不能为空");
                return errorResponse;
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
            debugInfo.put("success", response.getErrno() == 0);
            debugInfo.put("answer", response.getAnswer());
            debugInfo.put("goodsCount", response.getGoods() != null ? response.getGoods().size() : 0);
            debugInfo.put("sessionId", response.getSessionId());
            debugInfo.put("queryIntent", response.getQueryIntent());
            debugInfo.put("queryTime", response.getQueryTime());
            
            Map<String, Object> result = new HashMap<>();
            result.put("errno", 0);
            result.put("errmsg", "调试API调用成功");
            result.put("data", debugInfo);
            return result;
            
        } catch (Exception e) {
            logger.error("调试API调用失败", e);
            
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getMessage());
            errorInfo.put("errorType", e.getClass().getSimpleName());
            errorInfo.put("stackTrace", Arrays.toString(e.getStackTrace()).substring(0, 1000));
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("errno", 502);
            errorResponse.put("errmsg", "调试API调用失败：" + e.getMessage());
            errorResponse.put("data", errorInfo);
            return errorResponse;
        }
    }


}