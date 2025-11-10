<template>
  <div class="app-container">

    <!-- 查询和其他操作 -->
    <div class="filter-container">
      <el-input v-model="listQuery.userId" clearable class="filter-item" style="width: 200px;" :placeholder="$t('user_footprint.placeholder.filter_user_id')" />
      <el-input v-model="listQuery.goodsId" clearable class="filter-item" style="width: 200px;" :placeholder="$t('user_footprint.placeholder.filter_goods_id')" />
      <el-button class="filter-item" type="primary" icon="el-icon-search" @click="handleFilter">{{ $t('app.button.search') }}</el-button>
      <el-button :loading="downloadLoading" class="filter-item" type="primary" icon="el-icon-download" @click="handleDownload">{{ $t('app.button.download') }}</el-button>
      <el-button class="filter-item" type="danger" icon="el-icon-delete" :disabled="selectedRows.length === 0" @click="handleBatchDelete">批量删除</el-button>
    </div>

    <!-- 查询结果 -->
    <el-table v-loading="listLoading" :data="list" :element-loading-text="$t('app.message.list_loading')" border fit highlight-current-row @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" />
      <el-table-column align="center" width="100px" :label="$t('user_footprint.table.id')" prop="id" sortable />

      <el-table-column align="center" min-width="100px" :label="$t('user_footprint.table.user_id')" prop="userId" />

      <el-table-column align="center" min-width="100px" :label="$t('user_footprint.table.goods_id')" prop="goodsId" />

      <el-table-column align="center" min-width="100px" :label="$t('user_footprint.table.add_time')" prop="addTime" />

      <el-table-column align="center" label="操作" width="150" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button type="danger" size="mini" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>

    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="listQuery.page" :limit.sync="listQuery.limit" @pagination="getList" />

  </div>
</template>

<script>
import { listFootprint, deleteFootprint, batchDeleteFootprint } from '@/api/user'
import Pagination from '@/components/Pagination' // Secondary package based on el-pagination

export default {
  name: 'FootPrint',
  components: { Pagination },
  data() {
    return {
      list: null,
      total: 0,
      listLoading: true,
      listQuery: {
        page: 1,
        limit: 20,
        userId: undefined,
        goodsId: undefined,
        sort: 'add_time',
        order: 'desc'
      },
      downloadLoading: false,
      selectedRows: []
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.listLoading = true
      listFootprint(this.listQuery)
        .then(response => {
          this.list = response.data.data.list
          this.total = response.data.data.total
          this.listLoading = false
        })
        .catch(() => {
          this.list = []
          this.total = 0
          this.listLoading = false
        })
    },
    handleFilter() {
      this.listQuery.page = 1
      this.getList()
    },
    handleSelectionChange(val) {
      this.selectedRows = val
    },
    handleDelete(row) {
      this.$confirm('确定删除该足迹记录？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        deleteFootprint({ id: row.id, userId: row.userId }).then(response => {
          this.$message.success('删除成功')
          this.getList()
        })
      })
    },
    handleBatchDelete() {
      if (this.selectedRows.length === 0) {
        this.$message.warning('请选择要删除的记录')
        return
      }

      this.$confirm(`确定删除选中的${this.selectedRows.length}条足迹记录？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const ids = this.selectedRows.map(row => row.id)
        batchDeleteFootprint({ ids }).then(response => {
          this.$message.success('批量删除成功')
          this.getList()
        })
      })
    },
    handleDownload() {
      this.downloadLoading = true
      import('@/vendor/Export2Excel').then(excel => {
        const tHeader = ['用户ID', '商品ID', '添加时间']
        const filterVal = ['userId', 'goodsId', 'addTime']
        excel.export_json_to_excel2(
          tHeader,
          this.list,
          filterVal,
          '用户收藏信息'
        )
        this.downloadLoading = false
      })
    }
  }
}
</script>
