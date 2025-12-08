// 响应数据验证工具函数

export function validateLLMResponse(response) {
  if (!response || typeof response !== 'object') {
    return { valid: false, error: '无效的响应格式' }
  }
  
  if (response.errno !== 0) {
    return { valid: false, error: response.errmsg || '请求失败' }
  }
  
  if (!response.data || typeof response.data.answer !== 'string') {
    return { valid: false, error: '服务器返回数据格式错误' }
  }
  
  return { valid: true, data: response.data }
}

export function validateQuestion(question) {
  if (!question || typeof question !== 'string') {
    return { valid: false, error: '问题内容不能为空' }
  }
  
  const trimmed = question.trim()
  if (!trimmed) {
    return { valid: false, error: '问题内容不能为空' }
  }
  
  if (trimmed.length > 500) {
    return { valid: false, error: '问题内容不能超过500个字符' }
  }
  
  return { valid: true, content: trimmed }
}

export function generateMessageId() {
  return `msg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
}

export function formatTime(timestamp, options = {}) {
  const { showRelative = false, format = 'HH:mm:ss' } = options
  
  if (showRelative) {
    const now = Date.now()
    const diff = now - timestamp
    
    // 1分钟内显示"刚刚"
    if (diff < 60000) return '刚刚'
    
    // 1小时内显示"X分钟前"
    if (diff < 3600000) {
      const minutes = Math.floor(diff / 60000)
      return `${minutes}分钟前`
    }
    
    // 今天内显示"HH:mm"
    const today = new Date().toDateString()
    const messageDate = new Date(timestamp).toDateString()
    if (today === messageDate) {
      return new Date(timestamp).toLocaleTimeString('zh-CN', { 
        hour: '2-digit', 
        minute: '2-digit' 
      })
    }
    
    // 昨天显示"昨天 HH:mm"
    const yesterday = new Date(Date.now() - 86400000).toDateString()
    if (yesterday === messageDate) {
      return '昨天 ' + new Date(timestamp).toLocaleTimeString('zh-CN', {
        hour: '2-digit',
        minute: '2-digit'
      })
    }
  }
  
  // 固定格式显示
  const date = new Date(timestamp)
  const formatMap = {
    'HH:mm:ss': () => {
      const h = date.getHours().toString().padStart(2, '0')
      const m = date.getMinutes().toString().padStart(2, '0')
      const s = date.getSeconds().toString().padStart(2, '0')
      return `${h}:${m}:${s}`
    },
    'HH:mm': () => {
      const h = date.getHours().toString().padStart(2, '0')
      const m = date.getMinutes().toString().padStart(2, '0')
      return `${h}:${m}`
    }
  }
  
  return formatMap[format] ? formatMap[format]() : formatMap['HH:mm:ss']()
}