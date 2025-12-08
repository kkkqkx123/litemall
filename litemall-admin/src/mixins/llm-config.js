// LLM配置管理mixin

import { DEFAULT_CONFIG, LLM_CONFIG_KEYS } from '@/constants/llm-constants'

export default {
  data() {
    return {
      llmConfig: { ...DEFAULT_CONFIG }
    }
  },
  
  methods: {
    // 动态更新配置
    updateConfig(path, value) {
      const keys = path.split('.')
      let obj = this.llmConfig
      
      for (let i = 0; i < keys.length - 1; i++) {
        if (!obj[keys[i]]) obj[keys[i]] = {}
        obj = obj[keys[i]]
      }
      
      obj[keys[keys.length - 1]] = value
    },
    
    // 获取配置值
    getConfig(path, defaultValue) {
      const keys = path.split('.')
      let value = this.llmConfig
      
      for (const key of keys) {
        if (value && typeof value === 'object' && key in value) {
          value = value[key]
        } else {
          return defaultValue
        }
      }
      
      return value
    },
    
    // 重置为默认配置
    resetConfig() {
      this.llmConfig = { ...DEFAULT_CONFIG }
    }
  }
}