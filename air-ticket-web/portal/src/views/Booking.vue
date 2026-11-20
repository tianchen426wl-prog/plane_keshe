<template>
  <div class="booking-page">
    <div class="booking-steps">
      <span>① 选择航班</span><strong>② 确认预订</strong><span>③ 预订结果</span>
    </div>
    <div class="page-heading">
      <div>
        <h1>{{ paid ? '预订已完成' : '确认你的行程' }}</h1>
        <p>核对乘机信息和费用明细，确认后完成预订。</p>
      </div>
      <el-tag v-if="hold" :type="remaining < 60 ? 'danger' : 'warning'">{{
        countdown
      }}</el-tag>
    </div>
    <div v-if="error" class="ui-error" role="alert">
      {{ error }} <el-button type="text" @click="load">重新加载</el-button>
    </div>
    <el-skeleton v-if="loading" :rows="6" animated />
    <div v-else-if="paid" class="surface result-panel">
      <i class="el-icon-circle-check" />
      <h2>预订成功</h2>
      <p class="muted">订单已保存。可在“我的订单”查看行程、金额并申请退订。</p>
      <el-button type="primary" @click="orders">查看我的订单</el-button>
    </div>
    <div v-else-if="hold" class="booking-grid">
      <div>
        <article class="surface">
          <div class="route-title">{{ hold.start }} → {{ hold.end }}</div>
          <dl>
            <div>
              <dt>航空公司</dt>
              <dd>{{ hold.companyName }}</dd>
            </div>
            <div>
              <dt>出发时间</dt>
              <dd>{{ format(hold.starttime) }}</dd>
            </div>
            <div>
              <dt>乘机地点</dt>
              <dd>{{ hold.place }}</dd>
            </div>
            <div>
              <dt>所选舱位</dt>
              <dd>{{ hold.cabinName }}</dd>
            </div>
            <div v-if="hold.priceSnapshot"><dt>座位</dt><dd>{{ hold.priceSnapshot.seats.join('、') }}</dd></div>
            <div v-if="hold.priceSnapshot"><dt>乘机人</dt><dd>{{ hold.priceSnapshot.passengers.join('、') }}</dd></div>
            <div>
              <dt>占位编号</dt>
              <dd>{{ hold.holdId }}</dd>
            </div>
          </dl>
        </article>
        <article class="surface rules">
          <h3>预订须知</h3>
          <p>
            当前占位已锁定单价；数量和舱位需要调整时，请取消占位后重新选择。未完成占位到期自动释放。
          </p>
          <el-alert
            title="请在占位有效期内完成确认，以免座位自动释放"
            type="info"
            :closable="false"
            show-icon
          />
        </article>
      </div>
      <aside class="surface summary">
        <h3>费用明细</h3>
        <dl v-if="hold.priceSnapshot"><div v-for="item in hold.priceSnapshot.items" :key="item.code"><dt>{{ item.name }}</dt><dd>¥{{ item.amount }}</dd></div></dl>
        <p v-if="hold.priceSnapshot" class="muted">{{ hold.priceSnapshot.policy.description }}</p>
        <dl>
          <div>
            <dt>基础单价</dt>
            <dd>¥{{ hold.cabinPrice }}</dd>
          </div>
          <div>
            <dt>购买数量</dt>
            <dd>{{ hold.quantity }} 张</dd>
          </div>
        </dl>
        <div class="total">
          <span>合计</span><strong class="money">¥{{ hold.totalCost }}</strong>
        </div>
        <el-button
          type="primary"
          data-testid="confirm-booking"
          :loading="paying"
          :disabled="remaining <= 0"
          @click="pay"
          >确认预订</el-button
        ><el-button
          class="cancel-hold"
          :loading="cancelling"
          :disabled="paying"
          @click="cancel"
          >取消占位并释放</el-button
        ><el-button type="text" @click="orders"
          >稍后确认，前往我的订单</el-button
        >
      </aside>
    </div>
    <div v-else-if="!error" class="surface result-panel">
      <h2>当前占位已处理或已过期</h2>
      <p class="muted">
        如已确认购买，请前往订单页查看；如已过期，请重新查询航班。
      </p>
      <el-button type="primary" @click="orders">查看我的订单</el-button
      ><el-button @click="$router.push('/flight')">重新选航班</el-button>
    </div>
  </div>
</template>
<script>
import { createOrder, cancelHold } from '../api/jpdd'
import { getHoldDetail } from '../api/hbxx'
export default {
  data() {
    return {
      hold: null,
      loading: false,
      error: '',
      paid: false,
      paying: false,
      cancelling: false,
      now: Date.now(),
      clockOffset: 0,
      timer: null,
    }
  },
  watch: {
    '$route.params.holdId'() {
      this.paid = false
      this.hold = null
      this.load()
    },
  },
  computed: {
    remaining() {
      return this.hold
        ? Math.max(
            0,
            Math.floor(
              (new Date(this.hold.expireTime).getTime() - this.now) / 1000
            )
          )
        : 0
    },
    countdown() {
      return this.remaining > 0
        ? `占位剩余 ${Math.floor(this.remaining / 60)}:${String(
            this.remaining % 60
          ).padStart(2, '0')}`
        : '占位已过期'
    },
  },
  created() {
    this.load()
    this.timer = setInterval(() => {
      this.now = Date.now() + this.clockOffset
    }, 1000)
  },
  beforeDestroy() {
    clearInterval(this.timer)
  },
  methods: {
    format(v) {
      return new Date(v).toLocaleString('zh-CN', {
        timeZone: 'Asia/Shanghai',
        hour12: false,
      })
    },
    orders() {
      this.$router.push({
        path: '/user/orders',
        query: { tab: this.paid ? '1' : 'hold' },
      })
    },
    async load() {
      const requestedId = this.$route.params.holdId
      this.loading = true
      this.error = ''
      this.hold = null
      try {
        const r = await getHoldDetail(requestedId)
        if (requestedId !== this.$route.params.holdId) return
        this.hold = r.data.status === 0 ? {...r.data,cabinPrice:r.data.unitPrice} : null
        if(r.data.serverTime){this.clockOffset=new Date(r.data.serverTime).getTime()-Date.now();this.now=Date.now()+this.clockOffset}
      } catch (e) {
        this.error = '暂时无法读取占位信息，请重试。'
      } finally {
        this.loading = false
      }
    },
    async pay() {
      if (!this.hold || this.paying) return
      const requestedId = this.$route.params.holdId
      this.paying = true
      try {
        await createOrder({ holdId: this.hold.holdId })
        if (requestedId === this.$route.params.holdId) this.paid = true
        this.$message.success('购买成功！')
      } catch (e) {
        this.error = e.message || '确认失败，请刷新核对订单后重试。'
      } finally {
        this.paying = false
      }
    },
    async cancel() {
      try {
        await this.$confirm('取消后将释放当前占位，确定继续吗？', '取消占位')
        this.cancelling = true
        await cancelHold(this.hold.holdId)
        this.$message.success('占位已取消')
        this.$router.push('/flight')
      } catch (e) {
      } finally {
        this.cancelling = false
      }
    },
  },
}
</script>
<style scoped>
.booking-steps {
  display: flex;
  gap: 28px;
  margin-bottom: 32px;
  color: var(--muted);
}
.booking-steps strong {
  color: var(--brand);
}
.booking-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 24px;
}
.rules {
  margin-top: 24px;
}
.rules p {
  color: var(--muted);
}
dl {
  margin: 18px 0 0;
}
dl > div {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 14px 0;
  border-bottom: 1px solid #eef1f6;
}
dt {
  color: var(--muted);
}
dd {
  margin: 0;
  text-align: right;
}
.summary {
  align-self: start;
  position: sticky;
  top: 24px;
}
.summary > .el-button {
  display: block;
  width: 100%;
  margin: 12px 0 0;
}
.total {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 28px 0;
}
.result-panel {
  text-align: center;
  padding: 50px 24px;
}
.result-panel i {
  font-size: 54px;
  color: #15803d;
}
.result-panel h2 {
  margin-top: 16px;
}
@media (max-width: 800px) {
  .booking-grid {
    grid-template-columns: 1fr;
  }
  .summary {
    position: static;
  }
  .booking-steps {
    gap: 16px;
    font-size: 12px;
  }
  .page-heading {
    flex-wrap: wrap;
  }
}
</style>
