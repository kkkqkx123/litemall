import request from '@/utils/request'

/**
 * LLM问答相关API接口
 */

/**
 * 发送问答请求
 * @param {Object} data 请求数据
 * @param {string} data.question - 用户问题
 * @param {string} data.sessionId - 会话ID（可选）
 * @param {string} data.context - 上下文信息（可选）
 * @param {number} data.maxResults - 最大结果数（可选）
 * @returns {Promise} 返回Promise对象
 */
export function askQuestion(data) {
  return request({
    url: '/admin/llm/qa/ask',
    method: 'post',
    data
  })
}

/**
 * 获取会话历史
 * @param {string} sessionId - 会话ID
 * @returns {Promise} 返回Promise对象
 */
export function getSessionHistory(sessionId) {
  return request({
    url: '/admin/llm/qa/session/' + sessionId + '/history',
    method: 'get'
  })
}

/**
 * 获取会话统计信息
 * @returns {Promise} 返回Promise对象
 */
export function getSessionStatistics() {
  return request({
    url: '/admin/llm/qa/session/statistics',
    method: 'get'
  })
}

/**
 * 清空会话
 * @param {string} sessionId - 会话ID
 * @returns {Promise} 返回Promise对象
 */
export function clearSession(sessionId) {
  return request({
    url: '/admin/llm/qa/session/' + sessionId + '/clear',
    method: 'delete'
  })
}

/**
 * 获取LLM服务状态
 * @returns {Promise} 返回Promise对象
 */
export function getLLMServiceStatus() {
  return request({
    url: '/admin/llm/qa/service/status',
    method: 'get'
  })
}

/**
 * 获取热门问题
 * @returns {Promise} 返回Promise对象
 */
export function getHotQuestions() {
  return request({
    url: '/admin/llm/qa/hot-questions',
    method: 'get'
  })
}
