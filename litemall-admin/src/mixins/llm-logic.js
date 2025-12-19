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
      try {
        // 使用验证工具函数检查响应
        const validation = validateLLMResponse(response)
        
        if (!validation.valid) {
          console.error('API响应验证失败:', validation.error)
          console.error('完整响应:', response)
          return { success: false, error: validation.error }
        }
        
        // 从验证结果中获取数据
        const answer = validation.data.answer
        const goods = validation.data.goods || []
        const sessionId = validation.data.sessionId
        
        // 检查answer是否为空
        if (!answer || answer.trim() === '') {
          return { success: false, error: 'AI暂时无法回答您的问题，请稍后再试' }
        }
        
        return { 
          success: true, 
          answer: answer.trim(),
          goods: goods,
          sessionId: sessionId // 确保返回sessionId
        }
        
      } catch (error) {
        console.error('处理API响应时出错:', error)
        return { success: false, error: '处理响应数据时出错' }
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