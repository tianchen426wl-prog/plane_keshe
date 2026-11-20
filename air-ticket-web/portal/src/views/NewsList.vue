<template>
  <div class="news-list">
    <h2 class="page-title">航空资讯</h2>
    <div v-loading="loading" class="news-grid">
      <el-card v-for="item in list" :key="item.id" class="news-card" shadow="hover">
        <div class="news-image">
          <img :src="getNewsImage(item.title, item.picture)" alt=""/>
        </div>
        <div class="news-content">
          <h4 class="line-clamp">{{ item.title }}</h4>
          <p class="time"><i class="el-icon-time"></i> {{ item.time }}</p>
          <el-button type="text" @click="$router.push(`/news/${item.id}`)">阅读全文 <i class="el-icon-arrow-right"></i></el-button>
        </div>
      </el-card>
    </div>
    <div v-if="total > 0" class="pagination">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @current-change="handlePageChange"
      />
    </div>
    <el-empty v-if="!loading && list.length === 0" description="暂无资讯" />
  </div>
</template>

<script>
import { getNewsList } from '../api/news'
import { getNewsImage } from '../utils/newsImage'

export default {
  name: 'NewsList',
  data() {
    return {
      list: [],
      page: 1,
      pageSize: 9,
      total: 0,
      loading: false
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    getNewsImage,
    loadData() {
      this.loading = true
      getNewsList({ page: this.page, pageSize: this.pageSize }).then(res => {
        this.list = res.data.records || []
        this.total = res.data.total || 0
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    handlePageChange(page) {
      this.page = page
      this.loadData()
    }
  }
}
</script>

<style scoped>
.news-list {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}
.page-title {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
  font-size: 28px;
}
.news-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}
.news-card {
  cursor: pointer;
  transition: transform 0.3s;
}
.news-card:hover {
  transform: translateY(-5px);
}
.news-card .news-image {
  height: 180px;
  overflow: hidden;
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}
.news-card .news-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}
.news-card:hover .news-image img {
  transform: scale(1.05);
}
.news-content h4 {
  color: #333;
  margin-bottom: 8px;
  font-size: 16px;
}
.line-clamp {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.news-content .time {
  color: #999;
  font-size: 12px;
  margin-bottom: 8px;
}
.pagination {
  text-align: center;
  margin-top: 30px;
}
@media screen and (max-width: 992px) {
  .news-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media screen and (max-width: 576px) {
  .news-grid {
    grid-template-columns: 1fr;
  }
  .news-card .news-image {
    height: 160px;
  }
}
</style>