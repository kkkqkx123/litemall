// LLM问答系统常量定义

export const MESSAGE_TYPES = {
  USER: 'user',
  ASSISTANT: 'assistant',
  ERROR: 'error',
  SYSTEM: 'system'
}

export const MESSAGE_ROLES = {
  [MESSAGE_TYPES.USER]: '用户',
  [MESSAGE_TYPES.ASSISTANT]: 'AI助手',
  [MESSAGE_TYPES.ERROR]: '系统',
  [MESSAGE_TYPES.SYSTEM]: '系统'
}

export const ERROR_CODES = {
  NETWORK_ERROR: 'NETWORK_ERROR',
  TIMEOUT_ERROR: 'TIMEOUT_ERROR',
  SERVER_ERROR: 'SERVER_ERROR',
  VALIDATION_ERROR: 'VALIDATION_ERROR',
  DATA_FORMAT_ERROR: 'DATA_FORMAT_ERROR'
}

export const LLM_CONFIG_KEYS = {
  MAX_RESULTS: 'request.maxResults',
  MAX_HISTORY_LENGTH: 'context.maxHistoryLength',
  MAX_MESSAGE_LENGTH: 'context.maxMessageLength',
  TIMEOUT: 'request.timeout',
  ENABLE_SERVICE_CHECK: 'features.enableServiceCheck',
  MAX_ERROR_MESSAGES: 'ui.maxErrorMessages',
  AUTO_SCROLL_THRESHOLD: 'ui.autoScrollThreshold',
  ENABLE_SMOOTH_SCROLL: 'ui.enableSmoothScroll'
}

export const DEFAULT_CONFIG = {
  request: {
    maxResults: 10,
    timeout: 30000,
    retryCount: 3
  },
  context: {
    maxHistoryLength: 5,
    maxMessageLength: 1000
  },
  ui: {
    maxErrorMessages: 3,
    autoScrollThreshold: 100,
    enableSmoothScroll: true
  },
  features: {
    enableHotQuestions: true,
    enableServiceCheck: true,
    enableQuickQuestions: true
  }
}
