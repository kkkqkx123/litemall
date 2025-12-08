import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Java纯标准库调用LLM API示例
 * 不依赖任何外部HTTP客户端库
 */
public class PureJavaHttpExample {
    
    /**
     * 使用HttpURLConnection发送POST请求调用LLM API
     */
    public static String callLLMAPI(String apiUrl, String apiKey, String prompt) throws IOException {
        HttpURLConnection connection = null;
        
        try {
            // 创建URL对象
            URL url = new URL(apiUrl);
            connection = (HttpURLConnection) url.openConnection();
            
            // 设置请求方法
            connection.setRequestMethod("POST");
            
            // 设置请求头
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Accept", "application/json");
            
            // 启用输入输出流
            connection.setDoOutput(true);
            connection.setDoInput(true);
            
            // 设置连接超时和读取超时
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);
            
            // 构建请求体
            Map<String, Object> requestBody = Map.of(
                "model", "Qwen/Qwen3-32B",
                "messages", new Object[]{
                    Map.of("role", "user", "content", prompt)
                },
                "temperature", 0.1,
                "max_tokens", 2000
            );
            
            // 发送请求体
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = new ObjectMapper().writeValueAsString(requestBody)
                    .getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // 获取响应码
            int responseCode = connection.getResponseCode();
            
            // 读取响应
            InputStream is = responseCode >= 200 && responseCode < 300 
                ? connection.getInputStream() 
                : connection.getErrorStream();
                
            if (is == null) {
                throw new IOException("No response stream available");
            }
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line).append("\n");
                }
            }
            
            if (responseCode >= 200 && responseCode < 300) {
                return parseAPIResponse(response.toString());
            } else {
                throw new IOException("HTTP Error " + responseCode + ": " + response.toString());
            }
            
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * 解析API响应
     */
    private static String parseAPIResponse(String responseJson) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> response = mapper.readValue(responseJson, Map.class);
        
        // 检查是否有错误
        if (response.containsKey("error")) {
            Map<String, Object> error = (Map<String, Object>) response.get("error");
            String errorMessage = (String) error.get("message");
            throw new IOException("API Error: " + errorMessage);
        }
        
        // 解析choices数组
        if (response.containsKey("choices")) {
            java.util.List<Map<String, Object>> choices = 
                (java.util.List<Map<String, Object>>) response.get("choices");
            
            if (!choices.isEmpty()) {
                Map<String, Object> firstChoice = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                return (String) message.get("content");
            }
        }
        
        throw new IOException("Invalid response format: " + responseJson);
    }
    
    public static void main(String[] args) {
        String apiUrl = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
        String apiKey = "your-api-key";
        String prompt = "请用JSON格式回答：{\"query_type\": \"price_range\", \"conditions\": {\"min_price\": 100, \"max_price\": 500}}";
        
        try {
            String response = callLLMAPI(apiUrl, apiKey, prompt);
            System.out.println("API Response: " + response);
        } catch (IOException e) {
            System.err.println("API call failed: " + e.getMessage());
        }
    }
}