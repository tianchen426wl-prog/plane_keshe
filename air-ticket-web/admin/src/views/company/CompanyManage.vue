<template>
  <div class="company-manage">
    <el-card>
      <div slot="header" class="page-header">
        <span>航空公司管理</span>
      </div>
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="公司名称">
          <el-input v-model="searchForm.keyword" placeholder="请输入公司名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
          <el-button type="success" icon="el-icon-plus" @click="handleAdd">新增</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="companyId" label="ID" width="80" />
        <el-table-column prop="companyName" label="公司名称" min-width="160" />
        <el-table-column prop="companyPhone" label="电话" width="150" />
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
      <el-form ref="companyForm" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="公司名称" prop="companyName">
          <el-input v-model="form.companyName" placeholder="请输入公司名称" />
        </el-form-item>
        <el-form-item label="公司简介" prop="companyProfile">
          <el-input v-model="form.companyProfile" type="textarea" :rows="3" placeholder="请输入公司简介" />
        </el-form-item>
        <el-form-item label="配图URL" prop="companyPicture">
          <el-input v-model="form.companyPicture" placeholder="请输入配图URL" />
        </el-form-item>
        <el-form-item label="公司文化" prop="companyCulture">
          <el-input v-model="form.companyCulture" type="textarea" :rows="3" placeholder="请输入公司文化" />
        </el-form-item>
        <el-form-item label="电话" prop="companyPhone">
          <el-input v-model="form.companyPhone" placeholder="请输入电话" />
        </el-form-item>
        <el-form-item label="宗旨" prop="companyPurport">
          <el-input v-model="form.companyPurport" type="textarea" :rows="2" placeholder="请输入公司宗旨" />
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
import { getList, addCompany, updateCompany, deleteCompany } from '@/api/company'

export default {
  name: 'CompanyManage',
  data() {
    return {
      loading: false,
      submitLoading: false,
      searchForm: { keyword: '' },
      tableData: [],
      pageNum: 1,
      pageSize: 10,
      total: 0,
      dialogVisible: false,
      dialogTitle: '新增航空公司',
      isEdit: false,
      editId: null,
      form: {
        companyName: '',
        companyProfile: '',
        companyPicture: '',
        companyCulture: '',
        companyPhone: '',
        companyPurport: ''
      },
      rules: {
        companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
        companyPhone: [{ required: true, message: '请输入电话', trigger: 'blur' }]
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
        const res = await getList({
          page: this.pageNum,
          pageSize: this.pageSize,
          keyword: this.searchForm.keyword || undefined
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
      this.searchForm.keyword = ''
      this.pageNum = 1
      this.fetchList()
    },
    handleAdd() {
      this.dialogTitle = '新增航空公司'
      this.isEdit = false
      this.editId = null
      this.form = { companyName: '', companyProfile: '', companyPicture: '', companyCulture: '', companyPhone: '', companyPurport: '' }
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.companyForm && this.$refs.companyForm.clearValidate()
      })
    },
    handleEdit(row) {
      this.dialogTitle = '编辑航空公司'
      this.isEdit = true
      this.editId = row.companyId
      this.form = { ...row }
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.companyForm && this.$refs.companyForm.clearValidate()
      })
    },
    handleSubmit() {
      this.$refs.companyForm.validate(async valid => {
        if (!valid) return
        this.submitLoading = true
        try {
          if (this.isEdit) {
            await updateCompany({ ...this.form, companyId: this.editId })
            this.$message.success('修改成功')
          } else {
            await addCompany(this.form)
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
      this.$confirm('确定删除该航空公司吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteCompany(row.companyId)
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
