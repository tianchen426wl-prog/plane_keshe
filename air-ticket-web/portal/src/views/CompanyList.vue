<template>
  <div class="company-list">
    <h2 class="page-title">航空公司</h2>
    <div class="search-bar">
      <el-input
        v-model="searchName"
        placeholder="请输入航空公司名称搜索"
        clearable
        style="width: 300px; margin-right: 10px;"
        @keyup.enter.native="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>
    <div v-loading="loading" class="company-grid">
      <el-card
        v-for="item in list"
        :key="item.companyId"
        class="company-card"
        shadow="hover"
        @click.native="$router.push(`/company/${item.companyId}`)"
      >
        <div class="card-image">
          <img @error="companyImageError" loading="lazy" :src="getCompanyImage(item.companyName, item.companyPicture)" alt=""/>
        </div>
        <h3>{{ item.companyName }}</h3>
        <p><i class="el-icon-phone"></i> {{ item.companyPhone }}</p>
        <el-button type="text">查看详情 <i class="el-icon-arrow-right"></i></el-button>
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
    <el-empty v-if="!loading && list.length === 0" description="暂无航空公司数据" />
  </div>
</template>

<script>
import { getCompanyList } from '../api/company'
import { getCompanyImage, companyImageError } from '../utils/companyImage'

export default {
  name: 'CompanyList',
  data() {
    return {
      searchName: '',
      list: [],
      page: 1,
      pageSize: 8,
      total: 0,
      loading: false
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    getCompanyImage,
    companyImageError,
    loadData() {
      this.loading = true
      getCompanyList({
        page: this.page,
        pageSize: this.pageSize,
        keyword: this.searchName
      }).then(res => {
        this.list = res.data.records || []
        this.total = res.data.total || 0
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    handleSearch() {
      this.page = 1
      this.loadData()
    },
    resetSearch() {
      this.searchName = ''
      this.page = 1
      this.loadData()
    },
    handlePageChange(page) {
      this.page = page
      this.loadData()
    }
  }
}
</script>

<style scoped>
.company-list {
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
.search-bar {
  text-align: center;
  margin-bottom: 30px;
}
.company-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}
.company-card {
  text-align: center;
  cursor: pointer;
  transition: transform 0.3s;
}
.company-card:hover {
  transform: translateY(-5px);
}
.company-card .card-image {
  height: 180px;
  overflow: hidden;
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}
.company-card .card-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform 0.3s;
}
.company-card:hover .card-image img {
  transform: scale(1.05);
}
.company-card h3 {
  margin: 10px 0;
  color: #333;
}
.company-card p {
  color: #666;
}
.pagination {
  text-align: center;
  margin-top: 20px;
}
@media screen and (max-width: 992px) {
  .company-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media screen and (max-width: 576px) {
  .company-grid {
    grid-template-columns: 1fr;
  }
  .company-card .card-image {
    height: 150px;
  }
}
</style>