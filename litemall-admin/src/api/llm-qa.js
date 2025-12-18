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
  return request({
    url: '/llm/qa/ask',
    method: 'post',
    data: data
  })
}

/**
 * 获取LLM服务状态
 * @returns {Promise} - 返回Promise对象
 */
export function getLLMServiceStatus() {
  return request({
    url: '/llm/qa/service/status',
    method: 'get'
  })
}

/**
 * 获取热门问题
 * @param {Object} params - 查询参数
 * @param {number} params.limit - 返回问题数量限制
 * @param {string} params.category - 问题分类
 * @returns {Promise} - 返回Promise对象
 */
export function getHotQuestions(params) {
  return request({
    url: '/llm/qa/hot-questions',
    method: 'get',
    params: params
  })
}
