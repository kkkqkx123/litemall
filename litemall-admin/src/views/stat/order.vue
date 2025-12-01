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
import VeLine from 'v-charts/lib/line'
export default {
  components: { VeLine },
  data() {
    return {
      chartData: {},
      chartSettings: {},
      chartExtend: {},
      filterForm: {
        categoryId: null,
        year: new Date().getFullYear(),
        quarter: '',
        month: '',
        day: null
      },
      categoryOptions: [
        { value: 1005000, label: '新鲜水果' },
        { value: 1005001, label: '海鲜水产' },
        { value: 1005002, label: '精选肉类' },
        { value: 1005003, label: '蛋类' },
        { value: 1005004, label: '新鲜蔬菜' },
        { value: 1005005, label: '速冻食品' },
        { value: 1005006, label: '饮品' },
        { value: 1005007, label: '休闲零食' },
        { value: 1005008, label: '粮油调味' },
        { value: 1005009, label: '方便速食' }
      ]
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    loadData() {
      const query = {
        categoryId: this.filterForm.categoryId,
        year: this.filterForm.year,
        quarter: this.filterForm.quarter,
        month: this.filterForm.month,
        day: this.filterForm.day
      }

      statOrderEnhanced(query).then(response => {
        this.chartData = response.data.data
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
      const currentDate = new Date()
      this.filterForm = {
        categoryId: null,
        year: currentDate.getFullYear(),
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
