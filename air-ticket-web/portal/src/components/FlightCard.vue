<template>
  <article class="flight-card surface" :data-testid="'flight-' + flight.hbid">
    <header>
      <strong>{{ flight.companyName || "航空公司" }}</strong
      ><span class="muted">{{ flight.flightNo || flight.hbid }}</span
      ><el-tag :type="canBook ? 'success' : 'info'">{{ saleLabel }}</el-tag>
    </header>
    <div class="flight-body">
      <div>
        <div class="flight-time">{{ time }}</div>
        <div class="route-title">{{ flight.start }} → {{ flight.end }}</div>
        <small class="muted">{{ date }} 起飞 · 抵达 {{ arrival }}</small>
      </div>
      <div class="flight-meta">
        <span>飞行时长 {{ flight.duration || "待确认" }}</span
        ><small class="muted"
          >{{ flight.departureAirportName || flight.place }} →
          {{ flight.arrivalAirportName || flight.end }}</small
        >
      </div>
      <div class="flight-action">
        <div>
          <small class="muted">含必付费用 · 最低可售价格</small>
          <div class="money">
            ¥{{
              flight.minTotal != null
                ? Number(flight.minTotal).toLocaleString()
                : "—"
            }}<small> / 张</small>
          </div>
          <small v-if="quantity > 1 && flight.minTotal != null" class="muted"
            >{{ quantity }}人合计 ¥{{
              Number(flight.minTotal * quantity).toLocaleString()
            }}
            起</small
          >
        </div>
        <el-button
          type="primary"
          :disabled="!canBook"
          data-testid="choose-flight"
          @click="$emit('select', flight)"
          >{{ loggedIn ? "选择舱位" : "登录后预订" }}</el-button
        >
      </div>
    </div>
  </article>
</template>
<script>
export default {
  props: {
    flight: { type: Object, required: true },
    loggedIn: Boolean,
    quantity: { type: Number, default: 1 },
  },
  computed: {
    canBook() {
      return this.saleLabel === "可预订" && this.flight.minTotal != null;
    },
    saleLabel() {
      const now = Date.now();
      if (this.flight.operationalStatus === "CANCELLED") return "已取消";
      if (this.flight.saleStatus !== 1) return "已停售";
      if (
        this.flight.saleOpenAt &&
        new Date(this.flight.saleOpenAt).getTime() > now
      )
        return "未开售";
      if (
        this.flight.saleCloseAt &&
        new Date(this.flight.saleCloseAt).getTime() <= now
      )
        return "已截止";
      return this.flight.number > 0 ? "可预订" : "已售罄";
    },
    arrival() {
      return this.flight.arrivalTime
        ? new Date(this.flight.arrivalTime).toLocaleString("zh-CN", {
            timeZone: "Asia/Shanghai",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit",
            hour12: false,
          })
        : "待确认";
    },
    time() {
      return new Date(this.flight.starttime).toLocaleTimeString("zh-CN", {
        hour: "2-digit",
        minute: "2-digit",
        timeZone: "Asia/Shanghai",
        hour12: false,
      });
    },
    date() {
      return new Date(this.flight.starttime).toLocaleDateString("zh-CN", {
        timeZone: "Asia/Shanghai",
      });
    },
  },
};
</script>
<style scoped>
.flight-card {
  padding: 0;
  margin-bottom: 18px;
  overflow: hidden;
}
.flight-card header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 24px;
  border-bottom: 1px solid var(--border);
}
.flight-body {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 24px;
  align-items: center;
  padding: 28px 24px;
}
.flight-time {
  font-size: 32px;
  font-weight: 700;
  color: var(--ink);
}
.flight-meta {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.flight-action {
  text-align: right;
}
.flight-action .el-button {
  margin-top: 12px;
}
@media (max-width: 650px) {
  .flight-body {
    grid-template-columns: 1fr 1fr;
    padding: 20px;
    gap: 16px;
  }
  .flight-card header {
    flex-wrap: wrap;
    padding: 14px 20px;
    gap: 8px;
  }
  .flight-action {
    grid-column: 1/-1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    text-align: left;
  }
  .flight-action .el-button {
    margin: 0;
  }
  .route-title {
    font-size: 20px;
  }
}
</style>
