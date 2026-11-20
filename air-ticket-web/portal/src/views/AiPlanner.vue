<template>
  <div class="planner-page">
    <section class="planner-hero">
      <div class="hero-copy">
        <span class="eyebrow"
          ><i aria-hidden="true" class="el-icon-magic-stick" /> 云际 ·
          智能行程规划</span
        >
        <h1>下一程，<br class="mobile-break" />从一个想法开始。</h1>
        <p>说出你的出行计划，让合适的航班来找你。</p>
        <div class="hero-tags">
          <span>国内直飞</span><span>单程出行</span><span>1–5 位成人</span>
        </div>
      </div>
      <div class="hero-visual" aria-hidden="true">
        <div class="visual-orbit" />
        <div class="visual-route">
          <span>出行灵感<small>YOUR IDEA</small></span
          ><i aria-hidden="true" class="el-icon-position" /><span
            >专属行程<small>YOUR JOURNEY</small></span
          >
        </div>
        <span class="visual-caption">让每一个出发的念头，都有方向。</span>
      </div>
    </section>

    <div class="planner-workspace">
      <section class="surface composer" aria-labelledby="composer-title">
        <div class="composer-heading">
          <div>
            <span class="section-label">01 / 描述需求</span>
            <h2 id="composer-title">这一次，你想怎么出发？</h2>
          </div>
          <span
            class="service-status"
            :class="{
              online: enabled && !checking,
              unavailable: !enabled && !checking,
            }"
            role="status"
            ><i aria-hidden="true" />{{
              checking
                ? '正在连接服务'
                : enabled
                ? '智能规划已就绪'
                : '服务暂不可用'
            }}</span
          >
        </div>
        <div class="input-tabs" role="tablist" aria-label="需求输入方式">
          <button
            id="tab-description"
            type="button"
            role="tab"
            aria-controls="panel-description"
            :aria-selected="mode === 'description'"
            :class="{ active: mode === 'description' }"
            :disabled="loading"
            @click="mode = 'description'"
          >
            <i aria-hidden="true" class="el-icon-chat-line-square" />
            自然语言描述</button
          ><button
            id="tab-fields"
            type="button"
            role="tab"
            aria-controls="panel-fields"
            :aria-selected="mode === 'fields'"
            :class="{ active: mode === 'fields' }"
            :disabled="loading"
            @click="mode = 'fields'"
          >
            <i aria-hidden="true" class="el-icon-edit-outline" /> 快捷填写
          </button>
        </div>
        <div
          v-show="mode === 'description'"
          id="panel-description"
          role="tabpanel"
          aria-labelledby="tab-description"
        >
          <label class="field-label" for="trip-description"
            >告诉我你的出行想法</label
          >
          <p class="field-hint">
            带上出发地、目的地、日期、人数和机票总预算，推荐会更准确。
          </p>
          <el-input
            id="trip-description"
            ref="description"
            v-model="message"
            type="textarea"
            :rows="6"
            :maxlength="1000"
            show-word-limit
            :disabled="loading"
            placeholder="例如：明天从北京飞上海，1位成人，机票总预算1500元。"
            @keydown.ctrl.enter.native.prevent="plan"
          />
          <div class="examples-heading">
            <span>还没想好怎么说？试试这些</span
            ><small>点击填入，可继续修改</small>
          </div>
          <div class="example-grid">
            <button
              v-for="example in examples"
              :key="example.title"
              type="button"
              :disabled="loading"
              @click="useExample(example)"
            >
              <i aria-hidden="true" :class="example.icon" /><strong>{{
                example.title
              }}</strong
              ><span>{{ example.label }}</span
              ><i aria-hidden="true" class="el-icon-top-right" />
            </button>
          </div>
        </div>
        <div
          v-show="mode === 'fields'"
          id="panel-fields"
          role="tabpanel"
          aria-labelledby="tab-fields"
        >
          <p class="field-hint">按项填写，我们会将你的需求整理为行程描述。</p>
          <el-form
            ref="tripForm"
            :model="trip"
            :rules="rules"
            label-position="top"
            :disabled="loading"
            class="trip-form"
          >
            <div class="form-grid">
              <el-form-item label="出发城市" prop="departure"
                ><el-input
                  v-model.trim="trip.departure"
                  placeholder="如：北京"
                  maxlength="30"
                  prefix-icon="el-icon-position" /></el-form-item
              ><el-form-item label="目的城市" prop="arrival"
                ><el-input
                  v-model.trim="trip.arrival"
                  placeholder="如：上海"
                  maxlength="30"
                  prefix-icon="el-icon-location-outline" /></el-form-item
              ><el-form-item label="出发日期" prop="date"
                ><el-date-picker
                  v-model="trip.date"
                  type="date"
                  value-format="yyyy-MM-dd"
                  placeholder="选择出发日期"
                  :picker-options="pickerOptions" /></el-form-item
              ><el-form-item label="成人旅客" prop="quantity"
                ><el-input-number
                  v-model="trip.quantity"
                  :min="1"
                  :max="5"
                  controls-position="right"
                  aria-label="成人旅客人数" /></el-form-item
              ><el-form-item
                class="budget-field"
                label="机票总预算（所有旅客合计）"
                ><el-input-number
                  v-model="trip.budget"
                  :min="0"
                  :max="1000000"
                  :precision="0"
                  :step="100"
                  controls-position="right"
                  aria-label="机票总预算，零表示不限"
                /><span class="budget-hint"
                  >元，填 0 表示不限预算</span
                ></el-form-item
              >
            </div>
          </el-form>
        </div>
        <div v-if="!checking && !enabled" class="service-notice" role="status">
          <i aria-hidden="true" class="el-icon-info" />
          <div>
            {{ statusError || '智能规划暂未启用，你仍可直接查询航班。'
            }}<button type="button" :disabled="checking" @click="checkService">
              重新连接
            </button>
          </div>
        </div>
        <div class="composer-footer">
          <div class="planner-actions">
            <el-button
              type="primary"
              :disabled="
                checking ||
                !enabled ||
                (mode === 'description' && !message.trim())
              "
              :loading="loading"
              @click="plan"
              ><i
                aria-hidden="true"
                v-if="!loading"
                class="el-icon-magic-stick"
              />
              {{ loading ? '正在规划行程' : '生成我的行程' }}</el-button
            ><el-button :disabled="loading" @click="$router.push('/flight')"
              >直接查询航班 <i aria-hidden="true" class="el-icon-right"
            /></el-button>
          </div>
          <small
            ><i aria-hidden="true" class="el-icon-lock" />
            仅需出行偏好，无需填写证件或联系方式</small
          >
        </div>
      </section>
      <aside class="planner-sidebar">
        <section class="guide-card">
          <span class="section-label">HOW IT WORKS</span>
          <h2>从想法到出发，只需三步</h2>
          <ol>
            <li>
              <span>01</span>
              <div>
                <h3>说出你的计划</h3>
                <p>填写路线、时间与预算，<br />也可以直接用一句话描述。</p>
              </div>
            </li>
            <li>
              <span>02</span>
              <div>
                <h3>发现合适的航班</h3>
                <p>匹配当前可售航班，<br />结合你的需求呈现候选方案。</p>
              </div>
            </li>
            <li>
              <span>03</span>
              <div>
                <h3>确认后再预订</h3>
                <p>进入航班页核对舱位、<br />座位和费用，再完成预订。</p>
              </div>
            </li>
          </ol>
        </section>
        <section class="scope-card">
          <i aria-hidden="true" class="el-icon-discover" />
          <h3>关于这次规划</h3>
          <p>
            支持国内成人直飞单程航班，每次 1–5
            位旅客。预算指所有旅客的机票总预算。
          </p>
          <div class="scope-note">
            推荐价格为预估总价，最终价格与座位以预订时为准。
          </div>
        </section>
      </aside>
    </div>

    <section
      ref="results"
      class="planner-results"
      aria-labelledby="results-title"
      :aria-busy="loading"
      tabindex="-1"
    >
      <div class="results-heading">
        <div>
          <span class="section-label">02 / 发现行程</span>
          <h2 id="results-title">
            {{
              loading
                ? '正在为你寻找合适的航班'
                : result && result.needsClarification
                ? '再补充一点，行程更明确'
                : '你的候选行程'
            }}
          </h2>
        </div>
        <span v-if="result && candidates.length" class="result-count"
          >{{ candidates.length }} 个方案</span
        >
      </div>
      <div v-if="loading" class="surface loading-state" role="status">
        <div class="loading-heading">
          <i aria-hidden="true" class="el-icon-loading" />
          <div>
            <h3>正在解析需求并匹配可售航班…</h3>
            <p>通常需要片刻，请稍候。</p>
          </div>
        </div>
        <el-skeleton :rows="3" animated />
      </div>
      <div
        v-else-if="error"
        class="surface result-state error-state"
        role="alert"
      >
        <i aria-hidden="true" class="el-icon-warning-outline" />
        <div>
          <h3>这次规划未能完成</h3>
          <p>{{ error }}</p>
          <el-button type="primary" @click="plan" :disabled="!enabled"
            >重试规划</el-button
          ><el-button @click="$router.push('/flight')">查询航班</el-button>
        </div>
      </div>
      <template v-else-if="result">
        <div
          v-if="result.needsClarification"
          class="surface result-state clarification-state"
        >
          <i aria-hidden="true" class="el-icon-chat-dot-round" />
          <div>
            <h3>还需要一些出行信息</h3>
            <p>
              {{
                result.message ||
                '请补充出发城市、目的城市、日期、旅客人数和机票总预算。'
              }}
            </p>
            <el-button type="primary" @click="editRequest"
              >补充我的需求</el-button
            >
          </div>
        </div>
        <template v-else>
          <div
            v-if="constraintTags.length"
            class="constraint-tags"
            aria-label="本次规划条件"
          >
            <span v-for="tag in constraintTags" :key="tag"
              ><i aria-hidden="true" class="el-icon-check" /> {{ tag }}</span
            ><button type="button" @click="editRequest">
              修改需求 <i aria-hidden="true" class="el-icon-edit" />
            </button>
          </div>
          <p v-if="candidates.length" class="result-description">
            已根据当前可售航班筛选，以下为预估总价。请选择方案，前往航班页核对舱位与费用。
          </p>
          <article
            v-for="(candidate, index) in candidates"
            :key="candidate.hbid"
            class="surface itinerary-card"
          >
            <div class="itinerary-top">
              <span class="candidate-number"
                >方案 {{ String(index + 1).padStart(2, '0') }}</span
              ><strong>{{ candidate.companyName }}</strong
              ><span class="flight-number">{{ candidate.flightNo }}</span
              ><span class="direct-tag">直飞 · 单程</span>
            </div>
            <div class="itinerary-main">
              <div class="route-timeline">
                <div class="route-stop">
                  <strong>{{ time(candidate.starttime) }}</strong
                  ><span>{{ candidate.start }}</span
                  ><small>{{ date(candidate.starttime) }}</small>
                </div>
                <div class="route-connector">
                  <small>{{ duration(candidate) }}</small>
                  <div>
                    <span /><i
                      aria-hidden="true"
                      class="el-icon-position"
                    /><span />
                  </div>
                  <small>北京时间</small>
                </div>
                <div class="route-stop">
                  <strong>{{ time(candidate.arrivalTime) }}</strong
                  ><span>{{ candidate.end }}</span
                  ><small>{{ date(candidate.arrivalTime) }}</small>
                </div>
              </div>
              <div class="itinerary-price">
                <small>{{ candidate.quantity || 1 }} 位成人 · 预估总价</small>
                <div class="money">
                  <span>¥</span>{{ money(candidate.estimatedTotal) }}
                </div>
                <el-button type="primary" @click="choose(candidate)"
                  >查看航班与舱位 <i aria-hidden="true" class="el-icon-right"
                /></el-button>
              </div>
            </div>
            <div class="itinerary-bottom">
              <span v-if="candidate.aircraftType"
                ><i aria-hidden="true" class="el-icon-position" />
                {{ candidate.aircraftType }}</span
              ><span v-if="candidate.baggageKg != null"
                ><i aria-hidden="true" class="el-icon-suitcase" /> 托运行李
                {{ candidate.baggageKg }}kg / 人</span
              ><span
                ><i aria-hidden="true" class="el-icon-info" />
                预订前将重新核对价格，自选付费座位另计</span
              >
            </div>
          </article>
          <div
            v-if="!candidates.length"
            class="surface result-state empty-state"
          >
            <i aria-hidden="true" class="el-icon-search" />
            <div>
              <h3>暂未找到符合条件的航班</h3>
              <p>试着调整出发日期、路线或机票总预算，再寻找其他出行可能。</p>
              <el-button type="primary" @click="editRequest"
                >调整出行需求</el-button
              ><el-button @click="$router.push('/flight')"
                >查看全部航班</el-button
              >
            </div>
          </div>
        </template>
      </template>
      <div v-else class="surface initial-state">
        <div class="empty-route" aria-hidden="true">
          <i aria-hidden="true" class="el-icon-location-outline" /><span /><i
            aria-hidden="true"
            class="el-icon-position"
          /><span /><i aria-hidden="true" class="el-icon-location-outline" />
        </div>
        <h3>你的下一程，在这里展开</h3>
        <p>填写上方出行需求，适合你的航班方案将在这里呈现。</p>
        <span>路线 · 日期 · 人数 · 预算</span>
      </div>
    </section>
  </div>
</template>

<script>
import request from '../api/request'

const shanghaiDate = (value = new Date()) => {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(value)
  const get = (type) => parts.find((part) => part.type === type).value
  return `${get('year')}-${get('month')}-${get('day')}`
}
export default {
  name: 'AiPlanner',
  data() {
    return {
      enabled: false,
      checking: true,
      statusError: '',
      message: '',
      mode: 'description',
      loading: false,
      result: null,
      error: '',
      trip: { departure: '', arrival: '', date: '', quantity: 1, budget: 0 },
      examples: [
        {
          title: '商务出行',
          icon: 'el-icon-suitcase',
          label: '北京 → 上海 · 1人',
          message: '明天从北京飞上海，1位成人，机票总预算1500元。',
        },
        {
          title: '周末出发',
          icon: 'el-icon-coffee-cup',
          label: '上海 → 成都 · 1人',
          message: '下个周六从上海飞成都，1位成人，机票总预算1200元。',
        },
        {
          title: '结伴旅行',
          icon: 'el-icon-user',
          label: '广州 → 三亚 · 2人',
          message: '后天从广州飞三亚，2位成人，机票总预算3000元。',
        },
      ],
      rules: {
        quantity: [
          {
            required: true,
            type: 'integer',
            min: 1,
            max: 5,
            message: '请选择1–5位成人旅客',
            trigger: 'change',
          },
        ],
        departure: [
          { required: true, message: '请填写出发城市', trigger: 'blur' },
        ],
        arrival: [
          { required: true, message: '请填写目的城市', trigger: 'blur' },
          {
            validator: (rule, value, done) =>
              done(
                value && value === this.trip.departure
                  ? new Error('目的城市不能与出发城市相同')
                  : undefined
              ),
            trigger: 'blur',
          },
        ],
        date: [
          { required: true, message: '请选择出发日期', trigger: 'change' },
        ],
      },
      pickerOptions: {
        disabledDate(value) {
          return shanghaiDate(value) < shanghaiDate()
        },
      },
    }
  },
  computed: {
    candidates() {
      return (this.result && this.result.candidates) || []
    },
    constraintTags() {
      const c = this.result && this.result.constraints
      if (!c) return []
      return [
        c.start && c.end ? `${c.start} → ${c.end}` : '',
        c.date,
        c.quantity ? `${c.quantity} 位成人` : '',
        Number(c.budget) > 0 ? `总预算 ¥${this.money(c.budget)}` : '预算不限',
      ].filter(Boolean)
    },
  },
  created() {
    this.checkService()
  },
  methods: {
    async checkService() {
      this.checking = true
      this.statusError = ''
      try {
        this.enabled = !!(await request.get('/ai/capabilities')).data.enabled
      } catch (e) {
        this.enabled = false
        this.statusError =
          '暂时无法连接智能规划服务，请稍后重试或直接查询航班。'
      } finally {
        this.checking = false
      }
    },
    time(value) {
      return value && !isNaN(new Date(value))
        ? new Date(value).toLocaleTimeString('zh-CN', {
            timeZone: 'Asia/Shanghai',
            hour: '2-digit',
            minute: '2-digit',
            hour12: false,
          })
        : '--:--'
    },
    date(value) {
      return value && !isNaN(new Date(value))
        ? new Date(value).toLocaleDateString('zh-CN', {
            timeZone: 'Asia/Shanghai',
            month: '2-digit',
            day: '2-digit',
          })
        : '时间待确认'
    },
    money(value) {
      return value != null && Number.isFinite(Number(value))
        ? Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
        : '—'
    },
    duration(candidate) {
      const minutes = Math.round(
        (new Date(candidate.arrivalTime) - new Date(candidate.starttime)) /
          60000
      )
      return Number.isFinite(minutes) && minutes > 0
        ? `${Math.floor(minutes / 60)}小时${
            minutes % 60 ? (minutes % 60) + '分' : ''
          }`
        : '直飞航班'
    },
    useExample(example) {
      this.message = example.message
      this.$refs.description.focus()
    },
    editRequest() {
      this.$el.querySelector('.composer').scrollIntoView({
        behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches
          ? 'auto'
          : 'smooth',
        block: 'start',
      })
      this.$nextTick(() => {
        if (this.mode === 'description') this.$refs.description.focus()
        else this.$el.querySelector('.trip-form input').focus()
      })
    },
    async plan() {
      if (this.loading || this.checking || !this.enabled) return
      let message = this.message.trim()
      if (this.mode === 'fields') {
        const valid = await new Promise((resolve) =>
          this.$refs.tripForm.validate(resolve)
        )
        if (!valid) return
        const t = this.trip
        message = `${t.date}从${t.departure}飞${t.arrival}，${
          t.quantity
        }位成人，${
          t.budget > 0 ? '机票总预算' + t.budget + '元' : '机票预算不限'
        }，国内直飞单程。`
      }
      if (!message || this.loading) return
      this.loading = true
      this.error = ''
      this.result = null
      try {
        this.result = (
          await request.post(
            '/ai/itineraries/plan',
            { message },
            { timeout: 35000 }
          )
        ).data
      } catch (e) {
        this.error =
          e.code === 'ECONNABORTED'
            ? '规划请求超时，请稍后重试。'
            : (e.response && e.response.data && e.response.data.message) ||
              e.message ||
              '智能规划暂时无法完成，请稍后再试。'
      } finally {
        this.loading = false
        this.$nextTick(() => {
          const results = this.$refs.results
          if (!results) return
          results.focus({ preventScroll: true })
          results.scrollIntoView({
            behavior: window.matchMedia('(prefers-reduced-motion: reduce)')
              .matches
              ? 'auto'
              : 'smooth',
            block: 'start',
          })
        })
      }
    },
    choose(candidate) {
      const constraints = (this.result && this.result.constraints) || {}
      const date =
        constraints.date ||
        (candidate.starttime && !isNaN(new Date(candidate.starttime))
          ? shanghaiDate(new Date(candidate.starttime))
          : '')
      this.$router.push({
        path: '/flight',
        query: {
          departure: candidate.start,
          arrival: candidate.end,
          hbid: String(candidate.hbid),
          quantity: String(candidate.quantity || constraints.quantity || 1),
          ...(constraints.budget > 0 ? { maxTotal: String(constraints.budget) } : {}),
          ...(date ? { date } : {}),
        },
      })
    },
  },
}
</script>

<style scoped>
.planner-hero {
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  align-items: center;
  gap: 28px;
  background: linear-gradient(115deg, #102f42, #174c53);
  border-radius: 22px;
  padding: 38px 42px;
  color: #fff;
  margin-bottom: 28px;
}
.eyebrow {
  font-size: 12px;
  letter-spacing: 2px;
  color: #a3ded3;
  display: flex;
  gap: 8px;
  align-items: center;
}
.eyebrow i {
  font-size: 18px;
}
.hero-copy h1 {
  font-size: 34px;
  font-weight: 600;
  letter-spacing: 1px;
  line-height: 1.5;
  margin: 17px 0 10px;
}
.hero-copy p {
  color: #c4d8df;
  margin-bottom: 20px;
}
.hero-tags {
  display: flex;
  gap: 10px;
}
.hero-tags span {
  font-size: 11px;
  border: 1px solid #ffffff28;
  border-radius: 20px;
  padding: 3px 12px;
  color: #cce6e3;
}
.mobile-break {
  display: none;
}
.hero-visual {
  height: 190px;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.visual-orbit {
  position: absolute;
  width: 90%;
  height: 145px;
  border: 1px solid #ffffff1a;
  border-radius: 50%;
  transform: rotate(-16deg);
  left: 4%;
  top: 10px;
}
.visual-route {
  display: flex;
  justify-content: center;
  gap: 32px;
  align-items: center;
  position: relative;
}
.visual-route > span {
  font-size: 17px;
  letter-spacing: 2px;
}
.visual-route small {
  display: block;
  font-size: 9px;
  letter-spacing: 2px;
  color: #91baba;
  margin-top: 8px;
}
.visual-route > i {
  font-size: 45px;
  color: #8ed6c7;
  transform: rotate(12deg);
}
.visual-caption {
  text-align: center;
  font-size: 11px;
  letter-spacing: 2px;
  color: #a4c3c8;
  margin-top: 24px;
}
.planner-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 290px;
  gap: 24px;
  align-items: start;
}
.composer {
  padding: 28px;
  min-width: 0;
  box-shadow: 0 10px 32px #12374604;
}
.composer-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}
.section-label {
  color: var(--brand);
  font-size: 10px;
  letter-spacing: 2px;
  font-weight: 600;
}
.composer-heading h2 {
  font-size: 21px;
  margin: 8px 0 0;
  color: var(--ink);
}
.service-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  background: #f1f4f5;
  color: var(--muted);
  font-size: 10px;
  border-radius: 20px;
  padding: 5px 9px;
}
.service-status > i {
  width: 5px;
  height: 5px;
  background: #9aaeb5;
  border-radius: 50%;
}
.service-status.online {
  background: var(--tint);
  color: var(--brand);
}
.service-status.online > i {
  background: var(--brand);
}
.service-status.unavailable {
  background: #fbf4e7;
  color: #946725;
}
.service-status.unavailable > i {
  background: #b18a46;
}
.input-tabs {
  display: flex;
  border-bottom: 1px solid var(--border);
  gap: 28px;
  margin: 25px 0 22px;
}
.input-tabs button {
  border: 0;
  background: none;
  color: var(--muted);
  padding: 0 0 13px;
  cursor: pointer;
  position: relative;
  font-size: 13px;
}
.input-tabs button.active {
  font-weight: 600;
  color: var(--brand);
}
.input-tabs button.active:after {
  content: '';
  position: absolute;
  bottom: -1px;
  height: 2px;
  left: 0;
  right: 0;
  background: var(--brand);
}
.input-tabs i {
  margin-right: 4px;
}
.field-label {
  color: var(--ink);
  font-size: 14px;
  font-weight: 600;
}
.field-hint {
  color: var(--muted);
  font-size: 12px;
  line-height: 1.8;
  margin: 8px 0 15px;
}
.composer ::v-deep .el-textarea__inner {
  padding: 17px 18px 30px;
  line-height: 1.9;
  background: #f8fafb;
  resize: vertical;
  font-size: 14px;
  min-height: 180px !important;
}
.composer ::v-deep .el-input__count {
  background: transparent;
  color: #7f9098;
  font-size: 11px;
  bottom: 9px;
  right: 15px;
}
.examples-heading {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
  margin: 22px 0 12px;
  font-size: 12px;
  color: var(--text);
}
.examples-heading small {
  font-size: 10px;
  color: var(--muted);
}
.example-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}
.example-grid button {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: start;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: #fff;
  padding: 13px 12px;
  cursor: pointer;
  color: var(--text);
  text-align: left;
  transition: border-color 0.2s, background 0.2s;
}
.example-grid button:hover {
  border-color: #93bcb5;
  background: #f7fbf9;
}
.example-grid button > i:first-child {
  font-size: 18px;
  color: var(--brand);
  margin-bottom: 8px;
}
.example-grid strong {
  font-size: 12px;
  font-weight: 500;
}
.example-grid span {
  font-size: 10px;
  color: var(--muted);
  margin-top: 4px;
}
.example-grid .el-icon-top-right {
  position: absolute;
  right: 12px;
  top: 13px;
  color: #829d9b;
  font-size: 11px;
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 18px;
}
.trip-form ::v-deep .el-form-item__label {
  font-size: 12px;
  line-height: 26px;
  padding-bottom: 4px;
}
.trip-form ::v-deep .el-form-item {
  margin-bottom: 20px;
}
.trip-form ::v-deep .el-date-editor,
.trip-form ::v-deep .el-input-number {
  width: 100%;
}
.trip-form ::v-deep .el-input__inner {
  background: #f8fafb;
  height: 46px;
}
.trip-form ::v-deep .el-input-number__decrease,
.trip-form ::v-deep .el-input-number__increase {
  line-height: 22px;
}
.budget-field {
  grid-column: 1/-1;
}
.budget-field .el-input-number {
  max-width: 210px;
}
.budget-hint {
  margin-left: 12px;
  color: var(--muted);
  font-size: 11px;
}
.service-notice {
  display: flex;
  gap: 9px;
  padding: 14px;
  background: #fbf6ec;
  border-radius: 10px;
  margin-top: 20px;
  color: #85652f;
  font-size: 12px;
}
.service-notice > i {
  margin-top: 4px;
}
.service-notice button {
  border: 0;
  background: none;
  padding: 0;
  margin-left: 8px;
  color: var(--brand);
  cursor: pointer;
  font-size: 12px;
}
.composer-footer {
  border-top: 1px solid var(--border);
  margin-top: 24px;
  padding-top: 21px;
}
.planner-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.planner-actions .el-button {
  margin: 0;
  padding: 13px 19px;
  font-size: 13px;
}
.planner-actions .el-button--primary {
  box-shadow: 0 5px 12px #16746f17;
}
.composer-footer > small {
  font-size: 10px;
  color: var(--muted);
  display: block;
  margin-top: 16px;
}
.planner-sidebar {
  display: grid;
  gap: 18px;
}
.guide-card {
  background: #edf4f2;
  border: 1px solid #dce8e3;
  border-radius: 16px;
  padding: 26px 22px;
}
.guide-card h2 {
  font-size: 16px;
  color: var(--ink);
  margin: 10px 0 22px;
}
.guide-card ol {
  list-style: none;
  padding: 0;
  margin: 0;
}
.guide-card li {
  display: flex;
  gap: 13px;
  padding-bottom: 23px;
  position: relative;
}
.guide-card li:last-child {
  padding-bottom: 0;
}
.guide-card li:not(:last-child):before {
  content: '';
  position: absolute;
  left: 15px;
  top: 35px;
  bottom: 5px;
  border-left: 1px dashed #c4d8d2;
}
.guide-card li > span {
  width: 31px;
  height: 31px;
  border-radius: 50%;
  background: #fff;
  border: 1px solid #d9e6e1;
  color: var(--brand);
  font-size: 10px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}
.guide-card h3 {
  font-size: 13px;
  font-weight: 600;
  margin: 3px 0 7px;
}
.guide-card p {
  font-size: 11px;
  color: var(--muted);
  line-height: 1.8;
  margin: 0;
}
.scope-card {
  padding: 22px;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 16px;
}
.scope-card > i {
  font-size: 22px;
  color: var(--brand);
  float: left;
  margin-right: 9px;
}
.scope-card h3 {
  font-size: 14px;
  color: var(--ink);
  margin-bottom: 14px;
}
.scope-card p {
  font-size: 12px;
  line-height: 1.9;
  color: var(--muted);
}
.scope-note {
  font-size: 11px;
  color: var(--muted);
  border-top: 1px solid var(--border);
  padding-top: 12px;
  line-height: 1.8;
}
.planner-results {
  margin-top: 34px;
  outline: none;
}
.planner-results:focus-visible {
  outline: 2px solid #9acbc1;
  outline-offset: 8px;
  border-radius: 12px;
}
.results-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 19px;
}
.results-heading h2 {
  font-size: 23px;
  color: var(--ink);
  margin: 7px 0 0;
}
.result-count {
  color: var(--brand);
  font-size: 12px;
  background: var(--tint);
  padding: 5px 12px;
  border-radius: 20px;
}
.initial-state {
  text-align: center;
  padding: 36px 24px;
  background: linear-gradient(#fff, #fbfdfc);
  border-style: dashed;
}
.empty-route {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #92b7ae;
  margin-bottom: 15px;
}
.empty-route span {
  width: 50px;
  border-top: 1px dashed #aac8c0;
}
.empty-route i {
  font-size: 23px;
}
.empty-route .el-icon-position {
  font-size: 35px;
  color: var(--brand);
}
.initial-state h3 {
  font-size: 17px;
  font-weight: 500;
  margin: 0 0 9px;
  color: var(--ink);
}
.initial-state p {
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 14px;
}
.initial-state > span {
  font-size: 10px;
  letter-spacing: 2px;
  color: #859c9e;
}
.loading-state {
  padding: 30px;
}
.loading-heading {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 25px;
}
.loading-heading > i {
  font-size: 30px;
  color: var(--brand);
}
.loading-heading h3 {
  font-size: 16px;
  margin: 0 0 4px;
}
.loading-heading p {
  margin: 0;
  color: var(--muted);
  font-size: 12px;
}
.result-state {
  display: flex;
  align-items: start;
  gap: 20px;
  padding: 32px;
}
.result-state > i {
  color: var(--brand);
  font-size: 34px;
  background: var(--tint);
  padding: 14px;
  border-radius: 16px;
}
.result-state h3 {
  font-size: 18px;
  margin: 0 0 8px;
  color: var(--ink);
}
.result-state p {
  font-size: 13px;
  color: var(--muted);
  line-height: 1.9;
  max-width: 700px;
}
.result-state .el-button {
  font-size: 12px;
  margin: 8px 8px 0 0;
}
.error-state > i {
  background: #fff4ea;
  color: #bd793e;
}
.constraint-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}
.constraint-tags > span {
  font-size: 12px;
  background: #e9f2ef;
  color: var(--brand);
  padding: 6px 12px;
  border-radius: 6px;
}
.constraint-tags > span > i {
  margin-right: 4px;
}
.constraint-tags button {
  background: transparent;
  border: 0;
  color: var(--brand);
  font-size: 12px;
  cursor: pointer;
  margin-left: auto;
}
.result-description {
  font-size: 12px;
  color: var(--muted);
  margin: 0 0 18px;
}
.itinerary-card {
  padding: 0;
  overflow: hidden;
  margin-bottom: 16px;
}
.itinerary-top {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 24px;
  border-bottom: 1px solid var(--border);
  background: #fcfdfd;
  font-size: 12px;
}
.candidate-number {
  color: var(--brand);
  background: var(--tint);
  border-radius: 5px;
  padding: 3px 9px;
  font-size: 10px;
}
.itinerary-top strong {
  font-weight: 500;
  color: var(--ink);
}
.flight-number {
  color: var(--muted);
  font-size: 11px;
}
.direct-tag {
  margin-left: auto;
  color: var(--brand);
  font-size: 11px;
}
.itinerary-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
  padding: 26px 32px;
}
.route-timeline {
  display: flex;
  align-items: center;
  gap: 35px;
  max-width: 600px;
  flex: 1;
}
.route-stop {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 74px;
}
.route-stop strong {
  font-size: 32px;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
  font-weight: 600;
  line-height: 1.4;
}
.route-stop > span {
  font-size: 14px;
  color: var(--text);
}
.route-stop > small {
  font-size: 10px;
  color: var(--muted);
}
.route-connector {
  flex: 1;
  min-width: 70px;
  max-width: 180px;
  text-align: center;
}
.route-connector > small {
  color: var(--muted);
  font-size: 10px;
  white-space: nowrap;
}
.route-connector > div {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 9px 0;
  color: var(--brand);
}
.route-connector > div > span {
  border-top: 1px solid #c0d6cf;
  flex: 1;
}
.route-connector i {
  font-size: 20px;
}
.itinerary-price {
  text-align: right;
  border-left: 1px dashed var(--border);
  padding-left: 32px;
  min-width: 210px;
}
.itinerary-price > small {
  font-size: 11px;
  color: var(--muted);
}
.itinerary-price .money {
  font-size: 30px;
  margin: 3px 0 9px;
}
.itinerary-price .money > span {
  font-size: 16px;
  margin-right: 3px;
}
.itinerary-price .el-button {
  font-size: 12px;
  padding: 12px 15px;
}
.itinerary-bottom {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 22px;
  background: #f7faf9;
  padding: 12px 24px;
  font-size: 10px;
  color: var(--muted);
}
.itinerary-bottom i {
  margin-right: 3px;
}
.composer .el-button--primary.is-disabled,
.composer .el-button--primary.is-disabled:hover {
  background: #bdd5cf;
  border-color: #bdd5cf;
  color: #fff;
  box-shadow: none;
}
button:disabled {
  cursor: not-allowed;
}
@media (max-width: 1050px) {
  .planner-workspace {
    grid-template-columns: minmax(0, 1fr) 250px;
    gap: 18px;
  }
  .composer {
    padding: 24px;
  }
  .composer-heading {
    align-items: start;
    flex-direction: column;
    gap: 10px;
  }
  .hero-copy h1 {
    font-size: 29px;
  }
  .planner-hero {
    padding: 32px;
  }
  .visual-route {
    gap: 18px;
  }
  .visual-route > span {
    font-size: 14px;
  }
  .guide-card {
    padding: 22px 18px;
  }
  .example-grid {
    gap: 8px;
  }
  .example-grid button {
    padding: 12px 9px;
  }
  .example-grid span {
    font-size: 9px;
  }
}
@media (max-width: 850px) {
  .planner-workspace {
    grid-template-columns: 1fr;
  }
  .composer-heading {
    flex-direction: row;
    align-items: center;
  }
  .planner-sidebar {
    grid-template-columns: 1.4fr 1fr;
  }
  .guide-card li {
    padding-bottom: 17px;
  }
  .guide-card br {
    display: none;
  }
  .guide-card h2 {
    margin-bottom: 18px;
  }
  .scope-card {
    align-self: start;
  }
  .hero-visual {
    display: none;
  }
  .planner-hero {
    grid-template-columns: 1fr;
  }
  .route-timeline {
    gap: 20px;
  }
  .itinerary-main {
    gap: 22px;
    padding: 24px;
  }
  .itinerary-price {
    padding-left: 22px;
    min-width: 180px;
  }
}
@media (max-width: 600px) {
  .planner-hero {
    padding: 27px 24px;
    border-radius: 18px;
    margin-bottom: 20px;
  }
  .hero-copy h1 {
    font-size: 28px;
    margin-top: 16px;
  }
  .mobile-break {
    display: block;
  }
  .hero-copy p {
    font-size: 12px;
    line-height: 1.8;
  }
  .eyebrow {
    font-size: 10px;
  }
  .hero-tags {
    gap: 7px;
  }
  .hero-tags span {
    font-size: 10px;
    padding: 3px 10px;
  }
  .composer {
    padding: 22px 18px;
  }
  .composer-heading {
    flex-direction: column;
    align-items: start;
  }
  .composer-heading h2 {
    font-size: 19px;
  }
  .input-tabs {
    gap: 20px;
    margin: 22px 0;
  }
  .input-tabs button {
    font-size: 12px;
  }
  .examples-heading {
    align-items: start;
    flex-direction: column;
    gap: 3px;
  }
  .example-grid {
    grid-template-columns: 1fr;
  }
  .example-grid button {
    display: grid;
    grid-template-columns: 25px 78px 1fr;
    align-items: center;
    padding: 13px 12px;
    gap: 6px;
  }
  .example-grid button > i:first-child {
    margin: 0;
  }
  .example-grid span {
    margin: 0;
    font-size: 10px;
  }
  .example-grid .el-icon-top-right {
    display: none;
  }
  .planner-actions {
    flex-direction: column;
  }
  .planner-actions .el-button {
    width: 100%;
  }
  .composer-footer > small {
    font-size: 10px;
    line-height: 1.8;
  }
  .form-grid {
    grid-template-columns: 1fr;
  }
  .budget-hint {
    display: block;
    margin: 8px 0 0;
  }
  .budget-field .el-input-number {
    max-width: 100%;
  }
  .planner-sidebar {
    grid-template-columns: 1fr;
  }
  .guide-card {
    padding: 22px;
  }
  .guide-card h2 {
    font-size: 16px;
  }
  .scope-card {
    padding: 22px;
  }
  .planner-results {
    margin-top: 27px;
  }
  .results-heading h2 {
    font-size: 21px;
  }
  .result-state {
    padding: 24px 20px;
    flex-direction: column;
    gap: 16px;
  }
  .result-state > i {
    font-size: 28px;
    padding: 12px;
  }
  .result-state h3 {
    font-size: 17px;
  }
  .initial-state {
    padding: 30px 22px;
  }
  .initial-state p {
    line-height: 1.9;
  }
  .constraint-tags {
    gap: 8px;
  }
  .constraint-tags > span {
    font-size: 11px;
    padding: 5px 9px;
  }
  .constraint-tags button {
    margin-left: 0;
  }
  .itinerary-top {
    padding: 14px 18px;
    gap: 8px;
    flex-wrap: wrap;
  }
  .itinerary-top strong {
    font-size: 11px;
  }
  .flight-number {
    font-size: 10px;
  }
  .direct-tag {
    font-size: 10px;
  }
  .itinerary-main {
    display: block;
    padding: 20px;
  }
  .route-timeline {
    gap: 18px;
    max-width: none;
  }
  .route-stop strong {
    font-size: 28px;
  }
  .route-stop {
    min-width: 65px;
  }
  .route-connector {
    max-width: none;
  }
  .route-stop:last-child {
    text-align: right;
  }
  .itinerary-price {
    border-left: 0;
    border-top: 1px dashed var(--border);
    padding: 18px 0 0;
    margin-top: 20px;
    display: grid;
    grid-template-columns: 1fr auto;
    text-align: left;
    align-items: center;
  }
  .itinerary-price > small {
    grid-column: 1;
  }
  .itinerary-price .money {
    grid-column: 1;
    margin-bottom: 0;
  }
  .itinerary-price .el-button {
    grid-column: 2;
    grid-row: 1/3;
    padding: 12px;
    font-size: 11px;
  }
  .itinerary-bottom {
    padding: 12px 20px;
    gap: 8px 16px;
    line-height: 1.8;
  }
  .loading-state {
    padding: 24px;
  }
  .loading-heading {
    gap: 12px;
  }
  .loading-heading h3 {
    font-size: 14px;
  }
}
</style>
