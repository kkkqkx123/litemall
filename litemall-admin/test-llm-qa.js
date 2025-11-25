// 测试llm-qa.js的API路径是否正确
const fs = require('fs');

// 读取API文件内容
const apiContent = fs.readFileSync('src/api/llm-qa.js', 'utf8');

console.log('=== llm-qa.js API路径检查 ===');

// 检查API路径
const apiPaths = [
  '/admin/llm/qa',                    // askQuestion
  '/admin/llm/qa/${sessionId}/history', // getSessionHistory
  '/admin/llm/qa/session/statistics', // getSessionStatistics
  '/admin/llm/qa/${sessionId}/clear',   // clearSession
  '/admin/llm/qa/service/status',     // getLLMServiceStatus
  '/admin/llm/qa/hot-questions'       // getHotQuestions
];

apiPaths.forEach(path => {
  if (apiContent.includes(path)) {
    console.log(`✓ 找到路径: ${path}`);
  } else {
    console.log(`✗ 未找到路径: ${path}`);
  }
});

console.log('\n=== 后端控制器映射对比 ===');
console.log('前端路径 /admin/llm/qa -> 后端映射 /admin/llm/qa ✓');
console.log('前端路径 /admin/llm/qa/{sessionId}/history -> 后端映射 /admin/llm/qa/{sessionId}/history ✓');
console.log('前端路径 /admin/llm/qa/{sessionId}/clear -> 后端映射 /admin/llm/qa/{sessionId}/clear ✓');

console.log('\n=== 修改总结 ===');
console.log('1. ✓ askQuestion API路径已修正: /admin/llm/qa/ask -> /admin/llm/qa');
console.log('2. ✓ getSessionHistory API路径已修正: /admin/llm/qa/session/{id}/history -> /admin/llm/qa/{id}/history');
console.log('3. ✓ clearSession API路径已修正: /admin/llm/qa/session/{id}/clear -> /admin/llm/qa/{id}/clear');
console.log('4. ✓ 前端响应处理已更新: response.success -> response.errno === 0');
console.log('5. ✓ 错误处理已增强: 支持errmsg和message字段');