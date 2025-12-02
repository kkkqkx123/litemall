<template>
  <div class="app-container">
    <!-- 筛选条件区域 -->
    <el-card class="filter-container">
      <el-form :inline="true" :model="filterForm" size="small">
        <el-form-item label="年份">
          <el-input-number v-model="filterForm.year" :min="2020" :max="2030" controls-position="right" @change="handleFilterChange" />
        </el-form-item>
        <el-form-item label="季度">
          <el-select v-model="filterForm.quarter" placeholder="选择季度" clearable @change="handleFilterChange">
            <el-option label="全部" value="" />
            <el-option label="Q1" value="1" />
            <el-option label="Q2" value="2" />
            <el-option label="Q3" value="3" />
            <el-option label="Q4" value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="月份">
          <el-select v-model="filterForm.month" placeholder="选择月份" clearable @change="handleFilterChange">
            <el-option label="全部" value="" />
            <el-option v-for="month in 12" :key="month" :label="month + '月'" :value="month" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker
            v-model="filterForm.day"
            type="date"
            placeholder="选择日期"
            format="yyyy-MM-dd"
            value-format="yyyy-MM-dd"
            clearable
            @change="handleFilterChange"
          />
        </el-form-item>
        <el-form-item label="商品类别">
          <el-select v-model="filterForm.categoryId" placeholder="选择商品类别" clearable @change="handleFilterChange">
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

    <!-- 图表展示区域 -->
    <el-card class="chart-container">
      <ve-line :extend="chartExtend" :data="chartData" :settings="chartSettings" />
    </el-card>
  </div>
</template>

<script>
import { statOrderEnhanced } from '@/api/stat'
import { listCatL1 } from '@/api/category'
import VeLine from 'v-charts/lib/line'
export default {
  components: { VeLine },
  data() {
    return {
      chartData: {
        columns: ['period', 'orders', 'customers', 'amount', 'pcr'],
        rows: []
      },
      chartSettings: {},
      chartExtend: {},
      filterForm: {
        categoryId: null,
        year: new Date().getFullYear(), // 使用当前年份
        quarter: '',
        month: '',
        day: null
      },
      categoryOptions: []
    }
  },
  created() {
    // 动态获取分类列表，移除硬编码
    this.loadCategories()
    this.loadData()
  },
  methods: {
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

    // 确保分类选项包含"全部类别"
    ensureAllCategoryOption(categories = []) {
      // 如果还没有"全部类别"选项，添加它
      const hasAllCategory = categories.some(item => item.value === null)
      if (!hasAllCategory) {
        return [{ value: null, label: '全部类别' }, ...categories]
      }
      return categories
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
    loadData() {
      // 构建查询参数，根据实际提供的参数决定时间范围
      const query = {
        categoryId: this.filterForm.categoryId
      }

      // 根据优先级设置参数：day > month > quarter > year
      if (this.filterForm.day) {
        // 如果选择了具体日期，查询当天数据
        query.day = this.filterForm.day
        query.year = this.filterForm.year // 提供年份作为参考
      } else if (this.filterForm.month) {
        // 如果选择了月份，查询整月数据
        query.month = parseInt(this.filterForm.month) // 确保月份是数字
        query.year = this.filterForm.year
      } else if (this.filterForm.quarter) {
        // 如果选择了季度，查询整季度数据
        query.quarter = parseInt(this.filterForm.quarter) // 确保季度是数字
        query.year = this.filterForm.year
      } else {
        // 默认使用年份，查询整年数据
        query.year = this.filterForm.year
      }

      console.log('查询参数:', query)

      statOrderEnhanced(query).then(response => {
        console.log('订单统计数据响应:', response)
        console.log('统计数据详情:', response.data.data)

        // 验证返回数据格式
        if (!response.data || !response.data.data) {
          console.error('返回数据格式错误:', response)
          this.$message.error('获取统计数据失败：数据格式错误')
          return
        }

        const statData = response.data.data

        // 检查是否有数据
        if (!statData.rows || statData.rows.length === 0) {
          console.warn('没有查询到统计数据，查询参数:', query)
          this.$message.info('当前筛选条件下没有统计数据')
          // 设置空数据格式，确保图表能正确显示
          this.chartData = {
            columns: ['period', 'orders', 'customers', 'amount', 'pcr'],
            rows: []
          }
          return
        }

        // 验证数据格式
        if (!statData.columns || !Array.isArray(statData.rows)) {
          console.error('统计数据格式不正确:', statData)
          this.$message.error('统计数据格式不正确')
          return
        }

        // 设置图表数据
        this.chartData = statData
        this.chartSettings = {
          labelMap: {
            'orders': '订单量',
            'customers': '下单用户',
            'amount': '订单总额',
            'pcr': '客单价'
          }
        }
        this.chartExtend = {
          xAxis: { boundaryGap: true }
        }

        console.log('图表数据设置完成:', this.chartData)
      }).catch(error => {
        console.error('获取订单统计数据失败:', error)
        this.$message.error('获取统计数据失败：' + (error.message || '网络错误'))
        // 设置空数据避免图表报错
        this.chartData = {
          columns: ['period', 'orders', 'customers', 'amount', 'pcr'],
          rows: []
        }
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
        year: new Date().getFullYear(), // 重置时使用当前年份
        quarter: '',
        month: '',
        day: null
      }
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
