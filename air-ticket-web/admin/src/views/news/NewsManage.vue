<template>
  <div class="news-manage">
    <el-card>
      <div slot="header" class="page-header">
        <span>资讯管理</span>
      </div>
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="标题">
          <el-input v-model="searchForm.title" placeholder="请输入资讯标题" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
          <el-button type="success" icon="el-icon-plus" @click="handleAdd">新增</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column label="配图URL" min-width="200">
          <template slot-scope="{ row }">
            <span v-if="row.picture">{{ row.picture }}</span>
            <span v-else style="color:#c0c4cc">无</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template slot-scope="{ row }">
            {{ row.createTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" class-name="table-actions" align="center" :width="$viewport.compact ? 120 : 190" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="primary" size="small" icon="el-icon-edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" icon="el-icon-delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pagination"
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page.sync="pageNum"
        @current-change="fetchList"
      />
    </el-card>

    <!-- Add/Edit Dialog -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="550px" :close-on-click-modal="false">
      <el-form ref="newsForm" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="简介" prop="profile">
          <el-input v-model="form.profile" type="textarea" :rows="4" placeholder="请输入简介内容" />
        </el-form-item>
        <el-form-item label="配图URL" prop="picture">
          <el-input v-model="form.picture" placeholder="请输入配图URL" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getNewsList, addNews, updateNews, deleteNews } from '@/api/news'

export default {
  name: 'NewsManage',
  data() {
    return {
      loading: false,
      submitLoading: false,
      searchForm: { title: '' },
      tableData: [],
      pageNum: 1,
      pageSize: 10,
      total: 0,
      dialogVisible: false,
      dialogTitle: '新增资讯',
      isEdit: false,
      editId: null,
      form: { title: '', profile: '', picture: '' },
      rules: {
        title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
        profile: [{ required: true, message: '请输入简介', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    async fetchList() {
      this.loading = true
      try {
        const res = await getNewsList({
          page: this.pageNum,
          pageSize: this.pageSize,
          title: this.searchForm.title || undefined
        })
        const data = res.data
        this.tableData = data.records || data.list || []
        this.total = data.total || 0
      } catch (e) {
        // error shown by interceptor
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.pageNum = 1
      this.fetchList()
    },
    handleReset() {
      this.searchForm.title = ''
      this.pageNum = 1
      this.fetchList()
    },
    handleAdd() {
      this.dialogTitle = '新增资讯'
      this.isEdit = false
      this.editId = null
      this.form = { title: '', profile: '', picture: '' }
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.newsForm && this.$refs.newsForm.clearValidate()
      })
    },
    handleEdit(row) {
      this.dialogTitle = '编辑资讯'
      this.isEdit = true
      this.editId = row.id
      this.form = { title: row.title, profile: row.profile, picture: row.picture || '' }
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.newsForm && this.$refs.newsForm.clearValidate()
      })
    },
    handleSubmit() {
      this.$refs.newsForm.validate(async valid => {
        if (!valid) return
        this.submitLoading = true
        try {
          if (this.isEdit) {
            await updateNews({ ...this.form, id: this.editId })
            this.$message.success('修改成功')
          } else {
            await addNews(this.form)
            this.$message.success('新增成功')
          }
          this.dialogVisible = false
          this.fetchList()
        } catch (e) {
          // error shown by interceptor
        } finally {
          this.submitLoading = false
        }
      })
    },
    handleDelete(row) {
      this.$confirm('确定删除该资讯吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteNews(row.id)
          this.$message.success('删除成功')
          this.fetchList()
        } catch (e) {
          // error shown by interceptor
        }
      }).catch(() => {})
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