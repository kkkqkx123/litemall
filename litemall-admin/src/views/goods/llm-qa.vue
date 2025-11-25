<template>
  <div class="app-container">
    <div class="llm-qa-container">
      <!-- 头部 -->
      <div class="qa-header">
        <h3>智能商品问答</h3>
        <div v-if="sessionId" class="session-info">
          <span>会话ID: {{ sessionId.substring(0, 8) }}...</span>
          <span>对话轮数: {{ messages.length }}</span>
        </div>
      </div>

      <!-- 对话区域 -->
      <div ref="conversationArea" class="conversation-area">
        <div v-if="messages.length === 0" class="welcome-message">
          <p>欢迎使用智能商品问答系统！</p>
          <p>您可以询问价格、库存、分类等相关问题。</p>
        </div>

        <div
          v-for="(message, index) in messages"
          :key="index"
          :class="['message', message.type]"
        >
          <div class="message-content">
            <div class="message-text">{{ message.content }}</div>
            <div class="message-time">{{ formatTime(message.timestamp) }}</div>
          </div>
        </div>

        <div v-if="isLoading" class="loading-indicator">
          <i class="el-icon-loading" />
          <span>AI正在思考中...</span>
        </div>
      </div>

      <!-- 快速提问按钮 -->
      <div class="quick-questions">
        <el-button
          v-for="question in quickQuestions"
          :key="question"
          size="small"
          plain
          @click="sendQuickQuestion(question)"
        >
          {{ question }}
        </el-button>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <el-input
          v-model="inputQuestion"
          type="textarea"
          :rows="2"
          placeholder="请输入您的问题..."
          :disabled="isLoading"
          @keyup.enter.native="sendQuestion"
        />
        <div class="input-actions">
          <el-button
            type="primary"
            :loading="isLoading"
            :disabled="!inputQuestion.trim()"
            @click="sendQuestion"
          >
            发送
          </el-button>
          <el-button v-if="messages.length > 0" @click="clearConversation">
            清空对话
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { askQuestion } from '@/api/llm-qa'

export default {
  name: 'LLMQA',
  data() {
    return {
      messages: [],
      sessionId: '',
      isLoading: false,
      inputQuestion: '',
      contextHistory: [],
      maxContextLength: 5,
      autoScroll: true,
      quickQuestions: [
        '价格在100-200元的商品有哪些？',
        '库存充足的商品有哪些？',
        '电子产品分类下的商品有哪些？',
        '统计在售商品总数',
        '价格最低的商品是什么？',
        '库存最多的商品有哪些？'
      ]
    }
  },

  methods: {
    // 生成会话ID
    generateSessionId() {
      return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
    },

    // 发送问题
    async sendQuestion() {
      if (!this.inputQuestion.trim() || this.isLoading) return

      const question = this.inputQuestion.trim()
      this.inputQuestion = ''

      // 生成会话ID（如果不存在）
      if (!this.sessionId) {
        this.sessionId = this.generateSessionId()
      }

      // 添加到消息列表
      this.messages.push({
        type: 'user',
        content: question,
        timestamp: Date.now()
      })

      this.isLoading = true

      try {
        // 构建上下文
        const context = this.buildContext()

        // 调用API
        const response = await askQuestion({
          question: question,
          sessionId: this.sessionId,
          context: context,
          maxResults: 10
        })

        if (response.success) {
          // 添加到消息列表
          this.messages.push({
            type: 'assistant',
            content: response.data.answer,
            timestamp: Date.now()
          })

          // 更新上下文历史
          this.updateContextHistory(question, response.data.answer)

          // 自动滚动到底部
          this.$nextTick(() => {
            this.scrollToBottom()
          })
        } else {
          this.showError(response.message || '请求失败')
        }
      } catch (error) {
        console.error('发送问题失败:', error)
        this.showError('发送失败，请稍后重试')
      } finally {
        this.isLoading = false
      }
    },

    // 发送快速提问
    sendQuickQuestion(question) {
      this.inputQuestion = question
      this.sendQuestion()
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
      if (this.contextHistory.length > this.maxContextLength) {
        this.contextHistory.shift()
      }
    },

    // 清空对话
    clearConversation() {
      this.$confirm('确定要清空当前对话吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.messages = []
        this.contextHistory = []
        this.sessionId = ''
        this.$message.success('对话已清空')
      }).catch(() => {})
    },

    // 滚动到底部
    scrollToBottom() {
      if (this.$refs.conversationArea) {
        this.$refs.conversationArea.scrollTop = this.$refs.conversationArea.scrollHeight
      }
    },

    // 格式化时间
    formatTime(timestamp) {
      const date = new Date(timestamp)
      return date.toLocaleTimeString()
    },

    // 显示错误
    showError(message) {
      this.$message.error(message)
      this.messages.push({
        type: 'error',
        content: message,
        timestamp: Date.now()
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.llm-qa-container {
  height: calc(100vh - 84px);
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);

  .qa-header {
    padding: 16px 20px;
    background: white;
    border-bottom: 1px solid #e4e7ed;
    display: flex;
    justify-content: space-between;
    align-items: center;

    h3 {
      margin: 0;
      color: #303133;
      font-size: 18px;
      font-weight: 500;
    }

    .session-info {
      font-size: 12px;
      color: #909399;

      span {
        margin-left: 16px;
      }
    }
  }

  .conversation-area {
    flex: 1;
    padding: 20px;
    overflow-y: auto;
    background: white;
    min-height: 300px;

    .welcome-message {
      text-align: center;
      color: #909399;
      padding: 60px 0;

      p {
        margin: 12px 0;
        font-size: 14px;
        line-height: 1.6;
      }
    }

    .message {
      margin-bottom: 20px;
      display: flex;

      &.user {
        justify-content: flex-end;

        .message-content {
          background: #409eff;
          color: white;
          border-radius: 16px 16px 0 16px;
          max-width: 70%;
        }
      }

      &.assistant {
        justify-content: flex-start;

        .message-content {
          background: #f0f2f5;
          color: #303133;
          border-radius: 16px 16px 16px 0;
          max-width: 70%;
        }
      }

      &.error {
        justify-content: center;

        .message-content {
          background: #fef0f0;
          color: #f56c6c;
          border-radius: 8px;
          max-width: 80%;
        }
      }

      .message-content {
        padding: 12px 16px;
        position: relative;
        box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);

        .message-text {
          word-wrap: break-word;
          line-height: 1.5;
          font-size: 14px;
        }

        .message-time {
          font-size: 11px;
          opacity: 0.7;
          margin-top: 6px;
          text-align: right;
        }
      }
    }

    .loading-indicator {
      text-align: center;
      color: #909399;
      padding: 20px;

      i {
        margin-right: 8px;
        font-size: 16px;
      }

      span {
        font-size: 14px;
      }
    }
  }

  .quick-questions {
    padding: 16px 20px;
    background: white;
    border-top: 1px solid #e4e7ed;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;

    .el-button {
      margin: 0;
      font-size: 12px;
      padding: 6px 12px;
    }
  }

  .input-area {
    padding: 20px;
    background: white;
    border-top: 1px solid #e4e7ed;

    .input-actions {
      margin-top: 12px;
      display: flex;
      justify-content: flex-end;
      gap: 8px;
    }
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .llm-qa-container {
    height: calc(100vh - 50px);

    .qa-header {
      padding: 12px 16px;

      h3 {
        font-size: 16px;
      }

      .session-info {
        display: none;
      }
    }

    .conversation-area {
      padding: 16px;

      .message {
        .message-content {
          max-width: 85%;
        }
      }
    }

    .quick-questions {
      padding: 12px 16px;
    }

    .input-area {
      padding: 16px;
    }
  }
}
</style>
