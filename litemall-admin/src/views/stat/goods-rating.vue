<template>
  <div class="app-container">
    <!-- 查询表单 -->
    <div class="filter-container">
      <el-select v-model="listQuery.categoryId" placeholder="选择商品分类" clearable class="filter-item">
        <el-option
          v-for="item in categories"
          :key="item.id"
          :label="item.name"
          :value="item.id"
        />
      </el-select>
      <el-select v-model="listQuery.sort" placeholder="排序字段" class="filter-item">
        <el-option label="平均评分" value="avg_rating" />
      </el-select>
      <el-select v-model="listQuery.order" placeholder="排序方式" class="filter-item">
        <el-option label="降序" value="desc" />
        <el-option label="升序" value="asc" />
      </el-select>
      <el-button class="filter-item" type="primary" icon="el-icon-search" @click="handleFilter">
        查询
      </el-button>
    </div>

    <!-- 数据表格 -->
    <el-table
      v-loading="listLoading"
      :data="list"
      border
      fit
      highlight-current-row
      style="width: 100%;"
    >
      <el-table-column label="商品ID" prop="goods_id" align="center" width="80" />
      <el-table-column label="商品名称" prop="goods_name" min-width="200" />
      <el-table-column label="商品分类" prop="category_name" width="120" />
      <el-table-column label="平均评分" prop="avg_rating" align="center" width="100">
        <template slot-scope="scope">
          <el-rate
            v-model="scope.row.avg_rating"
            disabled
            show-score
            text-color="#ff9900"
            score-template="{value}"
          />
        </template>
      </el-table-column>
      <el-table-column label="评价数量" prop="rating_count" align="center" width="100" />
    </el-table>

    <!-- 分页 -->
    <pagination v-show="total>0" :total="total" :page.sync="listQuery.page" :limit.sync="listQuery.limit" @pagination="getList" />
  </div>
</template>

<script>
import { statGoodsRating, statGoodsCategories } from '@/api/stat'
import Pagination from '@/components/Pagination'

export default {
  name: 'GoodsRating',
  components: { Pagination },
  data() {
    return {
      list: null,
      total: 0,
      listLoading: true,
      categories: [],
      listQuery: {
        categoryId: undefined,
        sort: 'avg_rating',
        order: 'desc',
        page: 1,
        limit: 20
      }
    }
  },
  created() {
    this.getCategories()
    this.getList()
  },
  methods: {
    getList() {
      this.listLoading = true
      statGoodsRating(this.listQuery).then(response => {
        this.list = response.data.data.rows
        this.total = response.data.data.total
        this.listLoading = false
      }).catch(() => {
        this.listLoading = false
      })
    },
    getCategories() {
      statGoodsCategories().then(response => {
        this.categories = response.data.data
      })
    },
    handleFilter() {
      this.listQuery.page = 1
      this.getList()
    }
  }
}
</script>

<style scoped>
.filter-container {
  padding-bottom: 10px;
}
.filter-item {
  display: inline-block;
  vertical-align: middle;
  margin-bottom: 10px;
  margin-right: 10px;
}
</style>
