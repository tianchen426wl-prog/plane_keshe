<template>
  <div>
    <div class="page-heading">
      <div>
        <h1>选择你的下一程</h1>
        <p>查询航班时刻、比较舱位票价，选择适合你的出行方案</p>
        <small class="muted"
          >参考航班与票价 ·
          非实时库存；实际时刻、价格以航空公司查询为准。</small
        >
      </div>
      <span class="pill">每日刷新 · 30天可查</span>
    </div>
    <SearchPanel
      :window-start="calendar && calendar.windowStart"
      :window-end="calendar && calendar.windowEnd"
      v-model="searchForm"
      :loading="loading"
      resettable
      @search="handleSearch"
      @reset="resetSearch"
    />
    <div class="toolbar">
      <el-button size="small" :disabled="!canPrevious" @click="moveDate(-1)"
        >前一天</el-button
      ><strong>{{ searchForm.date }}</strong
      ><el-button size="small" :disabled="!canNext" @click="moveDate(1)"
        >后一天</el-button
      ><small v-if="calendar" class="muted"
        >可查 {{ calendar.windowStart }} 至 {{ calendar.windowEnd }}</small
      >
    </div>
    <div v-if="calendar" class="date-strip">
      <button
        v-for="d in calendar.dates"
        :key="d.serviceDate"
        :class="{ active: searchForm.date === String(d.serviceDate) }"
        @click="chooseDate(d.serviceDate)"
      >
        <strong>{{ String(d.serviceDate).slice(5) }}</strong
        ><small>{{
          d.minTotal != null
            ? '¥' + d.minTotal + '起 · ' + searchForm.quantity + '人'
            : d.flightCount
            ? '暂无可售'
            : '暂无数据'
        }}</small>
      </button>
    </div>
    <div class="toolbar">
      <el-select
        v-model="companyId"
        clearable
        placeholder="全部航空公司"
        @change="handleSearch"
        ><el-option
          v-for="c in companies"
          :key="c.companyId"
          :label="c.companyName"
          :value="c.companyId" /></el-select
      ><el-select v-model="sort" @change="handleSearch"
        ><el-option label="按起飞时间" value="time" /><el-option
          label="按含必付费用价格"
          value="price" /><el-option
          label="按抵达时间"
          value="arrival" /><el-option
          label="按飞行时长"
          value="duration" /></el-select
      ><span class="muted"
        >{{ total }} 个航班 · {{ searchForm.quantity }}位成人 · 北京时间</span
      >
    </div>
    <section class="surface filters" aria-label="航班筛选">
      <label
        >舱位<el-select
          v-model="filters.cabinClass"
          clearable
          placeholder="不限舱位"
          @change="handleSearch"
          ><el-option label="经济舱" :value="1" /><el-option
            label="商务舱"
            :value="2" /></el-select
      ></label>
      <label
        >起飞时段<el-select v-model="timeBand" @change="handleSearch"
          ><el-option label="全天" value="" /><el-option
            label="凌晨 00:00–06:00"
            value="0-6" /><el-option
            label="上午 06:00–12:00"
            value="6-12" /><el-option
            label="下午 12:00–18:00"
            value="12-18" /><el-option
            label="晚间 18:00–24:00"
            value="18-24" /></el-select
      ></label>
      <label
        >整单预算（元）<el-input-number
          v-model="filters.maxTotal"
          :min="0"
          :max="1000000"
          :step="100"
          controls-position="right"
          @change="handleSearch"
        /><small class="muted">0 表示不限，不含自选座位费用</small></label
      >
      <label
        >每人托运行李<el-select
          v-model="filters.minBaggage"
          @change="handleSearch"
          ><el-option label="不限" :value="0" /><el-option
            label="至少20kg"
            :value="20" /><el-option label="至少30kg" :value="30" /></el-select
      ></label>
      <el-checkbox v-model="filters.onlyAvailable" @change="handleSearch"
        >仅看可预订</el-checkbox
      ><el-button type="text" @click="clearFilters">清空筛选</el-button>
    </section>
    <div v-if="error" class="ui-error" role="alert">
      {{ error }} <el-button type="text" @click="fetchPage">重新查询</el-button>
    </div>
    <el-skeleton v-if="loading" :rows="6" animated />
    <div v-else>
      <p v-if="!error && total > 0" role="status">
        存在符合条件的航班，共 {{ total }} 班。
      </p>
      <FlightCard
        :quantity="searchForm.quantity"
        v-for="f in list"
        :key="f.hbid"
        :flight="f"
        :logged-in="!!(user && user.token)"
        @select="openBuyDialog"
      /><el-empty
        v-if="!error && total === 0"
        description="没有符合条件的航班"
      />
    </div>
    <el-pagination
      v-if="total > 20"
      :total="total"
      :page-size="20"
      :current-page.sync="page"
      layout="total, prev, pager, next"
      @current-change="fetchPage"
    />
    <el-dialog
      title="行程、费用与座位"
      :visible.sync="buyDialogVisible"
      width="min(840px, 96vw)"
      top="3vh"
      custom-class="flight-booking-dialog"
      :close-on-click-modal="false"
      :close-on-press-escape="!holding"
      :show-close="!holding"
      @closed="resetBuyForm"
    >
      <el-skeleton v-if="cabinsLoading" :rows="5" animated />
      <div v-else-if="selectedFlight">
        <el-steps :active="bookingStep" simple
          ><el-step title="选择舱位" /><el-step title="旅客与座位" /><el-step
            title="核对费用"
        /></el-steps>
        <h2>{{ selectedFlight.start }} → {{ selectedFlight.end }}</h2>
        <p>{{ selectedFlight.companyName }} · {{ selectedFlight.flightNo }}</p>
        <p>
          {{ formatDateTime(selectedFlight.starttime) }} →
          {{ formatDateTime(selectedFlight.arrivalTime) }}
        </p>
        <p class="muted">
          {{ selectedFlight.departureAirportName }}
          {{ selectedFlight.departureTerminal }} →
          {{ selectedFlight.arrivalAirportName }}
          {{ selectedFlight.arrivalTerminal }} ·
          {{ selectedFlight.aircraftType }}
        </p>
        <el-alert
          title="请核对航站楼、机型、座位和费用明细后再确认预订。"
          type="info"
          :closable="false"
        />
        <p class="muted">{{ selectedFlight.fareDescription }}</p>
        <div v-show="bookingStep === 0" class="cabin-grid">
          <button
            v-for="c in cabins"
            :key="c.cabinId"
            :data-testid="'cabin-' + c.cabinClass"
            class="cabin-choice"
            :class="{ selected: selectedCabinId === c.cabinId }"
            :disabled="
              c.availableNumber < searchForm.quantity ||
              c.status !== 1 ||
              holding ||
              (filters.cabinClass && c.cabinClass !== filters.cabinClass)
            "
            @click="selectCabin(c)"
          >
            <strong>{{ c.cabinName }}</strong
            ><span class="money">¥{{ c.price }} <small>基础运价</small></span
            ><small>余票 {{ c.availableNumber }} 张</small>
          </button>
        </div>
        <section v-if="selectedCabin" v-show="bookingStep > 0">
          <div v-show="bookingStep === 1">
            <label
              >成人旅客人数
              <el-input-number
                v-model="buyQuantity"
                :min="1"
                :max="Math.min(5, selectedCabin.availableNumber)"
                :disabled="holding"
                @change="quantityChanged"
            /></label>
            <div class="profile-picker">
              <p>
                从常用乘机人填入
                <el-button type="text" @click="$router.push('/user/center')"
                  >管理乘机人</el-button
                >
              </p>
              <p v-if="profilesError" role="alert">
                {{ profilesError }}
                <el-button type="text" @click="loadProfiles">重试</el-button>
              </p>
              <el-select
                v-model="selectedProfiles"
                multiple
                :multiple-limit="buyQuantity"
                placeholder="选择同行人，或在下方手动填写"
                :disabled="holding || profilesLoading"
                @change="applyProfiles"
                ><el-option
                  v-for="p in profiles"
                  :key="p.id"
                  :value="p.id"
                  :label="p.name" /></el-select
              ><small class="muted"
                >已选档案按顺序填入对应旅客，可继续手动编辑。</small
              >
            </div>
            <div class="passengers">
              <label v-for="(_, i) in passengers" :key="i"
                >旅客 {{ i + 1 }} 姓名<el-input
                  :value="passengers[i]"
                  :disabled="holding"
                  :maxlength="50"
                  :placeholder="'请输入旅客' + (i + 1) + '姓名'"
                  @input="updatePassenger(i, $event)"
              /></label>
            </div>
            <el-skeleton v-if="seatsLoading" :rows="3" animated /><SeatMap
              v-else
              v-model="selectedSeats"
              :seats="seats"
              :quantity="buyQuantity"
              :disabled="holding"
              @input="invalidateQuote"
            />
            <el-button
              data-testid="calculate-quote"
              :loading="quoting"
              :disabled="holding || seatsLoading"
              @click="calculateQuote"
              >计算费用并核对座位</el-button
            >
          </div>
          <div
            v-if="quote && bookingStep === 2"
            class="quote-panel"
            data-testid="quote-panel"
          >
            <p>旅客：{{ passengers.join('、') }}</p>
            <p>
              座位：{{ quote.seats.join('、') }} · 托运行李
              {{ quote.policy.baggageKg }}kg/人
            </p>
            <p v-for="item in quote.items" :key="item.code">
              {{ item.name }} <strong>¥{{ item.amount }}</strong>
            </p>
            <p>
              应付总额 <strong class="money">¥{{ quote.total }}</strong> ·
              {{ buyQuantity }} 人
            </p>
            <small class="muted"
              >报价有效至
              {{ formatDateTime(quote.expiresAt) }}，锁定前重新校验库存。</small
            >
          </div>
        </section>
      </div>
      <div slot="footer">
        <el-button
          v-if="bookingStep > 0"
          :disabled="holding"
          @click="bookingStep--"
          >上一步</el-button
        ><el-button :disabled="holding" @click="buyDialogVisible = false"
          >暂不预订</el-button
        ><el-button
          v-if="bookingStep === 0"
          type="primary"
          :disabled="!selectedCabin || seatsLoading"
          @click="bookingStep = 1"
          >填写旅客与选择座位</el-button
        ><el-button
          v-if="bookingStep === 2"
          type="primary"
          data-testid="reserve-cabin"
          :loading="holding"
          :disabled="!quote || quoting"
          @click="handleHold"
          >锁定座位，核对订单</el-button
        >
      </div>
    </el-dialog>
  </div>
</template>
<script>
import { mapState } from 'vuex'
import SearchPanel from '../components/SearchPanel.vue'
import FlightCard from '../components/FlightCard.vue'
import SeatMap from '../components/SeatMap.vue'
import {
  searchFlights,
  getFlightDetail,
  getSeats,
  getQuote,
  getCalendar,
} from '../api/hbxx'
import { getCompanyList } from '../api/company'
import { holdSeat } from '../api/jpdd'
import { getPassengers } from '../api/passenger'
const localDate = () =>
  new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date())
const defaults = () => ({
  cabinClass: null,
  maxTotal: 0,
  minBaggage: 0,
  onlyAvailable: false,
})
const quantity = (v) => Math.max(1, Math.min(5, Math.trunc(Number(v) || 1)))
export default {
  components: { SearchPanel, FlightCard, SeatMap },
  data() {
    return {
      searchForm: {
        departure: '',
        arrival: '',
        date: localDate(),
        quantity: 1,
      },
      filters: defaults(),
      timeBand: '',
      list: [],
      total: 0,
      page: 1,
      companyId: null,
      companies: [],
      sort: 'time',
      calendar: null,
      loading: false,
      error: '',
      buyDialogVisible: false,
      selectedFlight: null,
      cabins: [],
      cabinsLoading: false,
      selectedCabin: null,
      selectedCabinId: null,
      buyQuantity: 1,
      passengers: [''],
      seats: [],
      selectedSeats: [],
      seatsLoading: false,
      quote: null,
      quoting: false,
      holding: false,
      holdRequestId: null,
      requestSequence: 0,
      quoteSequence: 0,
      calendarSequence: 0,
      bookingSequence: 0,
      poll: null,
      bookingStep: 0,
      profiles: [],
      profilesError: '',
      profilesLoading: false,
      selectedProfiles: [],
      pendingFlight: null,
      navigationPath: null,
    }
  },
  computed: {
    ...mapState(['user']),
    canPrevious() {
      return this.calendar && this.searchForm.date > this.calendar.windowStart
    },
    canNext() {
      return this.calendar && this.searchForm.date < this.calendar.windowEnd
    },
  },
  created() {
    this.readRoute()
    getCompanyList({ page: 1, pageSize: 100 })
      .then((r) => {
        this.companies = r.data.records || []
      })
      .catch(() => {})
    this.handleSearch()
    this.openRecommended()
    this.poll = setInterval(() => {
      if (!document.hidden && !this.buyDialogVisible && !this.loading)
        this.fetchPage()
    }, 60000)
  },
  beforeDestroy() {
    clearInterval(this.poll)
    this.requestSequence++
    this.quoteSequence++
    this.calendarSequence++
    this.bookingSequence++
  },
  watch: {
    '$route.fullPath'(path) {
      if (this.$route.path === '/flight' && path !== this.navigationPath) {
        this.navigationPath = path
        this.readRoute()
        this.page = 1
        this.fetchPage()
        this.fetchCalendar()
        this.openRecommended()
      }
    },
  },
  methods: {
    readRoute() {
      const q = this.$route.query
      this.searchForm = {
        departure: q.departure || '',
        arrival: q.arrival || '',
        date: q.date || localDate(),
        quantity: quantity(q.quantity),
      }
      this.pendingFlight = /^[1-9]\d*$/.test(q.hbid || '')
        ? Number(q.hbid)
        : null
      this.companyId = Number(q.companyId) || null
      this.sort = ['price', 'time', 'arrival', 'duration'].includes(q.sort)
        ? q.sort
        : 'time'
      this.timeBand = ['0-6', '6-12', '12-18', '18-24'].includes(q.timeBand)
        ? q.timeBand
        : ''
      this.filters = {
        cabinClass: [1, 2].includes(Number(q.cabinClass))
          ? Number(q.cabinClass)
          : null,
        maxTotal: Math.max(0, Math.min(1000000, Number(q.maxTotal) || 0)),
        minBaggage: [20, 30].includes(Number(q.minBaggage))
          ? Number(q.minBaggage)
          : 0,
        onlyAvailable: q.onlyAvailable === 'true',
      }
    },
    params() {
      const band = this.timeBand ? this.timeBand.split('-').map(Number) : []
      return {
        start: this.searchForm.departure,
        end: this.searchForm.arrival,
        date: this.searchForm.date,
        quantity: this.searchForm.quantity,
        companyId: this.companyId,
        sort: this.sort,
        ...this.filters,
        ...(band.length
          ? { departureFrom: band[0], departureTo: band[1] }
          : {}),
      }
    },
    formatDateTime(v) {
      return new Date(v).toLocaleString('zh-CN', {
        timeZone: 'Asia/Shanghai',
        hour12: false,
      })
    },
    handleSearch() {
      if (!this.searchForm.date) this.searchForm.date = localDate()
      this.searchForm.quantity = quantity(this.searchForm.quantity)
      this.page = 1
      const location = {
        path: '/flight',
        query: {
          ...this.searchForm,
          ...this.filters,
          companyId: this.companyId || undefined,
          sort: this.sort,
          timeBand: this.timeBand,
          ...(this.pendingFlight ? { hbid: String(this.pendingFlight) } : {}),
        },
      }
      this.replaceLocation(location)
      this.fetchPage()
      this.fetchCalendar()
    },
    replaceLocation(location) {
      this.navigationPath = this.$router.resolve(location).route.fullPath
      return this.$router.replace(location).catch(() => {})
    },
    async fetchCalendar() {
      const seq = ++this.calendarSequence
      try {
        const r = await getCalendar(this.params())
        if (seq === this.calendarSequence) this.calendar = r.data
      } catch (e) {
        if (seq === this.calendarSequence) this.calendar = null
      }
    },
    async fetchPage() {
      const seq = ++this.requestSequence
      this.loading = true
      this.error = ''
      try {
        const r = await searchFlights({
          ...this.params(),
          page: this.page,
          pageSize: 20,
        })
        if (seq !== this.requestSequence) return
        this.list = r.data.records || []
        this.total = r.data.total || 0
      } catch (e) {
        if (seq !== this.requestSequence) return
        this.list = []
        this.total = 0
        this.error =
          (e.response && e.response.data && e.response.data.message) ||
          '航班查询暂时不可用，请重试。'
      } finally {
        if (seq === this.requestSequence) this.loading = false
      }
    },
    clearFilters() {
      this.filters = defaults()
      this.companyId = null
      this.timeBand = ''
      this.sort = 'time'
      this.handleSearch()
    },
    resetSearch() {
      this.searchForm = {
        departure: '',
        arrival: '',
        date: localDate(),
        quantity: 1,
      }
      this.pendingFlight = null
      this.clearFilters()
    },
    chooseDate(date) {
      this.searchForm = { ...this.searchForm, date: String(date) }
      this.handleSearch()
    },
    moveDate(n) {
      const date = new Date(this.searchForm.date + 'T12:00:00+08:00')
      date.setUTCDate(date.getUTCDate() + n)
      this.chooseDate(date.toISOString().slice(0, 10))
    },
    async openRecommended() {
      if (!this.pendingFlight) return
      const id = this.pendingFlight
      await this.openBuyDialog({ hbid: id })
      if (this.user && this.user.token) {
        this.pendingFlight = null
        const query = { ...this.$route.query }
        delete query.hbid
        this.replaceLocation({ path: '/flight', query })
      }
    },
    async openBuyDialog(row) {
      if (!this.user || !this.user.token) {
        this.$router.push({
          path: '/login',
          query: {
            redirect: this.$router.resolve({
              path: '/flight',
              query: {
                ...this.$route.query,
                hbid: String(row.hbid),
                quantity: String(this.searchForm.quantity),
              },
            }).route.fullPath,
          },
        })
        return
      }
      this.resetBuyForm()
      const seq = ++this.bookingSequence
      this.buyDialogVisible = true
      this.cabinsLoading = true
      this.loadProfiles()
      try {
        const r = await getFlightDetail(row.hbid)
        if (seq !== this.bookingSequence) return
        const f = r.data
        const now = Date.now()
        if (
          f.saleStatus !== 1 ||
          f.operationalStatus !== 'SCHEDULED' ||
          new Date(f.saleCloseAt).getTime() <= now ||
          new Date(f.saleOpenAt).getTime() > now
        ) {
          this.$message.warning('该航班暂不可预订，请选择其他航班')
          this.buyDialogVisible = false
          return
        }
        this.selectedFlight = f
        this.cabins = f.cabins || []
        if (
          !this.cabins.some(
            (c) =>
              c.status === 1 &&
              c.availableNumber >= this.searchForm.quantity &&
              (!this.filters.cabinClass ||
                c.cabinClass === this.filters.cabinClass)
          )
        ) {
          this.$message.warning('当前舱位余票不足以满足同行人数，请重新选择')
          this.buyDialogVisible = false
        }
      } catch (e) {
        if (seq === this.bookingSequence) this.buyDialogVisible = false
      } finally {
        if (seq === this.bookingSequence) this.cabinsLoading = false
      }
    },
    async loadProfiles() {
      this.profilesLoading = true
      this.profilesError = ''
      try {
        this.profiles = (await getPassengers()).data || []
      } catch (e) {
        this.profiles = []
        this.profilesError = '常用乘机人暂时无法加载，可直接填写姓名。'
      } finally {
        this.profilesLoading = false
      }
    },
    applyProfiles() {
      const selected = this.selectedProfiles
        .map((id) => this.profiles.find((p) => p.id === id))
        .filter(Boolean)
      this.passengers = Array.from({ length: this.buyQuantity }, (_, i) =>
        selected[i] ? selected[i].name : ''
      )
      this.invalidateQuote()
    },
    async selectCabin(c) {
      this.selectedCabin = c
      this.selectedCabinId = c.cabinId
      this.buyQuantity = this.searchForm.quantity
      this.passengers = Array.from(
        { length: this.buyQuantity },
        (_, i) => this.passengers[i] || ''
      )
      this.selectedSeats = []
      this.invalidateQuote()
      this.seatsLoading = true
      const seq = ++this.bookingSequence,
        id = c.cabinId
      try {
        const r = await getSeats(this.selectedFlight.hbid, id)
        if (seq === this.bookingSequence) this.seats = r.data.seats || []
      } finally {
        if (seq === this.bookingSequence) this.seatsLoading = false
      }
    },
    quantityChanged() {
      this.passengers = Array.from(
        { length: this.buyQuantity },
        (_, i) => this.passengers[i] || ''
      )
      this.selectedProfiles = this.selectedProfiles.slice(0, this.buyQuantity)
      this.selectedSeats = []
      this.invalidateQuote()
    },
    updatePassenger(i, v) {
      this.$set(this.passengers, i, v)
      this.invalidateQuote()
    },
    invalidateQuote() {
      this.quote = null
      this.holdRequestId = null
      this.quoteSequence++
    },
    async calculateQuote() {
      if (this.quoting) return
      if (this.passengers.some((p) => !p.trim())) {
        this.$message.warning('请填写每位乘机人姓名')
        return
      }
      if (
        this.selectedSeats.length &&
        this.selectedSeats.length !== this.buyQuantity
      ) {
        this.$message.warning('请选满座位，或使用免费自动分配')
        return
      }
      const seq = ++this.quoteSequence
      this.quoting = true
      try {
        const r = await getQuote({
          hbid: this.selectedFlight.hbid,
          cabinId: this.selectedCabinId,
          quantity: this.buyQuantity,
          passengers: this.passengers,
          seats: this.selectedSeats,
        })
        if (seq === this.quoteSequence) {
          this.quote = r.data
          this.bookingStep = 2
        }
      } catch (e) {
        if (seq === this.quoteSequence) this.quote = null
      } finally {
        this.quoting = false
      }
    },
    async handleHold() {
      if (!this.quote || this.holding) return
      this.holding = true
      try {
        if (!this.holdRequestId)
          this.holdRequestId =
            Date.now().toString(36) + Math.random().toString(36).slice(2)
        const r = await holdSeat({
          requestId: this.holdRequestId,
          hbid: this.selectedFlight.hbid,
          cabinId: this.selectedCabinId,
          quantity: this.buyQuantity,
          quoteId: this.quote.quoteId,
        })
        this.buyDialogVisible = false
        this.$router.push('/booking/' + r.data.holdId)
      } catch (e) {
        this.invalidateQuote()
        this.bookingStep = 1
        if (this.selectedCabin) {
          try {
            const r = await getSeats(
              this.selectedFlight.hbid,
              this.selectedCabinId
            )
            this.seats = r.data.seats || []
          } catch (refreshError) {}
        }
      } finally {
        this.holding = false
      }
    },
    resetBuyForm() {
      this.bookingSequence++
      this.bookingStep = 0
      this.selectedProfiles = []
      this.selectedFlight = null
      this.cabins = []
      this.selectedCabin = null
      this.selectedCabinId = null
      this.seats = []
      this.selectedSeats = []
      this.passengers = ['']
      this.buyQuantity = this.searchForm.quantity
      this.seatsLoading = false
      this.invalidateQuote()
    },
  },
}
</script>
<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin: 20px 0;
}
.date-strip {
  display: flex;
  gap: 6px;
  overflow: auto;
  padding: 4px 0 16px;
}
.date-strip button {
  flex: 0 0 94px;
  padding: 12px 8px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: white;
  color: var(--text);
  cursor: pointer;
}
.date-strip button > * {
  display: block;
}
.date-strip small {
  margin-top: 6px;
  color: var(--muted);
}
.date-strip .active {
  border-color: var(--brand);
  background: #eef5ff;
}
.cabin-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin: 22px 0;
}
.cabin-choice {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: start;
  background: white;
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 18px;
  text-align: left;
  cursor: pointer;
}
.cabin-choice.selected {
  border-color: var(--brand);
  background: #f3f7ff;
}
.cabin-choice:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.cabin-choice .money {
  font-size: 22px;
}
.cabin-choice .money small {
  font-size: 12px;
}
.passengers {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-top: 18px;
}
.passengers label {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.quote-panel {
  padding: 20px;
  background: #f4f8fd;
  border-radius: 12px;
  margin-top: 16px;
}
.quote-panel p {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}
.toolbar .el-select {
  max-width: 100%;
}
@media (max-width: 500px) {
  .toolbar small {
    flex-basis: 100%;
  }
  .quote-panel {
    padding: 14px;
  }
  .toolbar .el-select {
    width: 100%;
  }
}
.filters {
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 20px;
}
.filters > label:not(.el-checkbox) {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.filters label small {
  font-size: 11px;
}
.profile-picker {
  margin: 18px 0;
}
.profile-picker .el-select {
  width: 100%;
}
.profile-picker small {
  display: block;
  margin-top: 8px;
}
@media (max-width: 600px) {
  .filters > label:not(.el-checkbox) {
    width: 100%;
  }
  .filters .el-select,
  .filters .el-input-number {
    width: 100%;
  }
}
</style>
<style>
@media (max-width: 600px) {
  .flight-booking-dialog {
    width: 100% !important;
    max-width: 100vw !important;
    margin: 0 !important;
    min-height: 100vh;
    border-radius: 0 !important;
    display: flex;
    flex-direction: column;
  }
  .flight-booking-dialog .el-dialog__body {
    flex: 1;
    padding: 16px;
    padding-bottom: 110px;
  }
  .flight-booking-dialog .el-dialog__footer {
    position: sticky;
    bottom: 0;
    background: white;
    box-shadow: 0 -4px 16px #17345b12;
    padding: 12px;
    z-index: 2;
  }
  .flight-booking-dialog .el-dialog__footer > div {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    justify-content: flex-end;
  }
  .flight-booking-dialog .el-dialog__footer .el-button {
    margin: 0;
  }
  .flight-booking-dialog .el-step__title {
    font-size: 12px;
    max-width: none !important;
    white-space: nowrap;
  }
  .flight-booking-dialog .el-step.is-simple {
    flex-direction: column;
    gap: 6px;
  }
  .flight-booking-dialog .el-step.is-simple .el-step__main {
    display: block;
  }
  .flight-booking-dialog .el-step.is-simple .el-step__head {
    padding-right: 0;
  }
  .flight-booking-dialog .el-step.is-simple .el-step__arrow {
    display: none;
  }
}
</style>
