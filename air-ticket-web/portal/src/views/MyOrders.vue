<template>
  <div class="my-orders">
    <h2 class="page-title">我的订单</h2>
    <el-tabs v-model="activeTab" @tab-click="handleTabChange">
      <el-tab-pane label="待支付" name="hold" />
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="已购票" name="1" />
      <el-tab-pane label="退订中" name="2" />
      <el-tab-pane label="已退订" name="3" />
    </el-tabs>

    <el-dialog title="费用与座位" :visible.sync="detailsVisible" width="min(650px,94vw)"><div v-if="orderDetails"><p v-for="p in orderDetails.passengers" :key="p.seatNo">{{ p.passengerName }} · {{ p.seatNo }}</p><div v-if="orderDetails.priceSnapshot"><p v-for="i in orderDetails.priceSnapshot.items" :key="i.code">{{ i.name }}：¥{{ i.amount }}</p><p>{{ orderDetails.priceSnapshot.policy.description }}</p></div><p v-else>历史订单金额未拆分，按原有规则处理。</p><p>订单总额 ¥{{ orderDetails.cost }}</p><p v-if="orderDetails.refundAmount != null">已审核退款 ¥{{ orderDetails.refundAmount }}</p></div></el-dialog>
    <!-- 待支付占位列表 -->
    <div v-if="activeTab === 'hold'" v-loading="loading">
      <div v-if="holdList.length > 0" class="hold-list">
        <el-card
          v-for="item in holdList"
          :key="item.holdId"
          class="hold-card"
          :class="{ 'hold-expiring': getRemainSeconds(item.expireTime) <= 60 }"
        >
          <div class="hold-card-header">
            <span class="hold-id">占位单号：{{ item.holdId }}</span>
            <el-tag type="warning" size="small">待支付</el-tag>
          </div>
          <el-descriptions :column="2" border size="small" class="hold-desc">
            <el-descriptions-item label="航空公司">{{
              item.companyName || '-'
            }}</el-descriptions-item>
            <el-descriptions-item label="舱位">{{
              item.cabinName || '-'
            }}</el-descriptions-item>
            <el-descriptions-item label="出发地">{{
              item.start || '-'
            }}</el-descriptions-item>
            <el-descriptions-item label="目的地">{{
              item.end || '-'
            }}</el-descriptions-item>
            <el-descriptions-item label="出发时间">{{
              formatDateTime(item.starttime)
            }}</el-descriptions-item>
            <el-descriptions-item label="乘机地点">{{
              item.place || '-'
            }}</el-descriptions-item>
            <el-descriptions-item label="数量">{{
              item.quantity
            }}</el-descriptions-item>
            <el-descriptions-item label="单价"
              >¥{{ item.cabinPrice }}</el-descriptions-item
            >
            <el-descriptions-item label="总费用" :span="2">
              <span style="color: #f56c6c; font-size: 18px; font-weight: bold"
                >¥{{ item.totalCost }}</span
              >
            </el-descriptions-item>
          </el-descriptions>
          <div class="hold-countdown">
            <el-alert
              :title="'支付剩余时间：' + formatCountdown(item.expireTime)"
              :type="
                getRemainSeconds(item.expireTime) <= 60 ? 'error' : 'warning'
              "
              :closable="false"
              show-icon
            />
          </div>
          <div class="hold-actions">
            <el-button
              type="primary"
              icon="el-icon-wallet"
              :loading="payingId === item.holdId"
              :disabled="getRemainSeconds(item.expireTime) <= 0"
              @click="$router.push('/booking/' + item.holdId)"
            >
              立即支付
            </el-button>
            <el-button
              type="danger"
              icon="el-icon-close"
              :loading="cancellingId === item.holdId"
              @click="handleCancelHold(item)"
            >
              取消占位
            </el-button>
          </div>
        </el-card>
      </div>
      <el-empty
        v-if="!loading && holdList.length === 0"
        description="暂无待支付订单"
      />
    </div>

    <!-- 已购订单列表 -->
    <div v-else v-loading="loading">
      <article
        v-for="order in list"
        :key="order.ddid"
        class="order-card surface"
        :data-testid="'order-' + order.ddid"
      >
        <header>
          <div>
            <small class="muted">订单号</small>
            <span>{{ order.orderNo || order.ddid }}</span>
          </div>
          <el-tag
            :type="
              order.status === 1
                ? 'success'
                : order.status === 2
                ? 'warning'
                : 'info'
            "
            >{{
              { 1: '已购票', 2: '退订中', 3: '已退订' }[order.status]
            }}</el-tag
          >
        </header>
        <div class="order-body">
          <div>
            <div class="route-title">
              {{ formatRoute(order.start, order.end) }}
            </div>
            <p class="muted">
              {{ formatDateTime(order.starttime) }} ·
              {{ order.companyName || '历史航班' }}
            </p>
            <span
              >{{ order.cabinName || '历史舱位' }} ·
              {{ order.quantity }} 张</span
            >
          </div>
          <div class="order-actions">
            <el-button type="text" @click="showDetails(order)">行程与订单详情</el-button><strong class="money">¥{{ order.cost }}</strong
            ><el-button
              v-if="order.status === 1"
              type="danger"
              plain
              size="small"
              data-testid="refund-order"
              @click="handleCancel(order)"
              >申请退订</el-button
            ><small v-else class="muted">{{
              order.status === 2 ? '等待管理员审核' : '退订已完成'
            }}</small>
          </div>
        </div>
        <footer class="muted">
          下单时间 {{ formatDateTime(order.ordertime) }}
        </footer>
      </article>
      <el-empty v-if="!loading && list.length === 0" description="暂无订单" />
    </div>

    <div v-if="activeTab !== 'hold' && total > 0" class="pagination">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 支付确认对话框 -->
    <el-dialog
      title="确认支付"
      :visible.sync="payDialogVisible"
      width="420px"
      :close-on-click-modal="false"
    >
      <div class="pay-dialog-content">
        <el-alert
          title="请确认订单信息，购买后可按退订规则申请审核"
          type="warning"
          :closable="false"
          show-icon
          style="margin-bottom: 15px"
        />
        <el-descriptions v-if="payingHold" :column="1" border size="small">
          <el-descriptions-item label="航线"
            >{{ payingHold.start }} → {{ payingHold.end }}</el-descriptions-item
          >
          <el-descriptions-item label="舱位">{{
            payingHold.cabinName
          }}</el-descriptions-item>
          <el-descriptions-item label="数量"
            >{{ payingHold.quantity }} 张</el-descriptions-item
          >
          <el-descriptions-item label="支付金额">
            <span style="color: #f56c6c; font-size: 20px; font-weight: bold"
              >¥{{ payingHold.totalCost }}</span
            >
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <div slot="footer">
        <el-button @click="payDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="paying" @click="confirmPay"
          >确认支付</el-button
        >
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { mapState } from 'vuex'
import { getOrderDetails, getRefundQuote } from '../api/hbxx'
import {
  getMyOrders,
  cancelOrder,
  getMyHolds,
  createOrder,
  cancelHold,
} from '../api/jpdd'

export default {
  name: 'MyOrders',
  data() {
    return {
      orderDetails:null,detailsVisible:false,
      activeTab: ['hold', '1', '2', '3', ''].includes(this.$route.query.tab)
        ? this.$route.query.tab
        : 'hold',
      list: [],
      holdList: [],
      page: 1,
      pageSize: 10,
      total: 0,
      loading: false,
      payingId: null,
      cancellingId: null,
      payDialogVisible: false,
      payingHold: null,
      paying: false,
      countdownTimer: null,
    }
  },
  computed: {
    ...mapState(['user']),
  },
  created() {
    this.loadData()
  },
  beforeDestroy() {
    this.clearCountdown()
  },
  methods: {
    loadData() {
      if (!this.user) return
      const userId = this.user.id || this.user.userId
      if (this.activeTab === 'hold') {
        this.loadHolds(userId)
      } else {
        this.loadOrders(userId)
      }
    },
    loadOrders(userId) {
      this.loading = true
      const params = {
        userId,
        page: this.page,
        pageSize: this.pageSize,
      }
      if (this.activeTab !== '') {
        params.status = this.activeTab
      }
      getMyOrders(params)
        .then((res) => {
          this.list = (res.data.records || []).map((order) => {
            try {
              const snap = JSON.parse(order.flightSnapshot || '{}')
              return { ...order, ...snap }
            } catch (e) {
              return order
            }
          })
          this.total = res.data.total || 0
          this.loading = false
        })
        .catch(() => {
          this.loading = false
        })
    },
    loadHolds(userId) {
      this.loading = true
      getMyHolds(userId)
        .then((res) => {
          this.holdList = res.data || []
          this.loading = false
          this.startCountdown()
        })
        .catch(() => {
          this.loading = false
        })
    },
    startCountdown() {
      this.clearCountdown()
      this.countdownTimer = setInterval(() => {
        let hasExpired = false
        for (const item of this.holdList) {
          if (this.getRemainSeconds(item.expireTime) <= 0) {
            hasExpired = true
            break
          }
        }
        // 触发响应式更新（通过修改数组引用）
        this.holdList = [...this.holdList]
        if (hasExpired) {
          this.loadData()
        }
      }, 1000)
    },
    clearCountdown() {
      if (this.countdownTimer) {
        clearInterval(this.countdownTimer)
        this.countdownTimer = null
      }
    },
    getRemainSeconds(expireTime) {
      if (!expireTime) return 0
      const expire = new Date(expireTime).getTime()
      const now = Date.now()
      const diff = expire - now
      return diff > 0 ? Math.floor(diff / 1000) : 0
    },
    formatCountdown(expireTime) {
      const seconds = this.getRemainSeconds(expireTime)
      if (seconds <= 0) return '已过期'
      const min = Math.floor(seconds / 60)
      const sec = seconds % 60
      return `${min}分${String(sec).padStart(2, '0')}秒`
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
    handleTabChange() {
      this.page = 1
      if (this.activeTab !== 'hold') {
        this.clearCountdown()
      }
      this.loadData()
    },
    handlePageChange(page) {
      this.page = page
      this.loadData()
    },
    handlePay(row) {
      this.payingHold = row
      this.payDialogVisible = true
    },
    confirmPay() {
      if (!this.payingHold) return
      this.paying = true
      const userId = this.user.id || this.user.userId
      createOrder({
        holdId: this.payingHold.holdId,
        userid: userId,
      })
        .then(() => {
          this.$message.success('支付成功！订单已生成')
          this.payDialogVisible = false
          this.payingHold = null
          this.activeTab = '1'
          this.loadData()
        })
        .catch(() => {
          // error shown by interceptor
        })
        .finally(() => {
          this.paying = false
        })
    },
    handleCancelHold(row) {
      this.$confirm('确定要取消该占位吗？取消后座位将释放。', '确认取消占位', {
        confirmButtonText: '确定取消',
        cancelButtonText: '再想想',
        type: 'warning',
      })
        .then(() => {
          this.cancellingId = row.holdId
          const userId = this.user.id || this.user.userId
          cancelHold(row.holdId, userId)
            .then(() => {
              this.$message.success('占位已取消')
              this.loadData()
            })
            .catch(() => {
              // error shown by interceptor
            })
            .finally(() => {
              this.cancellingId = null
            })
        })
        .catch(() => {})
    },
    showDetails(row){this.$router.push('/user/orders/'+row.ddid)},
    async handleCancel(row) {
      try {
        const quote=(await getRefundQuote(row.ddid)).data
        await this.$confirm(`预计退款 ¥${quote.amount}，退订费 ¥${quote.fee}。${quote.notice}`, '确认退订', {confirmButtonText:'确定退订',cancelButtonText:'取消',type:'warning'})
        await cancelOrder(row.ddid,this.user.id||this.user.userId)
        this.$message.success('退订申请已提交');this.loadData()
      }catch(e){}
    },
  },
}
</script>

<style scoped>
.order-card {
  margin: 18px 0;
}
.order-card header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  overflow-wrap: anywhere;
}
.order-body {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 24px 0;
}
.order-actions {
  display: flex;
  flex-direction: column;
  align-items: end;
  gap: 12px;
}
.order-card footer {
  padding-top: 14px;
  border-top: 1px solid var(--border);
  font-size: 12px;
}
@media (max-width: 600px) {
  .order-body {
    flex-direction: column;
  }
  .order-actions {
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
  }
  .order-card header {
    align-items: start;
  }
  .hold-card .el-descriptions {
    font-size: 12px;
  }
}

.my-orders {
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
.pagination {
  text-align: center;
  margin-top: 20px;
}
.hold-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}
.hold-card {
  border-left: 4px solid #e6a23c;
}
.hold-card.hold-expiring {
  border-left-color: #f56c6c;
  animation: pulse 1.5s infinite;
}
@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.3);
  }
  50% {
    box-shadow: 0 0 8px 4px rgba(245, 108, 108, 0.15);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(245, 108, 108, 0);
  }
}
.hold-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.hold-id {
  font-weight: bold;
  color: #333;
}
.hold-desc {
  margin-bottom: 12px;
}
.hold-countdown {
  margin-bottom: 12px;
}
.hold-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}
.pay-dialog-content {
  padding: 5px 0;
}
</style>
