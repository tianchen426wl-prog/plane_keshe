<template>
  <div class="role-manage">
    <el-card>
      <div slot="header" class="page-header">
        <span>角色管理</span>
        <el-tag type="danger" size="small" style="margin-left:10px">仅超级管理员可访问</el-tag>
      </div>
      <el-form :inline="true" class="search-form">
        <el-form-item>
          <el-button type="success" icon="el-icon-plus" @click="handleAdd">新增管理员</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column label="角色" width="100">
          <template slot-scope="{ row }">
            <el-tag v-if="row.role === 0" type="primary">超管</el-tag>
            <el-tag v-else type="success">普通</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="菜单权限" min-width="200">
          <template slot-scope="{ row }">
            <template v-if="row.role === 0">
              <el-tag type="info" size="small">全部菜单</el-tag>
            </template>
            <template v-else>
              <el-tag
                v-for="menuName in getMenuNames(row.menuId)"
                :key="menuName"
                size="small"
                style="margin:2px"
              >{{ menuName }}</el-tag>
              <span v-if="getMenuNames(row.menuId).length === 0" style="color:#c0c4cc">无</span>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="操作" class-name="table-actions" align="center" :width="$viewport.compact ? 120 : 190" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="primary" size="small" icon="el-icon-edit" @click="handleEdit(row)">编辑</el-button>
            <el-button
              type="danger"
              size="small"
              icon="el-icon-delete"
              :disabled="isSelf(row)"
              @click="handleDelete(row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Add/Edit Dialog -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="550px" :close-on-click-modal="false">
      <el-form ref="roleForm" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="修改密码" prop="password" v-if="isEdit">
          <el-input v-model="form.password" type="password" placeholder="不修改则留空" show-password />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="form.role">
            <el-radio :label="0">超级管理员</el-radio>
            <el-radio :label="1">普通管理员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单权限" v-if="form.role === 1" prop="menuId">
          <el-checkbox-group v-model="checkedMenus">
            <el-checkbox
              v-for="m in allMenus"
              :key="m.id"
              :label="m.id"
            >{{ m.title }}</el-checkbox>
          </el-checkbox-group>
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
import { getAdminList, addAdmin, updateAdmin, deleteAdmin } from '@/api/admin'
import { getMenuList } from '@/api/menu'

export default {
  name: 'RoleManage',
  data() {
    return {
      loading: false,
      submitLoading: false,
      tableData: [],
      allMenus: [],
      dialogVisible: false,
      dialogTitle: '新增管理员',
      isEdit: false,
      editId: null,
      checkedMenus: [],
      form: {
        username: '',
        password: '',
        role: 1
      },
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }]
      }
    }
  },
  computed: {
    currentAdminId() {
      return this.$store.state.admin ? this.$store.state.admin.id : null
    }
  },
  created() {
    this.fetchList()
    this.fetchMenus()
  },
  methods: {
    async fetchList() {
      this.loading = true
      try {
        const res = await getAdminList()
        const data = res.data
        this.tableData = data.records || data.list || data || []
      } catch (e) {
        // error shown by interceptor
      } finally {
        this.loading = false
      }
    },
    async fetchMenus() {
      try {
        const res = await getMenuList()
        const data = res.data
        this.allMenus = (data.records || data.list || data || []).filter(m => !['/admin/menu','/admin/role'].includes(m.path))
      } catch (e) {
        // ignore
      }
    },
    getMenuNames(menuId) {
      if (!menuId || !this.allMenus.length) return []
      const ids = String(menuId).split(',').map(id => parseInt(id))
      return this.allMenus
        .filter(m => ids.includes(m.id))
        .map(m => m.title)
    },
    isSelf(row) {
      return row.id === this.currentAdminId
    },
    handleAdd() {
      this.dialogTitle = '新增管理员'
      this.isEdit = false
      this.editId = null
      this.form = { username: '', password: '', role: 1 }
      this.checkedMenus = []
      this.rules.password = [{ required: true, message: '请输入密码', trigger: 'blur' }]
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.roleForm && this.$refs.roleForm.clearValidate()
      })
    },
    handleEdit(row) {
      this.dialogTitle = '编辑管理员'
      this.isEdit = true
      this.editId = row.id
      this.form = {
        username: row.username,
        password: '',
        role: row.role
      }
      this.checkedMenus = row.menuId
        ? String(row.menuId).split(',').map(id => parseInt(id)).filter(id => !isNaN(id))
        : []
      this.rules.password = []
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.roleForm && this.$refs.roleForm.clearValidate()
      })
    },
    handleSubmit() {
      this.$refs.roleForm.validate(async valid => {
        if (!valid) return
        this.submitLoading = true
        try {
          const payload = {
            username: this.form.username,
            role: this.form.role
          }
          if (this.form.password) {
            payload.password = this.form.password
          }
          if (this.form.role === 1) {
            payload.menuId = this.checkedMenus.join(',')
          } else {
            payload.menuId = ''
          }

          if (this.isEdit) {
            await updateAdmin({ ...payload, id: this.editId })
            this.$message.success('修改成功')
          } else {
            await addAdmin(payload)
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
      if (this.isSelf(row)) {
        this.$message.warning('不能删除自己')
        return
      }
      this.$confirm('确定删除该管理员吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteAdmin(row.id)
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