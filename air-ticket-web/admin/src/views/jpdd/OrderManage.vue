<template>
  <div class="order-manage">
    <el-card>
      <div slot="header" class="page-header">
        <span>订单管理</span>
      </div>
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="航班起降地">
          <el-input
            v-model="searchForm.flightName"
            placeholder="请输入出发地/目的地"
            clearable
          />
        </el-form-item>
        <el-form-item label="用户姓名">
          <el-input
            v-model="searchForm.userName"
            placeholder="请输入用户姓名"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch"
            >搜索</el-button
          >
          <el-button icon="el-icon-refresh" @click="handleReset"
            >重置</el-button
          >
        </el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column
          prop="orderNo"
          label="订单号"
          min-width="210"
          show-overflow-tooltip
        />

        <el-table-column label="航班" min-width="180">
          <template slot-scope="{ row }">
            {{ formatRoute(row.start, row.end) }}
          </template>
        </el-table-column>
        <el-table-column label="航空公司" width="140">
          <template slot-scope="{ row }">
            {{ row.companyName || '-' }}
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
        <el-table-column prop="quantity" label="数量" width="70" />
        <el-table-column label="舱位" width="100">
          <template slot-scope="{ row }">
            {{ row.cabinName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="费用" width="100">
          <template slot-scope="{ row }"> ¥{{ row.cost || 0 }} </template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template slot-scope="{ row }">
            <el-tag v-if="row.status === 1" type="success">已购票</el-tag>
            <el-tag v-else-if="row.status === 2" type="warning">退订中</el-tag>
            <el-tag v-else-if="row.status === 3" type="info">已退订</el-tag>
            <el-tag v-else type="info">{{ row.status || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="追踪" width="95" fixed="right"
          ><template slot-scope="{ row }"
            ><el-button size="mini" @click="showLogs(row)"
              >日志</el-button
            ></template
          ></el-table-column
        >
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
    <el-drawer
      title="订单生命周期日志"
      :visible.sync="logVisible"
      size="min(640px, 100vw)"
      ><el-descriptions v-if="selectedOrder" :column="1" border
        ><el-descriptions-item label="订单号">{{
          selectedOrder.orderNo || selectedOrder.ddid
        }}</el-descriptions-item
        ><el-descriptions-item label="航线">{{
          formatRoute(selectedOrder.start, selectedOrder.end)
        }}</el-descriptions-item
        ><el-descriptions-item label="金额"
          >¥{{ selectedOrder.cost }}</el-descriptions-item
        ><el-descriptions-item label="下单时间">{{
          formatDateTime(selectedOrder.ordertime)
        }}</el-descriptions-item></el-descriptions
      >
      <h3 style="margin: 28px 0 20px">交易时间线</h3>
      <el-timeline
        ><el-timeline-item
          v-for="event in logs"
          :key="event.log_id"
          :timestamp="formatDateTime(event.log_time)"
          ><strong>{{ actionName(event.action) }}</strong>
          <p>{{ event.remark || '状态变更' }}</p>
          <small class="muted">{{
            event.operator ? '操作人 ID ' + event.operator : '系统记录'
          }}</small></el-timeline-item
        ></el-timeline
      ></el-drawer
    >
  </div>
</template>

<script>
import { getOrderList, getOrderLog } from '@/api/jpdd'

export default {
  name: 'OrderManage',
  data() {
    return {
      loading: false,
      searchForm: { flightName: '', userName: '' },
      tableData: [],
      pageNum: 1,
      pageSize: 10,
      logVisible: false,
      logs: [],
      selectedOrder: null,
      total: 0,
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    actionName(action) {
      return (
        {
          CREATE: '创建订单',
          PAY: '支付确认',
          UPDATE: '订单状态变化',
          REFUND_REQUEST: '申请退订',
          REFUND_APPROVE: '退订审核通过',
          REFUND_REJECT: '退订审核拒绝',
        }[action] || action
      )
    },
    async showLogs(row) {
      try {
        const res = await getOrderLog(row.ddid)
        this.logs = res.data || []
        this.selectedOrder = row
        this.logVisible = true
      } catch (e) {}
    },
    formatRoute(start, end) {
      if (!start || !end || start === '-' || end === '-') {
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
      try {
        const res = await getOrderList({
          page: this.pageNum,
          pageSize: this.pageSize,
          flightName: this.searchForm.flightName || undefined,
          userName: this.searchForm.userName || undefined,
        })
        const data = res.data
        this.tableData = (data.records || data.list || []).map((order) => {
          try {
            return { ...order, ...JSON.parse(order.flightSnapshot || '{}') }
          } catch (e) {
            return order
          }
        })
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
      this.searchForm = { flightName: '', userName: '' }
      this.pageNum = 1
      this.fetchList()
    },
  },
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
