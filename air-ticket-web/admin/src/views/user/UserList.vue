<template>
  <div class="user-list">
    <el-card>
      <div slot="header" class="page-header">
        <span>用户管理</span>
      </div>
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="displayData" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column label="性别" width="80">
          <template slot-scope="{ row }">
            {{ row.gender === 0 ? '男' : row.gender === 1 ? '女' : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="identity" label="身份证" min-width="180" />
      </el-table>
      <el-pagination
        class="pagination"
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page.sync="pageNum"
        @current-change="handlePageChange"
      />
    </el-card>
  </div>
</template>

<script>
import { getUserList } from '@/api/admin'

export default {
  name: 'UserList',
  data() {
    return {
      loading: false,
      searchForm: { username: '' },
      allData: [],
      tableData: [],
      pageNum: 1,
      pageSize: 10,
      total: 0
    }
  },
  computed: {
    displayData() {
      return this.tableData
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    async fetchList() {
      this.loading = true
      try {
        const res = await getUserList({ page: this.pageNum, pageSize: this.pageSize, username: this.searchForm.username })
        this.tableData = res.data.records || []
        this.total = res.data.total || 0
      } catch (e) {
        // error shown by interceptor
      } finally {
        this.loading = false
      }
    },
    updatePageData() {
      const start = (this.pageNum - 1) * this.pageSize
      const end = start + this.pageSize
      this.tableData = this.allData.slice(start, end)
    },
    handleSearch() {
      this.pageNum = 1
      this.fetchList()
    },
    handleReset() {
      this.searchForm.username = ''
      this.pageNum = 1
      this.fetchList()
    },
    handlePageChange() {
      this.fetchList()
    }
  }
}
</script>

<style scoped>
.page-header {
  font-size: 16px;
  font-weight: bold;
}
.search-form {
  margin-bottom: 10px;
}
.pagination {
  margin-top: 15px;
  text-align: right;
}
</style>