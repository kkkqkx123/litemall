// LLM UI逻辑mixin

export default {
  methods: {
    // 显示加载状态
    showLoading() {
      this.isLoading = true
    },

    // 隐藏加载状态
    hideLoading() {
      this.isLoading = false
    },

    // 显示错误提示
    showError(message) {
      this.$message.error(message)
    },

    // 显示成功提示
    showSuccess(message) {
      this.$message.success(message)
    },

    // 聚焦输入框
    focusInput() {
      this.$nextTick(() => {
        const textarea = this.$el.querySelector('.input-area textarea')
        if (textarea) {
          textarea.focus()
          // 将光标移到末尾
          textarea.setSelectionRange(textarea.value.length, textarea.value.length)
        }
      })
    },

    // 确认对话框
    async showConfirm(message, title = '提示') {
      try {
        await this.$confirm(message, title, {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        return true
      } catch {
        return false
      }
    },

    // 统一的消息添加方法
    addMessage(type, content, options = {}) {
      const message = {
        id: this.generateMessageId(),
        type: type,
        content: content,
        timestamp: Date.now(),
        ...options
      }

      this.messages.push(message)

      // 智能判断是否自动滚动
      if (this.shouldAutoScroll() || options.forceScroll) {
        this.$nextTick(() => {
          this.scrollToBottom({ smooth: true })
        })
      }

      return message
    },

    // 添加用户消息
    addUserMessage(content) {
      return this.addMessage('user', content, {
        forceScroll: true
      })
    },

    // 添加AI回复消息
    addAssistantMessage(content) {
      return this.addMessage('assistant', content, {
        forceScroll: true
      })
    },

    // 添加错误消息
    addErrorMessage(content) {
      // 限制错误消息数量
      const errorCount = this.messages.filter(m => m.isError).length
      if (errorCount >= this.getConfig('ui.maxErrorMessages', 3)) {
        // 移除最早的错误消息
        const firstErrorIndex = this.messages.findIndex(m => m.isError)
        if (firstErrorIndex !== -1) {
          this.messages.splice(firstErrorIndex, 1)
        }
      }

      return this.addMessage('error', content, {
        isError: true,
        forceScroll: true
      })
    },

    // 统一的错误处理
    handleRequestError(error) {
      console.error('LLM请求失败:', error)

      let errorMessage = '发送失败，请稍后重试'

      // 分类处理不同类型的错误
      if (error.code === 'ECONNABORTED') {
        errorMessage = '请求超时，请检查网络连接'
      } else if (error.response) {
        const status = error.response.status
        if (status === 503) {
          errorMessage = 'AI服务暂时不可用，请稍后再试'
        } else if (status === 429) {
          errorMessage = '请求过于频繁，请稍后再试'
        } else if (status >= 500) {
          errorMessage = '服务器错误，请稍后再试'
        } else {
          const errorData = error.response.data
          const status = error.response.status
          errorMessage = `请求失败 (HTTP ${status}) - ${errorData?.errmsg || errorData?.message || '未知错误'}`
          console.error('HTTP错误详情:', error.response)
        }
      } else if (error.request) {
        errorMessage = '网络连接失败，请检查网络'
      }

      this.addErrorMessage(errorMessage)
    }
  }
}
