<template>
  <div class="app-container">

    <!-- 查询和其他操作 -->
    <div class="filter-container">
      <el-input v-model="listQuery.searchText" clearable class="filter-item" style="width: 200px;" placeholder="用户ID或商品名称" />
      <el-select v-model="listQuery.categoryId" clearable class="filter-item" style="width: 200px;" placeholder="商品类别">
        <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button class="filter-item" type="primary" icon="el-icon-search" @click="handleFilter">{{ $t('app.button.search') }}</el-button>
      <el-button :loading="downloadLoading" class="filter-item" type="primary" icon="el-icon-download" @click="handleDownload">{{ $t('app.button.download') }}</el-button>
    </div>

    <!-- 查询结果 -->
    <el-table v-loading="listLoading" :data="list" :element-loading-text="$t('app.message.list_loading')" border fit highlight-current-row>

      <el-table-column align="center" :label="$t('goods_comment.table.user_id')" prop="userId" width="80" />

      <el-table-column align="center" label="商品名称" prop="goodsName" min-width="150" />

      <el-table-column align="center" label="商品类别" prop="categoryName" width="120" />

      <el-table-column align="center" sortable="custom" :label="$t('goods_comment.table.star')" prop="star" width="80">
        <template slot-scope="scope">
          <el-rate v-model="scope.row.star" disabled show-score text-color="#ff9900" />
        </template>
      </el-table-column>

      <el-table-column align="center" :label="$t('goods_comment.table.content')" prop="content" min-width="200" show-overflow-tooltip />

      <el-table-column align="center" label="管理员回复" prop="adminContent" min-width="150" show-overflow-tooltip />

      <el-table-column align="center" :label="$t('goods_comment.table.add_time')" prop="addTime" width="160">
        <template slot-scope="scope">
          {{ parseTime(scope.row.addTime, '{y}-{m}-{d}') }}
        </template>
      </el-table-column>

      <el-table-column align="center" :label="$t('goods_comment.table.actions')" width="200" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button type="primary" size="mini" @click="handleReply(scope.row)">{{ $t('app.button.reply') }}</el-button>
          <el-button type="danger" size="mini" @click="handleDelete(scope.row)">{{ $t('app.button.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="listQuery.page" :limit.sync="listQuery.limit" :page-sizes="[20, 50, 100]" @pagination="getList" />

    <!-- 评论回复 -->
    <el-dialog :visible.sync="replyFormVisible" :title="$t('goods_comment.dialog.reply')">
      <el-form ref="replyForm" :model="replyForm" status-icon label-position="left" label-width="100px" style="width: 400px; margin-left:50px;">
        <el-form-item :label="$t('goods_comment.form.content')" prop="content">
          <el-input v-model="replyForm.content" :autosize="{ minRows: 4, maxRows: 8}" type="textarea" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="replyFormVisible = false">{{ $t('app.button.cancel') }}</el-button>
        <el-button type="primary" @click="reply">{{ $t('app.button.confirm') }}</el-button>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import { listComment, deleteComment } from '@/api/comment'
import { replyComment } from '@/api/order'
import { listCategory } from '@/api/category'
import Pagination from '@/components/Pagination' // Secondary package based on el-pagination
import { parseTime } from '@/utils'

export default {
  name: 'Comment',
  components: { Pagination },
  data() {
    return {
      list: [],
      total: 0,
      listLoading: true,
      listQuery: {
        page: 1,
        limit: 20,
        searchText: undefined, // 合并的搜索文本，可能是用户ID或商品名称
        categoryId: undefined,
        sort: 'add_time',
        order: 'desc'
      },
      downloadLoading: false,
      replyForm: {
        commentId: 0,
        content: ''
      },
      replyFormVisible: false,
      categoryOptions: [] // 商品类别选项
    }
  },
  created() {
    this.getList()
    this.getCategoryOptions()
  },
  methods: {
    parseTime,
    getList() {
      this.listLoading = true

      // 构建查询参数
      const query = {
        page: this.listQuery.page,
        limit: this.listQuery.limit,
        sort: this.listQuery.sort,
        order: this.listQuery.order
      }

      // 处理搜索文本，可能是用户ID或商品名称
      if (this.listQuery.searchText) {
        // 尝试转换为数字，如果成功则是用户ID，否则是商品名称
        if (!isNaN(this.listQuery.searchText)) {
          query.userId = this.listQuery.searchText
        } else {
          query.goodsName = this.listQuery.searchText
        }
      }

      if (this.listQuery.categoryId) {
        query.categoryId = this.listQuery.categoryId
      }

      listComment(query).then(response => {
        this.list = response.data.data.list
        this.total = response.data.data.total
        this.listLoading = false
      }).catch(() => {
        this.list = []
        this.total = 0
        this.listLoading = false
      })
    },
    getCategoryOptions() {
      listCategory({}).then(response => {
        const categories = response.data.data.list || []
        this.categoryOptions = categories.map(item => ({
          value: item.id,
          label: item.name
        }))
      })
    },
    handleFilter() {
      this.listQuery.page = 1
      this.getList()
    },
    handleReply(row) {
      this.replyForm = { commentId: row.id, content: '' }
      this.replyFormVisible = true
    },
    reply() {
      replyComment(this.replyForm).then(response => {
        this.replyFormVisible = false
        this.$notify.success({
          title: '成功',
          message: '回复成功'
        })
        this.getList()
      }).catch(response => {
        this.$notify.error({
          title: '失败',
          message: response.data.errmsg
        })
      })
    },
    handleDelete(row) {
      deleteComment(row).then(response => {
        this.$notify({
          title: '成功',
          message: '删除成功',
          type: 'success',
          duration: 2000
        })
        this.getList()
      })
    },
    handleDownload() {
      this.downloadLoading = true
      import('@/vendor/Export2Excel').then(excel => {
        const tHeader = ['评论ID', '用户ID', '商品名称', '商品类别', '评分', '评论', '管理员回复', '评论时间']
        const filterVal = ['id', 'userId', 'goodsName', 'categoryName', 'star', 'content', 'adminContent', 'addTime']
        excel.export_json_to_excel2(tHeader, this.list, filterVal, '商品评论信息')
        this.downloadLoading = false
      })
    }
  }
}
</script>
