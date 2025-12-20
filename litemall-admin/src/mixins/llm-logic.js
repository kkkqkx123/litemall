// LLM业务逻辑mixin

import { generateMessageId, validateQuestion } from '@/utils/llm-utils'

export default {
  methods: {
    // 生成会话ID
    generateSessionId() {
      const timestamp = Date.now()
      const random = Math.random().toString(36).substr(2, 9)
      return `session_${timestamp}_${random}`
    },

    // 生成消息ID
    generateMessageId() {
      return generateMessageId()
    },

    // 构建上下文
    buildContext() {
      if (this.contextHistory.length === 0) return ''

      return this.contextHistory
        .map(item => `用户：${item.question}\n回答：${item.answer}`)
        .join('\n')
    },

    // 更新上下文历史
    updateContextHistory(question, answer) {
      this.contextHistory.push({
        question: question,
        answer: answer,
        timestamp: Date.now()
      })

      // 限制历史记录数量
      const maxLength = this.getConfig('context.maxHistoryLength', 5)
      if (this.contextHistory.length > maxLength) {
        this.contextHistory.shift()
      }
    },

    // 验证问题格式
    validateQuestion(question) {
      return validateQuestion(question)
    },

    // 处理API响应
    processApiResponse(response) {
      try {
        // 输出完整的响应信息用于调试
        console.log('完整API响应:', JSON.stringify(response, null, 2))

        // 基本验证
        if (!response || typeof response !== 'object') {
          console.error('无效的响应格式:', response)
          return { success: false, error: '无效的响应格式' }
        }

        // 检查HTTP状态码
        if (response.status && response.status !== 200) {
          console.error('HTTP请求失败:', response.status, response.statusText)
          return { success: false, error: `HTTP请求失败: ${response.status} ${response.statusText}` }
        }

        // 检查响应数据结构
        if (!response.data) {
          console.error('响应缺少data字段:', response)
          return { success: false, error: '响应数据格式错误：缺少data字段' }
        }

        // 检查错误码
        if (response.data.errno !== 0) {
          const error = response.data.errmsg || '未知错误'
          console.error('API响应错误:', error, response.data)
          return { success: false, error: `请求失败 (错误码: ${response.data.errno}) - ${error}` }
        }

        // 检查是否有嵌套的data字段（根据后端代码，实际业务数据在response.data.data中）
        const businessData = response.data.data || response.data

        // 检查answer字段
        if (!businessData.answer || typeof businessData.answer !== 'string' || businessData.answer.trim() === '') {
          console.error('响应数据结构:', JSON.stringify(response, null, 2))
          return { success: false, error: '服务器返回数据格式错误：缺少answer字段' }
        }

        return {
          success: true,
          answer: businessData.answer.trim(),
          goods: businessData.goods || [],
          sessionId: businessData.sessionId,
          queryTime: businessData.queryTime || 0,
          fromCache: businessData.fromCache || false
        }
      } catch (error) {
        console.error('处理API响应时出错:', error)
        console.error('原始响应数据:', response)
        return { success: false, error: `处理响应数据时出错: ${error.message}` }
      }
    },

    // 智能判断是否自动滚动
    shouldAutoScroll() {
      if (!this.$refs.conversationArea) return true

      const container = this.$refs.conversationArea
      const threshold = this.getConfig('ui.autoScrollThreshold', 100)

      return container.scrollHeight - container.scrollTop - container.clientHeight < threshold
    },

    // 滚动到底部
    scrollToBottom(options = {}) {
      const { smooth = true, force = false } = options

      if (!this.$refs.conversationArea) return

      const container = this.$refs.conversationArea
      const targetScrollTop = container.scrollHeight - container.clientHeight

      // 如果内容没有变化且不是强制滚动，则不需要滚动
      if (!force && Math.abs(container.scrollTop - targetScrollTop) < 10) {
        return
      }

      if (smooth && 'scrollTo' in container) {
        // 使用平滑滚动
        container.scrollTo({
          top: targetScrollTop,
          behavior: 'smooth'
        })
      } else {
        // 降级到直接滚动
        container.scrollTop = targetScrollTop
      }
    }
  }
}
