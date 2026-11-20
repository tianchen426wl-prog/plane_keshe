<template>
  <div class="search-panel surface">
    <div class="search-kicker">
      <strong>单程机票</strong
      ><span class="muted">选择出发地、目的地与日期</span>
    </div>
    <el-form class="search-form" @submit.native.prevent="submit"
      ><div class="search-fields">
        <label
          >出发城市/机场<el-autocomplete
            v-model="form.departure"
            placeholder="请输入出发城市"
            :fetch-suggestions="suggest"
            clearable
            ><template slot-scope="{ item }"
              ><span>{{ item.label || item.value }}</span></template
            ></el-autocomplete
          ></label
        ><el-button
          class="swap"
          aria-label="交换出发地与目的地"
          native-type="button"
          @click="swap"
          >⇄</el-button
        ><label
          >到达城市/机场<el-autocomplete
            v-model="form.arrival"
            placeholder="请输入到达城市"
            :fetch-suggestions="suggest"
            clearable
            ><template slot-scope="{ item }"
              ><span>{{ item.label || item.value }}</span></template
            ></el-autocomplete
          ></label
        ><label
          >出发日期<el-date-picker
            v-model="form.date"
            type="date"
            placeholder="选择日期"
            :picker-options="pickerOptions"
            value-format="yyyy-MM-dd" /></label
        ><label class="quantity-field"
          >成人旅客<el-input-number
            v-model="form.quantity"
            :min="1"
            :max="5"
            controls-position="right"
            aria-label="成人旅客人数" /></label
        ><el-button
          type="primary"
          native-type="submit"
          data-testid="search-flights"
          :loading="loading"
          >查询航班</el-button
        ><el-button
          v-if="resettable"
          native-type="button"
          @click="$emit('reset')"
          >重置</el-button
        >
      </div></el-form
    >
  </div>
</template>
<script>
import { getAirports } from '../api/hbxx'
export default {
  props: {
    value: { type: Object, required: true },
    loading: Boolean,
    resettable: Boolean,
    windowStart: String,
    windowEnd: String,
  },
  data() {
    return { form: { quantity: 1, ...this.value }, airports: [] }
  },
  created() {
    getAirports()
      .then((r) => {
        this.airports = r.data || []
      })
      .catch(() => {})
  },
  computed: {
    pickerOptions() {
      const start = this.windowStart,
        end = this.windowEnd
      return {
        disabledDate(date) {
          const value = `${date.getFullYear()}-${String(
            date.getMonth() + 1
          ).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
          return (start && value < start) || (end && value > end)
        },
      }
    },
  },
  watch: {
    value: {
      deep: true,
      handler(v) {
        this.form = { quantity: 1, ...v }
      },
    },
  },
  methods: {
    suggest(query, done) {
      const choices = [...new Set(this.airports.map((a) => a.city))].map(
        (value) => ({ value })
      )
      this.airports.forEach((a) =>
        choices.push({ value: a.code, label: a.city + ' · ' + a.name })
      )
      const text = (query || '').toUpperCase()
      done(
        choices
          .filter((x) =>
            (x.value + ' ' + (x.label || '')).toUpperCase().includes(text)
          )
          .slice(0, 20)
      )
    },
    swap() {
      const f = this.form
      ;[f.departure, f.arrival] = [f.arrival, f.departure]
    },
    submit() {
      this.form.departure = this.form.departure.trim()
      this.form.arrival = this.form.arrival.trim()
      this.$emit('input', { ...this.form })
      this.$emit('search', { ...this.form })
    },
  },
}
</script>
<style scoped>
.search-panel {
  box-shadow: 0 10px 35px #17345b08;
}
.search-kicker {
  display: flex;
  gap: 18px;
  margin-bottom: 20px;
}
.search-kicker strong {
  color: var(--brand);
}
.search-fields {
  display: flex;
  align-items: end;
  gap: 12px;
}
.search-fields label {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 7px;
  color: var(--muted);
  font-size: 12px;
}
.search-fields .el-date-editor {
  width: 100%;
}
.search-fields .el-input-number {
  width: 100%;
}
.search-fields > .el-button {
  height: 46px;
  margin: 0;
}
.swap {
  padding: 0 12px;
}
@media (max-width: 900px) {
  .search-fields {
    flex-wrap: wrap;
  }
  .search-fields label {
    flex: 1 1 25%;
  }
  .search-fields > .el-button--primary {
    flex: 1;
  }
}
@media (max-width: 600px) {
  .search-kicker {
    flex-direction: column;
    gap: 4px;
  }
  .swap {
    display: none;
  }
  .search-fields label {
    flex: 1 1 40%;
  }
  .search-fields label:last-of-type {
    flex-basis: 100%;
  }
  .search-fields > .el-button {
    flex: 1;
  }
}
</style>
