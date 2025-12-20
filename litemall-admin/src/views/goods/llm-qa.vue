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
        </div>

        <div
          v-for="(message, messageIndex) in messages"
          :key="message.id || messageIndex"
          :class="['message', message.type]"
        >
          <!-- 用户消息 -->
          <div v-if="message.type === 'user'" class="message-content">
            <div class="message-text">{{ message.content }}</div>
            <div class="message-time">{{ formatTime(message.timestamp, { showRelative: true }) }}</div>
          </div>

          <!-- 助手消息 -->
          <div v-else-if="message.type === 'assistant'" class="message-content">
            <div class="message-text">{{ message.content }}</div>
            <div class="message-time">{{ formatTime(message.timestamp, { showRelative: true }) }}</div>
          </div>

          <!-- 商品消息 -->
          <div v-else-if="message.type === 'goods'" class="message-content">
            <div class="goods-list">
              <div v-for="(item, itemIndex) in message.content" :key="itemIndex" class="goods-item" @click="viewGoodsDetail(item.id)">
                <div class="goods-image">
                  <img :src="item.imageUrl || '/static/img/default-goods.png'" :alt="item.name">
                </div>
                <div class="goods-info">
                  <div class="goods-name">{{ item.name }}</div>
                  <div class="goods-price">¥{{ item.price }}</div>
                  <div v-if="item.brief" class="goods-brief">{{ item.brief }}</div>
                </div>
              </div>
            </div>
            <div class="message-time">{{ formatTime(message.timestamp, { showRelative: true }) }}</div>
          </div>
        </div>

        <div v-if="isLoading" class="loading-indicator">
          <i class="el-icon-loading" />
          <span>{{ loadingMessage }}</span>
          <div v-if="loadingProgress" class="loading-progress">
            <el-progress :percentage="loadingProgress.percentage" :status="loadingProgress.status" />
            <div class="progress-text">{{ loadingProgress.text }}</div>
          </div>
        </div>
      </div>

      <!-- 快速提问按钮 -->
      <div v-if="quickQuestions.length > 0" class="quick-questions">
        <el-button
          v-for="question in quickQuestions"
          :key="question"
          size="small"
          plain
          :disabled="isLoading || !isServiceAvailable"
          @click="sendQuickQuestion(question)"
        >
          {{ question }}
        </el-button>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <el-input
          ref="questionInput"
          v-model="inputQuestion"
          type="textarea"
          :rows="2"
          placeholder="请输入您的问题..."
          :disabled="isLoading || !isServiceAvailable"
          @keyup.enter.native="handleEnterKey"
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
          <el-button v-if="messages.length > 0" :disabled="isLoading" @click="clearConversation">
            清空对话
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { askQuestion, getLLMServiceStatus } from '@/api/llm-qa'
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
      // 加载消息
      loadingMessage: 'AI正在思考中...',
      // 加载进度
      loadingProgress: null,
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

      // 自动滚动
      autoScroll: true
    }
  },

  async mounted() {
    // 初始化组件
    try {
      // 1. 检查服务状态
      await this.initializeService()

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

    // 处理回车键
    handleEnterKey(event) {
      // 如果按下的是Ctrl+Enter，则换行；否则发送消息
      if (event.ctrlKey) {
        return
      }
      event.preventDefault()
      this.sendQuestion()
    },

    // 发送消息
    async sendMessage(content, options = {}) {
      if (!content || !content.trim()) {
        this.$message.warning('请输入问题')
        return false
      }

      // 添加用户消息
      this.addUserMessage(content)

      // 显示加载状态
      this.setLoadingState('initial')

      try {
        // 构建请求数据
        const requestData = {
          question: content,
          sessionId: this.sessionId
        }

        // 调用API
        const response = await askQuestion(requestData)

        // 处理响应
        const result = this.processApiResponse(response)
        if (!result.success) {
          this.showError(result.error)
          return false
        }

        // 添加助手消息
        this.addAssistantMessage(result.answer)

        // 如果有商品数据，处理商品信息
        if (result.goods && result.goods.length > 0) {
          this.addGoodsMessage(result.goods)
        }

        // 更新上下文历史
        this.updateContextHistory(content, result.answer)

        // 更新会话ID（如果后端返回了新的sessionId）
        if (result.sessionId) {
          this.sessionId = result.sessionId
        }

        return true
      } catch (error) {
        console.error('发送消息失败:', error)
        const errorMessage = this.getDetailedErrorMessage(error)
        this.showError(errorMessage)
        return false
      } finally {
        this.isLoading = false
      }
    },

    // 设置加载状态
    setLoadingState(stage) {
      this.isLoading = true

      switch (stage) {
        case 'initial':
          this.loadingMessage = 'AI正在理解您的问题...'
          this.loadingProgress = { percentage: 20, status: null, text: '分析问题中' }
          break
        case 'thinking':
          this.loadingMessage = 'AI正在思考...'
          this.loadingProgress = { percentage: 40, status: null, text: '生成回答中' }
          break
        case 'querying':
          this.loadingMessage = '正在查询商品信息...'
          this.loadingProgress = { percentage: 70, status: null, text: '数据库查询中' }
          break
        case 'finalizing':
          this.loadingMessage = '正在整理回答...'
          this.loadingProgress = { percentage: 90, status: null, text: '即将完成' }
          break
      }

      // 模拟进度更新
      if (stage !== 'finalizing') {
        const nextStage = stage === 'initial' ? 'thinking'
          : stage === 'thinking' ? 'querying'
            : 'finalizing'
        setTimeout(() => this.setLoadingState(nextStage), 3000)
      }
    },

    // 发送用户输入的问题
    async sendQuestion() {
      const content = this.inputQuestion.trim()
      if (!content) {
        this.$message.warning('请输入问题')
        return
      }

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
    },

    // 添加商品消息
    addGoodsMessage(goods) {
      if (!Array.isArray(goods) || goods.length === 0) return

      // 确保每个商品对象都有必要的字段
      const processedGoods = goods.map(item => ({
        id: item.id || item.goodsId || '',
        name: item.name || item.goodsName || '未知商品',
        brief: item.brief || item.goodsBrief || '',
        price: item.price || item.retailPrice || 0,
        imageUrl: item.imageUrl || item.picUrl || '',
        unit: item.unit || '件'
      }))

      const goodsMessage = {
        id: this.generateMessageId(),
        type: 'goods',
        content: processedGoods,
        timestamp: Date.now()
      }

      this.messages.push(goodsMessage)

      // 滚动到底部
      this.$nextTick(() => {
        this.scrollToBottom()
      })
    },

    // 生成消息ID
    generateMessageId() {
      return `msg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    },

    // 查看商品详情
    viewGoodsDetail(goodsId) {
      if (!goodsId) return

      // 使用Vue Router跳转到商品详情页
      this.$router.push({
        path: '/goods/list',
        query: { id: goodsId }
      }).catch(err => {
        console.error('跳转商品详情失败:', err)
        this.$message.error('跳转商品详情失败')
      })
    },

    // 获取详细错误信息
    getDetailedErrorMessage(error) {
      console.error('错误详情:', error)

      // 处理null或undefined错误
      if (!error) {
        return '发生未知错误，请稍后重试'
      }

      // 如果错误对象已经有errno和errmsg，直接返回
      if (typeof error === 'object' && 'errno' in error && 'errmsg' in error) {
        return `请求失败 (错误码: ${error.errno}) - ${error.errmsg}`
      }

      // 如果错误对象是Axios响应对象
      if (error.response) {
        const status = error.response.status
        const errorData = error.response.data

        // 首先检查响应数据中的errno和errmsg
        if (errorData && typeof errorData === 'object' && 'errno' in errorData && 'errmsg' in errorData) {
          return `请求失败 (错误码: ${errorData.errno}) - ${errorData.errmsg}`
        }

        // 根据HTTP状态码返回更具体的错误信息
        const statusMessages = {
          400: '请求参数错误',
          401: '未授权，请重新登录',
          403: '拒绝访问',
          404: '请求的资源不存在',
          408: '请求超时',
          429: '请求过于频繁，请稍后重试',
          500: '服务器内部错误',
          502: '网关错误',
          503: '服务暂时不可用',
          504: '网关超时'
        }

        const statusMessage = statusMessages[status] || `HTTP ${status}`
        const detailMessage = errorData?.errmsg || errorData?.message || ''

        return detailMessage ? `请求失败 (${statusMessage}) - ${detailMessage}` : `请求失败 (${statusMessage})`
      }

      // 如果是网络错误
      if (error.code === 'ECONNABORTED') {
        return '请求超时，请检查网络连接'
      }

      if (error.code === 'NETWORK_ERROR') {
        return '网络连接失败，请检查网络设置'
      }

      // 如果是其他类型的错误
      if (error.message) {
        return error.message
      }

      // 如果错误是字符串类型
      if (typeof error === 'string') {
        return error
      }

      // 默认错误信息
      return '未知错误，请稍后重试'
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

        // 商品消息样式
        .goods-list {
          margin-top: 8px;

          .goods-item {
            display: flex;
            padding: 8px;
            margin-bottom: 8px;
            border: 1px solid #ebeef5;
            border-radius: 4px;
            cursor: pointer;
            transition: all 0.3s;

            &:hover {
              border-color: #409eff;
              box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
            }

            &:last-child {
              margin-bottom: 0;
            }

            .goods-image {
              width: 60px;
              height: 60px;
              margin-right: 12px;
              flex-shrink: 0;

              img {
                width: 100%;
                height: 100%;
                object-fit: cover;
                border-radius: 4px;
              }
            }

            .goods-info {
              flex: 1;
              display: flex;
              flex-direction: column;
              justify-content: space-between;

              .goods-name {
                font-size: 14px;
                font-weight: 500;
                color: #303133;
                margin-bottom: 4px;
                overflow: hidden;
                text-overflow: ellipsis;
                display: -webkit-box;
                -webkit-line-clamp: 2;
                -webkit-box-orient: vertical;
              }

              .goods-price {
                font-size: 16px;
                font-weight: 600;
                color: #f56c6c;
                margin-bottom: 4px;
              }

              .goods-brief {
                font-size: 12px;
                color: #909399;
                overflow: hidden;
                text-overflow: ellipsis;
                display: -webkit-box;
                -webkit-line-clamp: 2;
                -webkit-box-orient: vertical;
              }
            }
          }
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

      .loading-progress {
        margin-top: 12px;
        width: 80%;
        margin-left: auto;
        margin-right: auto;

        .progress-text {
          font-size: 12px;
          margin-top: 6px;
          color: #606266;
        }
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
      padding: 12px 20px;

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
