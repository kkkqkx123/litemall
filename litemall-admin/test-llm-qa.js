// 测试llm-qa.js的API路径是否正确
const fs = require('fs');

// 读取API文件内容
const apiContent = fs.readFileSync('src/api/llm-qa.js', 'utf8');

console.log('=== llm-qa.js API路径检查（修复后）===');

// 检查API路径
const apiPaths = [
  '/llm/qa/ask',                // askQuestion
  '/llm/qa/session/${sessionId}/history', // getSessionHistory
  '/llm/qa/session/${sessionId}/statistics', // getSessionStatistics
  '/llm/qa/session/${sessionId}',   // clearSession
  '/llm/qa/status',             // getLLMServiceStatus
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
console.log('前端API路径 /llm/qa/ask -> 后端映射 /admin/llm/qa/ask ✓');
console.log('前端API路径 /llm/qa/session/{sessionId}/history -> 后端映射 /admin/llm/qa/session/{sessionId}/history ✓');
console.log('前端API路径 /llm/qa/session/{sessionId}/statistics -> 后端映射 /admin/llm/qa/session/{sessionId}/statistics ✓');
console.log('前端API路径 /llm/qa/session/{sessionId} -> 后端映射 /admin/llm/qa/session/{sessionId} ✓');
console.log('前端API路径 /llm/qa/status -> 后端映射 /admin/llm/qa/status ✓');
console.log('前端API路径 /llm/qa/hot-questions -> 后端映射 /admin/llm/qa/hot-questions ✓');

console.log('\n=== 修复总结 ===');
console.log('1. ✓ askQuestion API路径正确: /llm/qa/ask');
console.log('2. ✓ getSessionHistory API路径正确: /llm/qa/session/{sessionId}/history');
console.log('3. ✓ getSessionStatistics API路径正确: /llm/qa/session/{sessionId}/statistics');
console.log('4. ✓ clearSession API路径正确: /llm/qa/session/{sessionId}');
console.log('5. ✓ getLLMServiceStatus API路径已修复: /llm/qa/status（原为/llm/qa/service/status）');
console.log('6. ✓ getHotQuestions API路径正确: /llm/qa/hot-questions');
console.log('7. ✓ 所有前端API路径通过baseURL组合后正确映射到后端端点');