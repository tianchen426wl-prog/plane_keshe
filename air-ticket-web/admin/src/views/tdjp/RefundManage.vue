<template>
  <div class="refund-manage">
    <el-card>
      <div slot="header" class="page-header">
        <span>退订管理</span>
      </div>
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="待审核" name="pending" />
        <el-tab-pane label="已通过" name="approved" /><el-tab-pane label="已拒绝" name="rejected" />
      </el-tabs>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="退订ID" width="80" />
        <el-table-column prop="ddid" label="订单ID" width="80" />
        <el-table-column label="航班信息" min-width="180">
          <template slot-scope="{ row }">
            {{ formatRoute(row.start, row.end) }}
            <br />
            <small>{{ row.companyName || '-' }}</small>
          </template>
        </el-table-column>
        <el-table-column label="出发时间" width="170">
          <template slot-scope="{ row }">
            {{ formatDateTime(row.starttime) }}
          </template>
        </el-table-column>
        <el-table-column label="用户" width="120">
          <template slot-scope="{ row }">
            {{ row.userName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="舱位" width="100">
          <template slot-scope="{ row }">
            {{ row.cabinName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="退订时间" width="170">
          <template slot-scope="{ row }">
            {{ formatDateTime(row.canceltime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template slot-scope="{ row }">
            <el-tag v-if="row.process === 0" type="warning">待审核</el-tag>
            <el-tag v-else-if="row.process === 1" type="success">已通过</el-tag>
            <el-tag v-else type="danger">已拒绝</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewReason" label="审核意见" min-width="150" />
        <el-table-column label="操作" class-name="table-actions" align="center" :width="$viewport.compact ? 120 : 210" fixed="right" v-if="activeTab === 'pending'">
          <template slot-scope="{ row }">
            <el-button
              type="success"
              size="small"
              :loading="approveLoadingMap[row.id]"
              @click="handleApprove(row)"
            >审核通过</el-button>
            <el-button size="small" type="danger" @click="handleReject(row)">拒绝</el-button>
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
  </div>
</template>

<script>
import { getRefundList, approveRefund, rejectRefund } from '@/api/tdjp'

export default {
  name: 'RefundManage',
  data() {
    return {
      loading: false,
      activeTab: 'pending',
      tableData: [],
      pageNum: 1,
      pageSize: 10,
      total: 0,
      approveLoadingMap: {}
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    formatRoute(start, end) {
      if (!start || !end) {
        return '未关联航班'
      }
      return `${start} → ${end}`
    },
    formatDateTime(dateStr) {
      if (!dateStr) {
        return '-'
      }
      try {
        const date = new Date(dateStr)
        if (isNaN(date.getTime())) {
          return '-'
        }
        const year = date.getFullYear()
        const month = String(date.getMonth() + 1).padStart(2, '0')
        const day = String(date.getDate()).padStart(2, '0')
        const hours = String(date.getHours()).padStart(2, '0')
        const minutes = String(date.getMinutes()).padStart(2, '0')
        const seconds = String(date.getSeconds()).padStart(2, '0')
        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
      } catch (e) {
        return '-'
      }
    },
    async fetchList() {
      this.loading = true
      const process = this.activeTab === 'pending' ? 0 : (this.activeTab === 'approved' ? 1 : 2)
      try {
        const res = await getRefundList({
          page: this.pageNum,
          pageSize: this.pageSize,
          process
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
    handleTabClick() {
      this.pageNum = 1
      this.fetchList()
    },
    async handleReject(row) {
      try {
        const { value } = await this.$prompt('请输入拒绝原因', '拒绝退订', { inputValidator: v => !!v && v.trim().length > 0 && v.length <= 500 })
        await rejectRefund(row.id, value)
        this.$message.success('已拒绝，订单恢复为已购票')
        this.fetchList()
      } catch(e) { /* request interceptor reports errors; dialog cancel is expected */ }
    },
    handleApprove(row) {
      this.$confirm('确定审核通过该退订申请吗？', '审核确认', {
        confirmButtonText: '确定通过',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        this.$set(this.approveLoadingMap, row.id, true)
        try {
          await approveRefund(row.id)
          this.$message.success('审核通过成功')
          this.fetchList()
        } catch (e) {
          // error shown by interceptor
        } finally {
          this.$set(this.approveLoadingMap, row.id, false)
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
.pagination {
  margin-top: 15px;
  text-align: right;
}
</style>
