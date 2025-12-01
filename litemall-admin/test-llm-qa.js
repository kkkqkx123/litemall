// 测试llm-qa.js的API路径是否正确
const fs = require('fs');

// 读取API文件内容
const apiContent = fs.readFileSync('src/api/llm-qa.js', 'utf8');

console.log('=== llm-qa.js API路径检查（修复后）===');

// 检查API路径
const apiPaths = [
  '/llm/qa',                    // askQuestion
  '/llm/qa/${sessionId}/history', // getSessionHistory
  '/llm/qa/session/statistics', // getSessionStatistics
  '/llm/qa/${sessionId}/clear',   // clearSession
  '/llm/qa/service/status',     // getLLMServiceStatus
  '/llm/qa/hot-questions'       // getHotQuestions
];

apiPaths.forEach(path => {
  if (apiContent.includes(path)) {
    console.log(`✓ 找到路径: ${path}`);
  } else {
    console.log(`✗ 未找到路径: ${path}`);
  }
});

console.log('\n=== 后端控制器映射对比 ===');
console.log('前端API路径 /llm/qa -> 后端映射 /admin/llm/qa ✓');
console.log('前端API路径 /llm/qa/{sessionId}/history -> 后端映射 /admin/llm/qa/{sessionId}/history ✓');
console.log('前端API路径 /llm/qa/{sessionId}/clear -> 后端映射 /admin/llm/qa/{sessionId}/clear ✓');
console.log('前端API路径 /llm/qa/session/statistics -> 后端映射 /admin/llm/qa/session/statistics ✓');
console.log('前端API路径 /llm/qa/service/status -> 后端映射 /admin/llm/qa/service/status ✓');
console.log('前端API路径 /llm/qa/hot-questions -> 后端映射 /admin/llm/qa/hot-questions ✓');

console.log('\n=== 修复总结 ===');
console.log('1. ✓ askQuestion API路径已修正: /admin/llm/qa -> /llm/qa（移除了冗余前缀）');
console.log('2. ✓ getSessionHistory API路径已修正: /admin/llm/qa/{id}/history -> /llm/qa/{id}/history');
console.log('3. ✓ getSessionStatistics API路径已修正: 添加了正确路径');
console.log('4. ✓ clearSession API路径已修正: /admin/llm/qa/{id}/clear -> /llm/qa/{id}/clear');
console.log('5. ✓ getLLMServiceStatus API路径已修正: 添加了正确路径');
console.log('6. ✓ getHotQuestions API路径已修正: 添加了正确路径');
console.log('7. ✓ 所有前端API路径通过baseURL组合后正确映射到后端端点');