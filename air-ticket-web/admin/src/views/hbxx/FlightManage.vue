<template>
  <div class="flight-manage">
    <FlightOperations/>
    <el-card>
      <div slot="header" class="page-header">
        <span>航班管理</span>
      </div>
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="出发地">
          <el-input v-model="searchForm.start" placeholder="请输入出发地" clearable />
        </el-form-item>
        <el-form-item label="目的地">
          <el-input v-model="searchForm.end" placeholder="请输入目的地" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
          <el-button type="success" icon="el-icon-plus" @click="handleAdd">新增</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="hbid" label="航班ID" width="80" />
        <el-table-column prop="companyName" label="航空公司" width="160" />
        <el-table-column label="出发地→目的地" min-width="180">
          <template slot-scope="{ row }">
            {{ row.start }} → {{ row.end }}
          </template>
        </el-table-column>
        <el-table-column label="出发时间" width="170">
          <template slot-scope="{ row }">
            {{ row.starttime }}
          </template>
        </el-table-column>
        <el-table-column prop="number" label="余票" width="80" />
        <el-table-column label="票价" width="100">
          <template slot-scope="{ row }">
            ¥{{ row.price }}
          </template>
        </el-table-column>
        <el-table-column label="时长" width="100">
          <template slot-scope="{ row }">
            {{ row.duration || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="place" label="乘机地点" width="150" />
        <el-table-column label="操作" class-name="table-actions" align="center" :width="$viewport.compact ? 120 : 280" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="info" size="small" icon="el-icon-s-grid" @click="handleCabinManage(row)">舱位</el-button>
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
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="600px" :close-on-click-modal="false">
      <el-form ref="flightForm" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="航空公司" prop="companyId">
          <el-select v-model="form.companyId" placeholder="请选择航空公司" style="width:100%">
            <el-option
              v-for="c in companyList"
              :key="c.companyId"
              :label="c.companyName"
              :value="c.companyId"
            />
          </el-select>
        </el-form-item>
          <el-form-item label="销售状态"><el-select v-model="form.saleStatus"><el-option label="可售" :value="1"/><el-option label="停售" :value="0"/></el-select></el-form-item>
      <el-form-item label="行程类别" prop="xclb">
          <el-input v-model="form.xclb" placeholder="请输入行程类别（国内/国际）" />
        </el-form-item>
        <el-form-item label="出发地" prop="start">
          <el-input v-model="form.start" placeholder="请输入出发地" />
        </el-form-item>
        <el-form-item label="目的地" prop="end">
          <el-input v-model="form.end" placeholder="请输入目的地" />
        </el-form-item>
        <el-form-item label="出发时间" prop="starttime">
          <el-date-picker
            v-model="form.starttime"
            type="datetime"
            placeholder="选择出发时间"
            value-format="yyyy-MM-dd HH:mm:ss"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="票价" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="初始座位数" prop="number">
          <el-input-number :disabled="isEdit" v-model="form.number" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="时长" prop="duration">
          <el-input v-model="form.duration" placeholder="请输入飞行时长" />
        </el-form-item>
        <el-form-item label="乘机地点" prop="place">
          <el-input v-model="form.place" placeholder="请输入乘机地点" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 舱位管理对话框 -->
    <el-dialog title="舱位管理" :visible.sync="cabinDialogVisible" width="830px" :close-on-click-modal="false">
      <div v-if="cabinFlight" style="margin-bottom:15px;">
        <span>航班：{{ cabinFlight.start }} → {{ cabinFlight.end }}（{{ cabinFlight.starttime }}）</span>
      </div>
      <el-table :data="cabinList" border stripe v-loading="cabinLoading">
        <el-table-column prop="cabinName" label="舱位" width="100" />
        <el-table-column label="票价" width="150">
          <template slot-scope="{ row }">
            <el-input-number v-model="row.price" :min="0" :precision="2" :controls="false" size="small" style="width:120px" />
          </template>
        </el-table-column>
        <el-table-column label="总座位数" width="120">
          <template slot-scope="{ row }">
            <el-input-number v-model="row.totalNumber" :min="0" size="small" style="width:100px" />
          </template>
        </el-table-column>
        <el-table-column prop="availableNumber" label="可售余票" width="100" />
        <el-table-column label="状态" width="100"><template slot-scope="{ row }"><el-select v-model="row.status" size="small"><el-option label="可售" :value="1"/><el-option label="停用" :value="0"/></el-select></template></el-table-column>
        <el-table-column label="操作" class-name="table-actions" align="center" width="80">
          <template slot-scope="{ row }">
            <el-button type="primary" size="mini" @click="saveCabin(row)">保存</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div slot="footer">
        <el-button @click="cabinDialogVisible = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import FlightOperations from './FlightOperations.vue'
import { searchList, addFlight, updateFlight, deleteFlight, getCompanyAll, getCabins, updateCabin } from '@/api/hbxx'

export default {
  components:{FlightOperations},
  name: 'FlightManage',
  data() {
    return {
      loading: false,
      submitLoading: false,
      searchForm: { start: '', end: '' },
      tableData: [],
      pageNum: 1,
      pageSize: 10,
      total: 0,
      dialogVisible: false,
      dialogTitle: '新增航班',
      isEdit: false,
      editId: null,
      companyList: [],
      form: {
        saleStatus: 1,
        companyId: null,
        xclb: '',
        start: '',
        end: '',
        starttime: '',
        price: 0,
        number: 0,
        duration: '',
        place: ''
      },
      rules: {
        companyId: [{ required: true, message: '请选择航空公司', trigger: 'change' }],
        start: [{ required: true, message: '请输入出发地', trigger: 'blur' }],
        end: [{ required: true, message: '请输入目的地', trigger: 'blur' }],
        starttime: [{ required: true, message: '请选择出发时间', trigger: 'change' }],
        price: [{ required: true, message: '请输入票价', trigger: 'blur' }],
        number: [{ required: true, message: '请输入余票', trigger: 'blur' }]
      },
      cabinDialogVisible: false,
      cabinFlight: null,
      cabinList: [],
      cabinLoading: false
    }
  },
  created() {
    this.fetchList()
    this.fetchCompanies()
  },
  methods: {
    async fetchList() {
      this.loading = true
      try {
        const res = await searchList({
          page: this.pageNum,
          pageSize: this.pageSize,
          start: this.searchForm.start || undefined,
          end: this.searchForm.end || undefined
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
    async fetchCompanies() {
      try {
        const res = await getCompanyAll()
        const data = res.data
        this.companyList = data.records || data.list || data || []
      } catch (e) {
        // ignore
      }
    },
    handleSearch() {
      this.pageNum = 1
      this.fetchList()
    },
    handleReset() {
      this.searchForm = { start: '', end: '' }
      this.pageNum = 1
      this.fetchList()
    },
    handleAdd() {
      this.dialogTitle = '新增航班'
      this.isEdit = false
      this.editId = null
      this.form = {
        saleStatus: 1,
        companyId: null,
        xclb: '',
        start: '',
        end: '',
        starttime: '',
        price: 0,
        number: 0,
        duration: '',
        place: ''
      }
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.flightForm && this.$refs.flightForm.clearValidate()
      })
    },
    handleEdit(row) {
      this.dialogTitle = '编辑航班'
      this.isEdit = true
      this.editId = row.hbid
      this.form = {
        saleStatus: row.saleStatus,
        flightNo: row.flightNo,
        companyId: row.companyId,
        xclb: row.xclb || '',
        start: row.start,
        end: row.end,
        starttime: row.starttime,
        price: row.price,
        number: row.number,
        duration: row.duration || '',
        place: row.place || ''
      }
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.flightForm && this.$refs.flightForm.clearValidate()
      })
    },
    handleSubmit() {
      this.$refs.flightForm.validate(async valid => {
        if (!valid) return
        this.submitLoading = true
        try {
          if (this.isEdit) {
            await updateFlight({ ...this.form, hbid: this.editId })
            this.$message.success('修改成功')
          } else {
            await addFlight(this.form)
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
      this.$confirm('确定删除该航班吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteFlight(row.hbid)
          this.$message.success('删除成功')
          this.fetchList()
        } catch (e) {
          // error shown by interceptor
        }
      }).catch(() => {})
    },
    async handleCabinManage(row) {
      this.cabinFlight = row
      this.cabinDialogVisible = true
      this.cabinLoading = true
      try {
        const res = await getCabins(row.hbid)
        this.cabinList = res.data || []
      } catch (e) {
        this.cabinList = []
      } finally {
        this.cabinLoading = false
      }
    },
    async saveCabin(row) {
      try {
        await updateCabin(row)
        this.$message.success('舱位已更新')
        const res = await getCabins(this.cabinFlight.hbid)
        this.cabinList = res.data || []
      } catch (e) {
        // error shown by interceptor
      }
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
