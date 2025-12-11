package org.linlinjava.litemall.core.llm.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Qwen3Service HTTP请求捕获测试
 * 用于比较成功的curl请求和Java实现的差异
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class Qwen3ServiceHttpCaptureTest {

    @LocalServerPort
    private int port;

    @Autowired
    private Qwen3Service qwen3Service;

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * 测试直接调用Qwen3 API，捕获HTTP请求和响应
     */
    @Test
    public void testDirectApiCallWithHttpCapture() {
        // 准备测试数据
        String testPrompt = "你好，请简单介绍一下你自己";
        
        try {
            // 手动构建和发送请求，捕获详细信息
            String apiUrl = "https://api-inference.modelscope.cn/v1/chat/completions";
            String apiKey = "Bearer YOUR_API_KEY"; // 替换为实际的API密钥
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "Qwen/Qwen3-32B");
            
            // 构建消息列表
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", testPrompt);
            messages.add(userMessage);
            requestBody.put("messages", messages);
            
            requestBody.put("max_tokens", 100);
            requestBody.put("temperature", 0.7);
            requestBody.put("enable_thinking", false);
            
            // 序列化为JSON
            String requestBodyJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(requestBody);
            System.out.println("=== 手动构建的请求体 ===");
            System.out.println("JSON字符串：" + requestBodyJson);
            System.out.println("JSON长度：" + requestBodyJson.length());
            
            // 使用RestTemplate发送请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", apiKey);
            
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBodyJson, headers);
            
            System.out.println("=== 请求头信息 ===");
            System.out.println("Content-Type: " + headers.getContentType());
            System.out.println("Authorization: " + headers.get("Authorization"));
            
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl, 
                HttpMethod.POST, 
                requestEntity, 
                String.class
            );
            
            System.out.println("=== 响应信息 ===");
            System.out.println("状态码：" + response.getStatusCode());
            System.out.println("响应体：" + response.getBody());
            
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            
        } catch (Exception e) {
            System.err.println("测试失败：" + e.getMessage());
            e.printStackTrace();
            fail("API调用失败：" + e.getMessage());
        }
    }

    /**
     * 测试使用字符串作为请求体
     */
    @Test
    public void testStringRequestBody() {
        try {
            String apiUrl = "https://api-inference.modelscope.cn/v1/chat/completions";
            String apiKey = "Bearer YOUR_API_KEY"; // 替换为实际的API密钥
            
            // 直接使用字符串作为请求体
            String requestBodyJson = "{"
                + "\"model\":\"Qwen/Qwen3-32B\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\"你好，请简单介绍一下你自己\"}],"
                + "\"max_tokens\":100,"
                + "\"temperature\":0.7,"
                + "\"enable_thinking\":false"
                + "}";
            
            System.out.println("=== 字符串请求体 ===");
            System.out.println(requestBodyJson);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", apiKey);
            
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBodyJson, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl, 
                HttpMethod.POST, 
                requestEntity, 
                String.class
            );
            
            System.out.println("状态码：" + response.getStatusCode());
            System.out.println("响应体：" + response.getBody());
            
            assertEquals(HttpStatus.OK, response.getStatusCode());
            
        } catch (Exception e) {
            System.err.println("字符串请求体测试失败：" + e.getMessage());
            e.printStackTrace();
            fail("字符串请求体测试失败：" + e.getMessage());
        }
    }

    /**
     * 模拟成功的curl命令的Java实现
     */
    @Test
    public void testCurlEquivalent() {
        try {
            // 模拟curl命令：
            // curl -X POST "https://api-inference.modelscope.cn/v1/chat/completions" \
            //   -H "Content-Type: application/json" \
            //   -H "Authorization: Bearer YOUR_API_KEY" \
            //   -d '{
            //     "model": "Qwen/Qwen3-32B",
            //     "messages": [{"role": "user", "content": "你好，请简单介绍一下你自己"}],
            //     "max_tokens": 100,
            //     "temperature": 0.7,
            //     "enable_thinking": false
            //   }'
            
            String apiUrl = "https://api-inference.modelscope.cn/v1/chat/completions";
            String apiKey = "Bearer YOUR_API_KEY"; // 替换为实际的API密钥
            
            // 使用LinkedMultiValueMap模拟curl的表单数据
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("model", "Qwen/Qwen3-32B");
            formData.add("messages", "[{\"role\":\"user\",\"content\":\"你好，请简单介绍一下你自己\"}]");
            formData.add("max_tokens", "100");
            formData.add("temperature", "0.7");
            formData.add("enable_thinking", "false");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", apiKey);
            
            // 使用MultiValueMap作为请求体
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl, 
                HttpMethod.POST, 
                requestEntity, 
                String.class
            );
            
            System.out.println("=== curl等效测试 ===");
            System.out.println("状态码：" + response.getStatusCode());
            System.out.println("响应体：" + response.getBody());
            
            assertEquals(HttpStatus.OK, response.getStatusCode());
            
        } catch (Exception e) {
            System.err.println("curl等效测试失败：" + e.getMessage());
            e.printStackTrace();
            fail("curl等效测试失败：" + e.getMessage());
        }
    }

    /**
     * 测试比较Map和字符串序列化的差异
     */
    @Test
    public void testSerializationDifference() {
        try {
            // 构建Map请求体
            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("model", "Qwen/Qwen3-32B");
            
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", "你好，请简单介绍一下你自己");
            messages.add(userMessage);
            requestBodyMap.put("messages", messages);
            
            requestBodyMap.put("max_tokens", 100);
            requestBodyMap.put("temperature", 0.7);
            requestBodyMap.put("enable_thinking", false);
            
            // Map序列化
            String mapJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(requestBodyMap);
            
            // 手动构建的字符串
            String manualJson = "{"
                + "\"model\":\"Qwen/Qwen3-32B\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\"你好，请简单介绍一下你自己\"}],"
                + "\"max_tokens\":100,"
                + "\"temperature\":0.7,"
                + "\"enable_thinking\":false"
                + "}";
            
            System.out.println("=== 序列化比较 ===");
            System.out.println("Map序列化结果：" + mapJson);
            System.out.println("手动构建结果：" + manualJson);
            System.out.println("是否相等：" + mapJson.equals(manualJson));
            
            // 检查messages字段的具体内容
            System.out.println("=== messages字段详细比较 ===");
            System.out.println("Map中的messages：" + requestBodyMap.get("messages"));
            System.out.println("Map中的messages类型：" + requestBodyMap.get("messages").getClass().getName());
            
            // 比较字符级别的差异
            if (!mapJson.equals(manualJson)) {
                System.out.println("字符级别差异：");
                for (int i = 0; i < Math.min(mapJson.length(), manualJson.length()); i++) {
                    if (mapJson.charAt(i) != manualJson.charAt(i)) {
                        System.out.println("位置 " + i + ": Map='" + mapJson.charAt(i) + "', Manual='" + manualJson.charAt(i) + "'");
                        break;
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("序列化比较测试失败：" + e.getMessage());
            e.printStackTrace();
            fail("序列化比较测试失败：" + e.getMessage());
        }
    }
}