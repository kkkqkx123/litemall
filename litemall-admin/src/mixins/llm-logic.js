// LLM业务逻辑mixin

import { MESSAGE_TYPES } from '@/constants/llm-constants'
import { validateLLMResponse, validateQuestion, generateMessageId } from '@/utils/llm-utils'

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
      const validation = validateLLMResponse(response)
      
      if (!validation.valid) {
        return { success: false, error: validation.error }
      }
      
      const answer = validation.data.answer.trim()
      if (!answer) {
        return { success: false, error: 'AI暂时无法回答您的问题' }
      }
      
      return { success: true, answer: answer }
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