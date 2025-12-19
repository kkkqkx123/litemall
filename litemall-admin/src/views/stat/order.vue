<template>
  <div class="app-container">
    <!-- 筛选条件区域 -->
    <el-card class="filter-container">
      <el-form :inline="true" :model="filterForm" size="small">
        <el-form-item label="统计组织方式">
          <el-select v-model="filterForm.groupBy" placeholder="选择统计组织方式" @change="handleGroupByChange">
            <el-option label="按年统计" value="year" />
            <el-option label="按季度统计" value="quarter" />
            <el-option label="按月统计" value="month" />
            <el-option label="按日统计" value="day" />
          </el-select>
        </el-form-item>
        <el-form-item label="年份">
          <el-select v-model="filterForm.year" placeholder="选择年份" @change="handleYearChange">
            <el-option
              v-for="year in availableYears"
              :key="year"
              :label="year + '年'"
              :value="year"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="季度">
          <el-select
            v-model="filterForm.quarter"
            placeholder="选择季度"
            clearable
            @change="handleQuarterChange"
          >
            <el-option label="全年" :value="null" />
            <el-option label="第一季度" :value="1" />
            <el-option label="第二季度" :value="2" />
            <el-option label="第三季度" :value="3" />
            <el-option label="第四季度" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="月份">
          <el-select
            v-model="filterForm.month"
            placeholder="选择月份"
            clearable
            :disabled="!filterForm.year"
            @change="handleMonthChange"
          >
            <el-option label="全季度/全年" :value="null" />
            <el-option
              v-for="month in availableMonths"
              :key="month"
              :label="month + '月'"
              :value="month"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-select
            v-model="filterForm.day"
            placeholder="选择日期"
            clearable
            :disabled="!filterForm.month"
          >
            <el-option label="全月" :value="null" />
            <el-option
              v-for="day in 31"
              :key="day"
              :label="day + '日'"
              :value="day"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="商品类别">
          <el-select
            v-model="filterForm.categoryId"
            placeholder="选择商品类别"
            clearable
            @change="handleFilterChange"
          >
            <el-option
              v-for="item in categoryOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 统计信息展示区域 -->
    <el-card class="statistics-container">
      <!-- 统计汇总信息 -->
      <div class="summary-info">
        <div class="summary-item">
          <span class="label">总订单数：</span>
          <span class="value">{{ summary.totalCount || 0 }}</span>
        </div>
        <div class="summary-item">
          <span class="label">总金额：</span>
          <span class="value">¥{{ summary.totalAmount || 0 }}</span>
        </div>
        <div class="summary-item">
          <span class="label">平均金额：</span>
          <span class="value">¥{{ summary.avgAmount || 0 }}</span>
        </div>
      </div>

      <!-- 统计详情表格 -->
      <el-table
        v-loading="loading"
        element-loading-text="加载中..."
        :data="statisticsData"
        style="width: 100%"
      >
        <el-table-column prop="timeLabel" label="时间" width="180" />
        <el-table-column prop="orderCount" label="订单数量" width="120" />
        <el-table-column prop="orderAmount" label="订单金额">
          <template slot-scope="scope">
            ¥{{ scope.row.orderAmount }}
          </template>
        </el-table-column>
        <el-table-column prop="avgAmount" label="平均金额">
          <template slot-scope="scope">
            ¥{{ scope.row.avgAmount }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { statOrderEnhanced } from '@/api/stat'
import { listCatL1 } from '@/api/category'

export default {
  data() {
    return {
      loading: false,
      summary: {
        totalCount: 0,
        totalAmount: 0,
        avgAmount: 0
      },
      statisticsData: [],
      filterForm: {
        categoryId: null,
        groupBy: 'month', // 默认按月统计
        year: null,
        quarter: null,
        month: null,
        day: null
      },
      categoryOptions: [],
      availableYears: [],
      availableMonths: []
    }
  },
  created() {
    // 初始化可用年份
    this.initAvailableYears()
    // 动态获取分类列表
    this.loadCategories()
    // 设置默认年份为当前年份
    this.filterForm.year = new Date().getFullYear()
    // 初始化可用月份
    this.updateAvailableMonths()
    // 加载数据
    this.loadData()
  },
  methods: {
    handleGroupByChange() {
      // 当统计组织方式改变时，重置时间筛选条件并加载数据
      this.filterForm.quarter = null
      this.filterForm.month = null
      this.filterForm.day = null
      this.updateAvailableMonths()
      this.loadData()
    },

    initAvailableYears() {
      const currentYear = new Date().getFullYear()
      // 生成从当前年份往前5年的选项
      this.availableYears = Array.from({ length: 6 }, (_, i) => currentYear - i)
    },

    loadCategories() {
      // 首先尝试从商品分类API获取数据
      listCatL1().then(response => {
        // 后端返回格式: {errno: 0, errmsg: "", data: data}
        if (response.data.errno === 0) {
          // API调用成功，添加"全部类别"选项
          this.categoryOptions = [{ value: null, label: '全部类别' }, ...response.data.data]
        } else if (response.data.errno === 501) {
          // 未登录错误，由request.js的拦截器处理登录跳转
          console.warn('未登录，请先登录系统')
          // 不显示错误消息，由全局拦截器处理登录提示
          this.categoryOptions = [{ value: null, label: '全部类别' }]
        } else {
          this.$message.error('加载商品分类失败: ' + (response.data.errmsg || '未知错误'))
          this.categoryOptions = [{ value: null, label: '全部类别' }]
        }
      }).catch(error => {
        console.error('加载商品分类失败:', error)
        // 如果商品分类API失败，尝试使用统计接口获取分类
        this.loadCategoriesFromStat()
      })
    },

    // 从统计接口获取分类列表（备选方案）
    loadCategoriesFromStat() {
      import('@/api/stat').then(({ statGoodsCategories }) => {
        statGoodsCategories().then(response => {
          if (response.data.errno === 0) {
            // 转换数据格式以匹配前端期望
            const categories = response.data.data.map(item => ({
              value: item.id,
              label: item.name
            }))
            this.categoryOptions = [{ value: null, label: '全部类别' }, ...categories]
          } else {
            this.$message.error('从统计接口加载商品分类失败: ' + (response.data.errmsg || '未知错误'))
            this.categoryOptions = [{ value: null, label: '全部类别' }]
          }
        }).catch(error => {
          console.error('从统计接口加载商品分类失败:', error)
          this.categoryOptions = [{ value: null, label: '全部类别' }]
        })
      }).catch(error => {
        console.error('导入统计API失败:', error)
        this.categoryOptions = [{ value: null, label: '全部类别' }]
      })
    },

    handleYearChange() {
      this.filterForm.quarter = null
      this.filterForm.month = null
      this.filterForm.day = null
      this.updateAvailableMonths()
      this.loadData()
    },

    handleQuarterChange() {
      this.filterForm.month = null
      this.filterForm.day = null
      this.updateAvailableMonths()
      this.loadData()
    },

    handleMonthChange() {
      this.filterForm.day = null
      this.loadData()
    },

    updateAvailableMonths() {
      // 根据选择的季度更新可选月份
      if (this.filterForm.quarter) {
        const quarterMonths = {
          1: [1, 2, 3],
          2: [4, 5, 6],
          3: [7, 8, 9],
          4: [10, 11, 12]
        }
        this.availableMonths = quarterMonths[this.filterForm.quarter]
      } else {
        this.availableMonths = Array.from({ length: 12 }, (_, i) => i + 1)
      }
    },

    loadData() {
      this.loading = true

      // 构建查询参数
      const query = {
        categoryId: this.filterForm.categoryId,
        groupBy: this.filterForm.groupBy, // 使用统计组织方式参数
        year: this.filterForm.year
      }

      // 添加时间过滤条件（作为辅助筛选）
      if (this.filterForm.quarter) {
        query.quarter = this.filterForm.quarter
      }

      if (this.filterForm.month) {
        query.month = this.filterForm.month
      }

      if (this.filterForm.day) {
        query.day = this.filterForm.day
      }

      console.log('查询参数:', query)

      statOrderEnhanced(query).then(response => {
        console.log('订单统计数据响应:', response)

        // 验证返回数据格式
        if (!response.data || !response.data.data) {
          console.error('返回数据格式错误:', response)
          this.$message.error('获取统计数据失败：数据格式错误')
          this.loading = false
          return
        }

        const statData = response.data.data

        // 检查是否有数据
        if (!statData.details || statData.details.length === 0) {
          console.warn('没有查询到统计数据，查询参数:', query)
          this.$message.info('当前筛选条件下没有统计数据')
          // 设置空数据
          this.summary = {
            totalCount: 0,
            totalAmount: 0,
            avgAmount: 0
          }
          this.statisticsData = []
          this.loading = false
          return
        }

        // 设置统计数据
        this.summary = {
          totalCount: statData.summary.totalCount,
          totalAmount: statData.summary.totalAmount,
          avgAmount: statData.summary.avgAmount
        }
        this.statisticsData = statData.details

        console.log('统计数据设置完成:', this.summary, this.statisticsData)
      }).catch(error => {
        console.error('获取订单统计数据失败:', error)

        // 处理特定错误码
        if (error.response && error.response.data.errno === 502) {
          this.$message.error('选择的日期超出当月天数范围，请重新选择')
        } else {
          this.$message.error('获取统计数据失败：' + (error.message || '网络错误'))
        }

        // 设置空数据
        this.summary = {
          totalCount: 0,
          totalAmount: 0,
          avgAmount: 0
        }
        this.statisticsData = []
      }).finally(() => {
        this.loading = false
      })
    },

    handleFilterChange() {
      // 当筛选条件改变时自动刷新数据
      this.loadData()
    },

    handleQuery() {
      this.loadData()
    },

    handleReset() {
      this.filterForm = {
        categoryId: null,
        groupBy: 'month', // 重置时保留默认按月统计
        year: new Date().getFullYear(), // 重置时使用当前年份
        quarter: null,
        month: null,
        day: null
      }
      this.updateAvailableMonths()
      this.loadData()
    }
  }
}
</script>

<style scoped>
.filter-container {
  margin-bottom: 20px;
}

.chart-container {
  margin-bottom: 20px;
}
</style>
