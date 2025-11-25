<template>
  <div class="app-container">
    <!-- 查询区域 -->
    <div class="filter-container">
      <el-select v-model="listQuery.categoryId" placeholder="商品分类" clearable style="width: 200px" class="filter-item">
        <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-button v-waves class="filter-item" type="primary" icon="el-icon-search" @click="handleFilter">
        搜索
      </el-button>
      <el-button v-waves class="filter-item" type="success" icon="el-icon-refresh" @click="handleRefreshWordCloud">
        刷新词云
      </el-button>
    </div>

    <!-- 全局词云展示 -->
    <el-card class="word-cloud-card">
      <div slot="header" class="clearfix">
        <span>商品评论热词云图</span>
        <el-button style="float: right; padding: 3px 0" type="text" @click="handleRefreshGlobalWordCloud">
          刷新全局词云
        </el-button>
      </div>
      <word-cloud
        v-if="globalWordCloudData.length > 0"
        :data="globalWordCloudData"
        :max-words="50"
        @word-click="handleGlobalWordClick"
      />
      <div v-else class="no-word-cloud-data">
        <i class="el-icon-data-analysis" style="font-size: 48px; color: #909399;" />
        <p style="color: #909399; margin-top: 10px;">暂无词云数据</p>
      </div>
    </el-card>

    <!-- 商品评论列表 -->
    <el-table v-loading="listLoading" :data="list" element-loading-text="Loading" border fit highlight-current-row>
      <el-table-column align="center" label="商品ID" width="80">
        <template slot-scope="scope">
          {{ scope.row.goods_id }}
        </template>
      </el-table-column>
      <el-table-column label="商品名称" min-width="150">
        <template slot-scope="scope">
          {{ scope.row.goods_name }}
        </template>
      </el-table-column>
      <el-table-column label="分类" width="120" align="center">
        <template slot-scope="scope">
          {{ scope.row.categoryName }}
        </template>
      </el-table-column>
      <el-table-column label="评论数" width="100" align="center">
        <template slot-scope="scope">
          {{ scope.row.commentCount }}
        </template>
      </el-table-column>
      <el-table-column label="平均评分" width="100" align="center">
        <template slot-scope="scope">
          <el-rate v-model="scope.row.avgRating" disabled show-score text-color="#ff9900" score-template="{value}" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button type="primary" size="mini" @click="handleViewWordCloud(scope.row)">
            查看词云
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination v-show="total>0" :total="total" :page.sync="listQuery.page" :limit.sync="listQuery.limit" @pagination="fetchData" />

    <!-- 词云对话框 -->
    <el-dialog title="商品评论词云" :visible.sync="wordCloudDialogVisible" width="80%" top="5vh">
      <div class="word-cloud-container">
        <div v-if="wordCloudData.length === 0" class="no-data">
          暂无评论数据
        </div>
        <WordCloud
          v-else
          :data="wordCloudData"
          :max-words="50"
          @word-click="handleWordClick"
        />
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="wordCloudDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleRefreshWordCloud">刷新</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { statGoodsComment, statGoodsCategories, getGoodsWordCloud } from '@/api/stat'
import waves from '@/directive/waves' // waves directive
import Pagination from '@/components/Pagination' // secondary package based on el-pagination
import WordCloud from '@/components/WordCloud'

export default {
  name: 'CommentStat',
  components: { Pagination, WordCloud },
  directives: { waves },
  data() {
    return {
      list: null,
      total: 0,
      listLoading: true,
      categories: [],
      wordCloudDialogVisible: false,
      wordCloudData: [],
      globalWordCloudData: [],
      currentGoodsId: null,
      listQuery: {
        page: 1,
        limit: 20,
        categoryId: undefined
      }
    }
  },
  created() {
    this.fetchData()
    this.getCategories()
    this.getGlobalWordCloud()
  },
  methods: {
    fetchData() {
      this.listLoading = true
      statGoodsComment(this.listQuery).then(response => {
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
      this.fetchData()
    },
    handleViewWordCloud(row) {
      this.currentGoodsId = row.goodsId
      this.generateWordCloud()
      this.wordCloudDialogVisible = true
    },
    handleRefreshWordCloud() {
      if (this.currentGoodsId) {
        this.generateWordCloud()
      }
    },
    generateWordCloud() {
      getGoodsWordCloud(this.currentGoodsId).then(response => {
        // 转换后端返回的格式为WordCloud组件期望的格式
        this.wordCloudData = response.data.data.map(item => ({
          word: item.name,
          frequency: item.value
        }))
      }).catch(() => {
        this.wordCloudData = []
      })
    },
    handleWordClick(word) {
      this.$message({
        message: `词语：${word.word}，出现次数：${word.frequency}次`,
        type: 'info',
        duration: 3000
      })
    },
    // 全局词云相关方法
    getGlobalWordCloud() {
      // 获取热门商品的词云数据
      // 使用有评论数据的商品ID来生成词云
      const popularGoodsIds = [1181000, 1006002] // 从评论数据中获取的热门商品ID

      // 获取第一个热门商品的词云数据作为示例
      getGoodsWordCloud(popularGoodsIds[0]).then(response => {
        if (response.data.data && response.data.data.length > 0) {
          // 转换后端返回的格式为WordCloud组件期望的格式
          this.globalWordCloudData = response.data.data.map(item => ({
            word: item.name,
            frequency: item.value
          }))
        } else {
          // 如果没有数据，显示提示信息
          this.globalWordCloudData = []
        }
      }).catch(() => {
        this.globalWordCloudData = []
      })
    },
    handleRefreshGlobalWordCloud() {
      this.getGlobalWordCloud()
      this.$message({
        message: '词云数据已刷新',
        type: 'success',
        duration: 2000
      })
    },
    handleGlobalWordClick(word) {
      this.$message({
        message: `全局词云词语：${word.text}`,
        type: 'info',
        duration: 3000
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.word-cloud-card {
  margin-bottom: 20px;
}

.no-word-cloud-data {
  text-align: center;
  padding: 60px 0;
}

.word-cloud-container {
  min-height: 400px;
  padding: 20px;
  text-align: center;
  background: #f5f5f5;
  border-radius: 8px;
}

.word-cloud {
  line-height: 1.8;
  word-break: break-all;
}

.no-data {
  color: #909399;
  font-size: 16px;
  margin-top: 150px;
}

.filter-container {
  padding-bottom: 10px;
  .filter-item {
    display: inline-block;
    vertical-align: middle;
    margin-bottom: 10px;
  }
}
</style>
