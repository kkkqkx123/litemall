import request from '@/utils/request'

/**
 * 向LLM提问
 * @param {Object} data - 提问数据
 * @param {string} data.question - 问题内容
 * @param {string} data.sessionId - 会话ID
 * @param {string} data.context - 上下文信息
 * @param {number} data.maxResults - 最大返回结果数
 * @returns {Promise} - 返回Promise对象
 */
export function askQuestion(data) {
  console.log('=== askQuestion函数调用开始 ===')
  console.log('请求数据:', data)
  console.log('请求URL:', '/llm/qa/ask')

  return request({
    url: '/llm/qa/ask',
    method: 'post',
    data,
    timeout: 45000 // 45秒超时，适应LLM处理时间
  })
}

/**
 * 获取LLM服务状态
 * @returns {Promise} - 返回Promise对象
 */
export function getLLMServiceStatus() {
  return request({
    url: '/llm/qa/status',
    method: 'get'
  })
}

/**
 * 获取会话历史记录
 * @param {string} sessionId - 会话ID
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码
 * @param {number} params.limit - 每页数量
 * @returns {Promise} - 返回Promise对象
 */
export function getSessionHistory(sessionId, params) {
  return request({
    url: `/admin/llm/qa/session/${sessionId}/history`,
    method: 'get',
    params: params
  })
}

/**
 * 获取会话统计信息
 * @param {string} sessionId - 会话ID
 * @param {Object} params - 查询参数
 * @param {number} params.days - 统计天数
 * @returns {Promise} - 返回Promise对象
 */
export function getSessionStatistics(sessionId, params) {
  return request({
    url: `/llm/qa/session/${sessionId}/statistics`,
    method: 'get',
    params: params
  })
}

/**
 * 清空会话
 * @param {string} sessionId - 会话ID
 * @returns {Promise} - 返回Promise对象
 */
export function clearSession(sessionId) {
  return request({
    url: `/llm/qa/session/${sessionId}`,
    method: 'delete'
  })
}
