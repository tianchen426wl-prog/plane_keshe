<template>
  <div class="order-detail">
    <div class="page-heading">
      <div>
        <h1>行程与订单详情</h1>
        <p>行程、座位、费用和退订进度集中查看。</p>
      </div>
      <el-button @click="$router.push('/user/orders')">返回我的订单</el-button>
    </div>
    <div v-if="error" class="ui-error" role="alert">
      {{ error }} <el-button type="text" @click="load">重新加载</el-button>
    </div>
    <el-skeleton v-if="loading" :rows="8" animated />
    <template v-else-if="order">
      <div class="actions">
        <el-button @click="print">打印演示行程卡</el-button
        ><el-button :disabled="!canExport" @click="download"
          >加入日历（ICS）</el-button
        ><el-button
          v-if="order.status === 1"
          type="danger"
          plain
          :loading="refunding"
          @click="refund"
          >申请退订</el-button
        >
      </div>
      <article class="surface itinerary-sheet">
        <div class="sheet-heading">
          <div>
            <small class="muted">课程演示行程卡</small>
            <h2>{{ flight.start }} → {{ flight.end }}</h2>
            <p>
              {{ flight.companyName }} · {{ flight.flightNo || "历史航班" }}
            </p>
          </div>
          <el-tag>{{ statusLabel }}</el-tag>
        </div>
        <p class="demo-notice">课程演示记录，不可用于真实乘机或报销。</p>
        <div class="route-grid">
          <section>
            <h3>出发</h3>
            <strong>{{ format(flight.starttime) }}</strong>
            <p>
              {{
                flight.departureAirportName || flight.place || "历史记录未提供"
              }}
              {{ flight.departureTerminal }}
            </p>
          </section>
          <section>
            <h3>抵达</h3>
            <strong>{{ format(flight.arrivalTime) }}</strong>
            <p>
              {{ flight.arrivalAirportName || "历史记录未提供" }}
              {{ flight.arrivalTerminal }}
            </p>
          </section>
        </div>
        <p class="muted">
          航班状态：{{
            flight.operationalStatus === "CANCELLED" ? "已取消" : "计划执行"
          }}
          · 时间均为北京时间。当前航班信息可能调整，请在出发前核对。
        </p>
        <p v-if="order.flightSnapshot" class="muted">
          购票时行程：{{ order.flightSnapshot.flightNo }} ·
          {{ format(order.flightSnapshot.starttime) }} ·
          {{ order.flightSnapshot.cabinName }}
        </p>
        <el-descriptions :column="1" border size="small"
          ><el-descriptions-item label="订单号">{{
            order.orderNo || order.ddid
          }}</el-descriptions-item
          ><el-descriptions-item label="购票时间">{{
            format(order.ordertime)
          }}</el-descriptions-item
          ><el-descriptions-item label="旅客及座位"
            ><span v-for="(p, i) in order.passengers" :key="i" class="passenger"
              >{{ p.passengerName }} · {{ p.seatNo || "未提供" }}</span
            ><span v-if="!order.passengers.length"
              >历史订单未提供旅客明细</span
            ></el-descriptions-item
          ><el-descriptions-item label="人数"
            >{{ order.quantity }}位成人</el-descriptions-item
          ></el-descriptions
        >
        <h3>费用与行李</h3>
        <template v-if="order.priceSnapshot"
          ><p
            v-for="item in order.priceSnapshot.items"
            :key="item.code"
            class="price-item"
          >
            <span>{{ item.name }}</span
            ><strong>¥{{ item.amount }}</strong>
          </p>
          <p>每人托运行李 {{ order.priceSnapshot.policy.baggageKg }}kg</p>
          <p class="muted">
            {{ order.priceSnapshot.policy.description }}
          </p></template
        >
        <p v-else class="muted">历史订单未拆分费用，沿用原有规则。</p>
        <p class="price-item total">
          <span>订单总额</span><strong>¥{{ order.cost }}</strong>
        </p>
        <p v-if="order.refundAmount != null">
          已审核退款 ¥{{ order.refundAmount }}
        </p>
      </article>
      <section class="surface progress">
        <h2>订单进度</h2>
        <el-timeline
          ><el-timeline-item
            v-for="(event, i) in order.timeline"
            :key="i"
            :timestamp="format(event.logTime)"
            ><strong>{{ actionLabel(event.action) }}</strong>
            <p>{{ event.remark }}</p></el-timeline-item
          ></el-timeline
        ><el-empty
          v-if="!order.timeline.length"
          description="历史订单暂无进度记录"
        /><el-alert
          v-if="order.refund"
          :title="refundLabel"
          :description="
            order.refund.reviewReason || '退订申请已提交，等待审核。'
          "
          :type="order.refund.process === 2 ? 'warning' : 'info'"
          :closable="false"
        />
      </section>
    </template>
  </div>
</template>
<script>
import { getOrderDetails, getRefundQuote } from "../api/hbxx";
import { cancelOrder } from "../api/jpdd";
import { calendarText } from "../utils/itinerary";
export default {
  data: () => ({
    order: null,
    loading: false,
    error: "",
    refunding: false,
    sequence: 0,
  }),
  computed: {
    flight() {
      return this.order.flight || {};
    },
    statusLabel() {
      return (
        { 1: "已购票", 2: "退订审核中", 3: "已退订" }[this.order.status] ||
        "历史订单"
      );
    },
    canExport() {
      return (
        this.order.status === 1 &&
        this.flight.operationalStatus !== "CANCELLED" &&
        !!this.flight.starttime &&
        !!this.flight.arrivalTime
      );
    },
    refundLabel() {
      return (
        { 0: "退订审核中", 1: "退订审核通过", 2: "退订审核未通过" }[
          this.order.refund.process
        ] || "退订进度"
      );
    },
  },
  created() {
    this.load();
  },
  watch: {
    "$route.params.id"() {
      this.load();
    },
  },
  beforeDestroy() {
    this.sequence++;
    document.body.classList.remove('itinerary-printing');
  },
  methods: {
    format(value) {
      return value
        ? new Date(value).toLocaleString("zh-CN", {
            timeZone: "Asia/Shanghai",
            hour12: false,
          })
        : "历史记录未提供";
    },
    actionLabel(action) {
      return (
        {
          CREATE: "订单已创建",
          PAY: "模拟支付成功",
          REFUND_REQUEST: "已申请退订",
          REFUND_APPROVE: "退订审核通过",
          REFUND_REJECT: "退订审核未通过",
        }[action] || "订单更新"
      );
    },
    async load() {
      const seq = ++this.sequence;
      this.loading = true;
      this.error = "";
      this.order = null;
      try {
        const r = await getOrderDetails(this.$route.params.id);
        if (seq === this.sequence) this.order = r.data;
      } catch (e) {
        if (seq === this.sequence)
          this.error = "订单详情暂时不可用，或该订单不属于当前账户。";
      } finally {
        if (seq === this.sequence) this.loading = false;
      }
    },
    print() {
      document.body.classList.add('itinerary-printing');
      const cleanup=()=>document.body.classList.remove('itinerary-printing');
      window.addEventListener('afterprint',cleanup,{once:true});
      try { window.print(); } catch(e) { cleanup(); }
    },
    download() {
      try {
        const blob = new Blob([calendarText(this.order)], {
          type: "text/calendar;charset=utf-8",
        });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = "演示行程-" + this.order.ddid + ".ics";
        link.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
      } catch (e) {
        this.$message.warning(e.message);
      }
    },
    async refund() {
      if (this.refunding) return;
      const id = this.order.ddid;
      this.refunding = true;
      try {
        const quote = (await getRefundQuote(id)).data;
        await this.$confirm(
          `预计退款 ¥${quote.amount}，退订费 ¥${quote.fee}。${quote.notice}`,
          "确认退订",
          { type: "warning" }
        );
        await cancelOrder(id);
        this.$message.success("退订申请已提交");
        await this.load();
      } catch (e) {
      } finally {
        this.refunding = false;
      }
    },
  },
};
</script>
<style scoped>
.order-detail {
  max-width: 1000px;
  margin: auto;
}
.actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin: 20px 0;
}
.actions .el-button {
  margin: 0;
}
.sheet-heading {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}
.route-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin: 24px 0;
}
.route-grid strong {
  font-size: 20px;
}
.demo-notice {
  padding: 12px;
  background: #fff6df;
  border-radius: 8px;
  color: #805c17;
}
.passenger {
  display: inline-block;
  margin-right: 18px;
}
.price-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}
.total {
  border-top: 1px solid var(--border);
  padding-top: 16px;
  font-size: 22px;
}
.progress {
  margin-top: 24px;
}
.progress h2 {
  margin-bottom: 24px;
}
@media (max-width: 600px) {
  .route-grid {
    grid-template-columns: 1fr;
    gap: 8px;
  }
  .itinerary-sheet {
    padding: 18px;
  }
  .sheet-heading {
    flex-wrap: wrap;
  }
}
</style>
<style>
@media print {
  body.itinerary-printing * {
    visibility: hidden;
  }
  body.itinerary-printing .itinerary-sheet,
  body.itinerary-printing .itinerary-sheet * {
    visibility: visible;
  }
  body.itinerary-printing .itinerary-sheet {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    box-sizing: border-box;
    box-shadow: none !important;
    border: 0 !important;
  }
  body.itinerary-printing .itinerary-sheet .el-descriptions {
    break-inside: avoid;
  }
}
</style>
