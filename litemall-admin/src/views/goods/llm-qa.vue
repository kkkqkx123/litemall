<template>
  <div class="app-container">
    <div class="llm-qa-container">
      <!-- 头部 -->
      <div class="qa-header">
        <h3>智能商品问答</h3>
        <div v-if="sessionId" class="session-info">
          <span>会话ID: {{ sessionId.substring(0, 8) }}...</span>
          <span>对话轮数: {{ messages.length }}</span>
          <span v-if="!isServiceAvailable" class="service-status error">服务异常</span>
        </div>
      </div>

      <!-- 对话区域 -->
      <div ref="conversationArea" class="conversation-area">
        <div v-if="messages.length === 0" class="welcome-message">
          <p>欢迎使用智能商品问答系统！</p>
          <p>您可以询问价格、库存、分类等相关问题。</p>
          <div v-if="hotQuestions.length > 0" class="hot-questions-welcome">
            <p class="hot-questions-title">热门问题：</p>
            <div class="hot-questions-list">
              <el-tag
                v-for="question in hotQuestions.slice(0, 3)"
                :key="question"
                size="small"
                @click="sendQuickQuestion(question)"
                class="hot-question-tag"
              >
                {{ question }}
              </el-tag>
            </div>
          </div>
        </div>

        <div
          v-for="(message, index) in messages"
          :key="message.id || index"
          :class="['message', message.type]"
        >
          <div class="message-content">
            <div class="message-text">{{ message.content }}</div>
            <div class="message-time">{{ formatTime(message.timestamp, { showRelative: true }) }}</div>
          </div>
        </div>

        <div v-if="isLoading" class="loading-indicator">
          <i class="el-icon-loading" />
          <span>AI正在思考中...</span>
        </div>
      </div>

      <!-- 快速提问按钮 -->
      <div v-if="quickQuestions.length > 0" class="quick-questions">
        <el-button
          v-for="question in quickQuestions"
          :key="question"
          size="small"
          plain
          @click="sendQuickQuestion(question)"
          :disabled="isLoading || !isServiceAvailable"
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
          :disabled="isLoading || !isServiceAvailable"
          @keyup.enter.native="handleEnterKey"
          ref="questionInput"
        />
        <div class="input-actions">
          <el-button
            type="primary"
            :loading="isLoading"
            :disabled="!inputQuestion.trim() || !isServiceAvailable"
            @click="sendQuestion"
          >
            发送
          </el-button>
          <el-button v-if="messages.length > 0" @click="clearConversation" :disabled="isLoading">
            清空对话
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { askQuestion, getLLMServiceStatus, getHotQuestions } from '@/api/llm-qa'
import llmLogicMixin from '@/mixins/llm-logic'
import llmUiMixin from '@/mixins/llm-ui'
import llmConfigMixin from '@/mixins/llm-config'
import { formatTime } from '@/utils/llm-utils'

export default {
  name: 'LLMQA',
  mixins: [llmLogicMixin, llmUiMixin, llmConfigMixin],
  data() {
    return {
      // 输入的问题
      inputQuestion: '',
      // 消息列表
      messages: [],
      // 是否正在加载
      isLoading: false,
      // 会话ID
      sessionId: '',
      // 上下文历史
      contextHistory: [],
      // 快速提问列表
      quickQuestions: [
        '这个商品有什么特点？',
        '这个商品适合什么人群？',
        '这个商品的使用方法？',
        '这个商品的材质是什么？',
        '这个商品的保修政策？',
        '这个商品的配送时间？'
      ],
      // 服务状态
      isServiceAvailable: true,
      serviceStatus: null,
      // 热门问题
      hotQuestions: [],
      // 自动滚动
      autoScroll: true
    }
  },

  async mounted() {
    // 初始化组件
    try {
      // 1. 检查服务状态
      await this.initializeService()

      // 2. 加载热门问题
      await this.loadHotQuestions()

      // 3. 初始化会话ID
      if (!this.sessionId) {
        this.sessionId = this.generateSessionId()
      }

      // 4. 聚焦输入框
      this.$nextTick(() => {
        this.focusInput()
      })

    } catch (error) {
      console.error('组件初始化失败:', error)
      this.showError('组件初始化失败，请刷新页面重试')
    }
  },

  methods: {
    // 初始化服务
    async initializeService() {
      if (!this.getConfig('features.enableServiceCheck', true)) {
        this.isServiceAvailable = true
        return
      }

      try {
        const response = await getLLMServiceStatus()
        if (response.errno === 0 && response.data) {
          this.serviceStatus = response.data
          this.isServiceAvailable = response.data.status === 'running'

          if (!this.isServiceAvailable) {
            this.showError('AI服务暂时不可用')
          }
        }
      } catch (error) {
        console.error('获取服务状态失败:', error)
        this.isServiceAvailable = false
        this.showError('无法连接AI服务')
      }
    },

    // 加载热门问题
    async loadHotQuestions() {
      if (!this.getConfig('features.enableHotQuestions', true)) {
        return
      }

      try {
        const response = await getHotQuestions()
        if (response.errno === 0 && response.data) {
          this.hotQuestions = response.data.questions || []
          // 更新快速提问按钮
          if (this.hotQuestions.length > 0) {
            this.quickQuestions = [...this.hotQuestions.slice(0, 6)]
          }
        }
      } catch (error) {
        console.error('获取热门问题失败:', error)
      }
    },

    // 处理回车键
    handleEnterKey(event) {
      // 如果按下的是Ctrl+Enter，则换行；否则发送消息
      if (event.ctrlKey) {
        return
      }
      event.preventDefault()
      this.sendQuestion()
    },

    // 统一的发送消息方法
    async sendMessage(content, options = {}) {
      if (!content.trim() || this.isLoading || !this.isServiceAvailable) return false

      // const { isQuickQuestion = false } = options

      try {
        // 验证输入内容
        const validation = this.validateQuestion(content)
        if (!validation.valid) {
          this.showError(validation.error)
          return false
        }

        // 添加用户消息
        this.addUserMessage(validation.content)
        this.showLoading()

        // 生成会话ID（如果不存在）
        if (!this.sessionId) {
          this.sessionId = this.generateSessionId()
        }

        // 构建请求数据
        const requestData = {
          question: validation.content,
          sessionId: this.sessionId,
          context: this.buildContext(),
          maxResults: this.getConfig('request.maxResults', 10)
        }

        // 调用API
        const response = await askQuestion(requestData)
        const result = this.processApiResponse(response)

        if (!result.success) {
          this.addErrorMessage(result.error)
          return false
        }

        // 添加AI回复
        this.addAssistantMessage(result.answer)

        // 更新上下文历史
        this.updateContextHistory(validation.content, result.answer)

        return true
      } catch (error) {
        this.handleRequestError(error)
        return false
      } finally {
        this.hideLoading()
      }
    },

    // 发送用户输入的问题
    async sendQuestion() {
      const content = this.inputQuestion.trim()
      this.inputQuestion = ''

      const success = await this.sendMessage(content)
      if (success) {
        this.focusInput()
      }
    },

    // 发送快速提问
    async sendQuickQuestion(question) {
      // 处理输入内容冲突
      const currentInput = this.inputQuestion.trim()

      if (currentInput && currentInput !== question) {
        const confirmed = await this.showConfirm(
          '将替换您当前的输入内容，是否继续？',
          '提示'
        )
        
        if (!confirmed) return
      }

      this.inputQuestion = ''
      const success = await this.sendMessage(question, { isQuickQuestion: true })
      if (!success) {
        // 如果发送失败，恢复输入内容
        this.inputQuestion = question
        this.focusInput()
      }
    },

    // 清空对话
    async clearConversation() {
      const confirmed = await this.showConfirm('确定要清空当前对话吗？')
      if (!confirmed) return

      this.messages = []
      this.contextHistory = []
      this.sessionId = ''
      this.showSuccess('对话已清空')
    },

    // 格式化时间（使用工具函数）
    formatTime(timestamp, options = {}) {
      return formatTime(timestamp, options)
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
