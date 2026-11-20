<template>
  <div class="company-detail" v-loading="loading">
    <div class="detail-container" v-if="company">
      <div class="detail-image">
        <img @error="companyImageError" loading="lazy" :src="getCompanyImage(company.companyName, company.companyPicture)" alt=""/>
      </div>
      <h1 class="company-name">{{ company.companyName }}</h1>
      <p v-if="company.sourceUrl" class="source"><a :href="company.sourceUrl" target="_blank" rel="noopener noreferrer">官方网站</a></p><el-divider />
      <el-descriptions :column="2" border>
        <el-descriptions-item label="公司名称">{{ company.companyName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ company.companyPhone }}</el-descriptions-item>
        <el-descriptions-item label="公司简介" :span="2">{{ company.companyProfile }}</el-descriptions-item>
        <el-descriptions-item v-if="company.companyCulture" label="企业文化" :span="2">{{ company.companyCulture }}</el-descriptions-item>
        <el-descriptions-item v-if="company.companyPurport" label="公司宗旨" :span="2">{{ company.companyPurport }}</el-descriptions-item>
      </el-descriptions>
      <div class="back-btn">
        <el-button type="primary" @click="$router.back()">返回列表</el-button>
      </div>
    </div>
    <el-empty v-if="!loading && !company" description="航空公司不存在" />
  </div>
</template>

<script>
import { getCompanyDetail } from '../api/company'
import { getCompanyImage, companyImageError } from '../utils/companyImage'

export default {
  name: 'CompanyDetail',
  data() {
    return {
      company: null,
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
      const id = this.$route.params.id
      if (!id) return
      this.loading = true
      getCompanyDetail(id).then(res => {
        this.company = res.data
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    }
  }
}
</script>

<style scoped>
.company-detail {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}
.detail-container {
  background: #fff;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}
.detail-image {
  width: 100%;
  height: 400px;
  overflow: hidden;
  border-radius: 8px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}
.detail-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.company-name {
  text-align: center;
  font-size: 28px;
  color: #333;
  margin-bottom: 10px;
}
.back-btn {
  text-align: center;
  margin-top: 30px;
}
@media screen and (max-width: 768px) {
  .detail-container {
    padding: 20px 15px;
  }
  .detail-image {
    height: 260px;
  }
  .company-name {
    font-size: 22px;
  }
}
@media screen and (max-width: 576px) {
  .detail-image {
    height: 200px;
  }
}
</style>