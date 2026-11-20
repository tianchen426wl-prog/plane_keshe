<template>
  <div class="menu-manage">
    <el-card>
      <div slot="header" class="page-header">
        <span>菜单管理</span>
        <el-tag type="danger" size="small" style="margin-left:10px">仅超级管理员可访问</el-tag>
      </div>
      <el-form :inline="true" class="search-form">
        <el-form-item>
          <el-button type="success" icon="el-icon-plus" @click="handleAdd">新增菜单</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="path" label="路径" min-width="160" />
        <el-table-column prop="title" label="标题" width="140" />
        <el-table-column prop="icon" label="图标" min-width="240">
          <template slot-scope="{ row }">
            <span class="menu-icon-preview">
              <i :class="row.icon" v-if="row.icon" aria-hidden="true"></i>
              <code>{{ row.icon || '-' }}</code>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" class-name="table-actions" align="center" :width="$viewport.compact ? 120 : 190" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="primary" size="small" icon="el-icon-edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" icon="el-icon-delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Add/Edit Dialog -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="500px" :close-on-click-modal="false">
      <el-form ref="menuForm" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="路径" prop="path">
          <el-input v-model="form.path" placeholder="请输入路径，如 /admin/user" />
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入菜单标题" />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input v-model="form.icon" placeholder="请输入图标类名，如 el-icon-user" />
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
import { getMenuList, addMenu, updateMenu, deleteMenu } from '@/api/menu'

export default {
  name: 'MenuManage',
  data() {
    return {
      loading: false,
      submitLoading: false,
      tableData: [],
      dialogVisible: false,
      dialogTitle: '新增菜单',
      isEdit: false,
      editId: null,
      form: { path: '', title: '', icon: '' },
      rules: {
        path: [{ required: true, message: '请输入路径', trigger: 'blur' }],
        title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
        icon: [{ required: true, message: '请输入图标类名', trigger: 'blur' }]
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
        const res = await getMenuList()
        const data = res.data
        this.tableData = data.records || data.list || data || []
      } catch (e) {
        // error shown by interceptor
      } finally {
        this.loading = false
      }
    },
    handleAdd() {
      this.dialogTitle = '新增菜单'
      this.isEdit = false
      this.editId = null
      this.form = { path: '', title: '', icon: '' }
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.menuForm && this.$refs.menuForm.clearValidate()
      })
    },
    handleEdit(row) {
      this.dialogTitle = '编辑菜单'
      this.isEdit = true
      this.editId = row.id
      this.form = { path: row.path, title: row.title, icon: row.icon || '' }
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.menuForm && this.$refs.menuForm.clearValidate()
      })
    },
    handleSubmit() {
      this.$refs.menuForm.validate(async valid => {
        if (!valid) return
        this.submitLoading = true
        try {
          if (this.isEdit) {
            await updateMenu({ ...this.form, id: this.editId })
            this.$message.success('修改成功')
          } else {
            await addMenu(this.form)
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
      this.$confirm('确定删除该菜单吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteMenu(row.id)
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
</style>