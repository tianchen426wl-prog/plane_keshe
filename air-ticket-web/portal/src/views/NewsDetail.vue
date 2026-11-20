<template>
  <div class="news-detail" v-loading="loading">
    <div class="detail-container" v-if="news">
      <h1 class="title">{{ news.title }}</h1>
      <p class="time"><i class="el-icon-time"></i> {{ news.time }}</p>
      <div class="image" v-if="getNewsImage(news.title, news.picture)">
        <img :src="getNewsImage(news.title, news.picture)" alt=""/>
      </div>
      <div class="content" v-text="news.profile" style="white-space: pre-wrap"></div>
      <div class="back-btn">
        <el-button type="primary" @click="$router.back()">返回列表</el-button>
      </div>
    </div>
    <el-empty v-if="!loading && !news" description="资讯不存在" />
  </div>
</template>

<script>
import { getNewsDetail } from '../api/news'
import { getNewsImage } from '../utils/newsImage'

export default {
  name: 'NewsDetail',
  data() {
    return {
      news: null,
      loading: false
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    getNewsImage,
    loadData() {
      const id = this.$route.params.id
      if (!id) return
      this.loading = true
      getNewsDetail(id).then(res => {
        this.news = res.data
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    }
  }
}
</script>

<style scoped>
.news-detail {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}
.detail-container {
  background: #fff;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}
.title {
  text-align: center;
  font-size: 28px;
  color: #333;
  margin-bottom: 15px;
}
.time {
  text-align: center;
  color: #999;
  font-size: 14px;
  margin-bottom: 20px;
}
.image {
  width: 100%;
  max-height: 450px;
  overflow: hidden;
  border-radius: 8px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}
.image img {
  width: 100%;
  height: auto;
  max-height: 450px;
  object-fit: contain;
  display: block;
}
.content {
  line-height: 1.8;
  color: #555;
  font-size: 15px;
  margin-bottom: 30px;
}
.back-btn {
  text-align: center;
}
@media screen and (max-width: 768px) {
  .detail-container {
    padding: 20px 15px;
  }
  .title {
    font-size: 22px;
  }
  .image {
    max-height: 300px;
  }
  .image img {
    max-height: 300px;
  }
}
@media screen and (max-width: 576px) {
  .image {
    max-height: 220px;
  }
  .image img {
    max-height: 220px;
  }
}
</style>