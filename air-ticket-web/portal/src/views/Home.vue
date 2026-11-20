<template>
  <div class="home">
    <section class="home-hero" aria-label="出行推荐">
      <el-carousel
        height="460px"
        :interval="6000"
        arrow="always"
        indicator-position="inside"
        :pause-on-hover="true"
      >
        <el-carousel-item v-for="(slide, index) in slides" :key="slide.path">
          <div
            class="hero-slide"
            :style="{
              backgroundImage: `linear-gradient(90deg, rgba(8,32,51,.90), rgba(8,32,51,.40) 65%, rgba(8,32,51,.18)), url('${slide.image}')`,
            }"
          >
            <div class="hero-inner">
              <span class="hero-tag">{{ slide.tag }}</span>
              <h1>{{ slide.title }}</h1>
              <p>{{ slide.text }}</p>
              <el-button class="hero-action" @click="$router.push(slide.path)"
                >{{ slide.action }} <i class="el-icon-right" /></el-button
              ><span class="slide-number">0{{ index + 1 }} / 03</span>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </section>
    <div class="hero-search">
      <SearchPanel v-model="searchForm" @search="search" />
    </div>
    <div class="portal-content home-content">
      <div class="quick-links">
        <router-link v-for="item in shortcuts" :key="item.path" :to="item.path"
          ><i :class="item.icon" />
          <div>
            <strong>{{ item.title }}</strong
            ><small>{{ item.text }}</small>
          </div>
          <i class="el-icon-top-right"
        /></router-link>
      </div>
      <section aria-labelledby="flights-title">
        <div class="section-heading">
          <div>
            <span class="section-kicker">FLIGHT OVERVIEW</span>
            <h2 id="flights-title">航班简览</h2>
            <p class="muted">发现可售航线，开启下一段旅程。</p>
          </div>
          <router-link to="/flight"
            >全部航班 <i class="el-icon-right"
          /></router-link>
        </div>
        <div v-if="error" class="ui-error" role="alert">
          {{ error }} <el-button type="text" @click="load">重新加载</el-button>
        </div>
        <el-skeleton v-if="loading" :rows="5" animated />
        <div v-else class="home-grid">
          <article
            v-for="(f, index) in flights"
            :key="f.hbid"
            class="surface route-card"
          >
            <div class="route-art">
              <span class="route-index">0{{ index + 1 }} / 航线</span>
              <div>{{ f.start }} <i class="el-icon-right" /> {{ f.end }}</div>
              <i class="el-icon-position route-plane" />
            </div>
            <div class="route-info">
              <span>{{ f.companyName || '航班信息' }}</span
              ><span class="pill">{{ f.flightNo || f.hbid }}</span>
            </div>
            <p class="muted">
              {{ date(f.starttime) }} · {{ f.duration || '查看航班详情' }}
            </p>
            <el-button type="text" @click="route(f)"
              >查看航班与舱位 <i class="el-icon-right"
            /></el-button>
          </article>
          <el-empty
            v-if="!flights.length"
            class="section-empty"
            description="暂无可售航班，可前往航班查询选择其他日期"
            ><el-button @click="$router.push('/flight')"
              >查询航班</el-button
            ></el-empty
          >
        </div>
      </section>
      <section class="planner-feature" aria-labelledby="planner-title">
        <div class="planner-copy">
          <span class="section-kicker">YOUR NEXT JOURNEY</span>
          <h2 id="planner-title">想去哪里？<br />让灵感先起飞。</h2>
          <p>
            告诉智能规划你的出发地、目的地和预算，<br />从可售航班中寻找适合你的出行方案。
          </p>
          <el-button type="primary" @click="$router.push('/planner')"
            >开始智能规划 <i class="el-icon-right"
          /></el-button>
        </div>
        <div class="planner-preview">
          <span class="planner-label"
            ><i class="el-icon-magic-stick" /> 智能行程规划</span
          >
          <div class="prompt-example">
            “从上海出发，周末去成都，帮我找一个合适的航班。”
          </div>
          <div class="planner-steps">
            <span><i class="el-icon-location-outline" /> 确定目的地</span
            ><span><i class="el-icon-date" /> 匹配出行日期</span
            ><span><i class="el-icon-position" /> 推荐航班方案</span>
          </div>
          <small>输入你的出行需求，开启专属行程。</small>
        </div>
      </section>
      <section aria-labelledby="news-title">
        <div class="section-heading">
          <div>
            <span class="section-kicker">TRAVEL JOURNAL</span>
            <h2 id="news-title">新闻与出行资讯</h2>
          </div>
          <router-link to="/news"
            >全部资讯 <i class="el-icon-right"
          /></router-link>
        </div>
        <el-skeleton v-if="loading" :rows="3" animated />
        <div v-else class="home-grid">
          <router-link
            v-for="(n, index) in news"
            :key="n.id"
            :to="'/news/' + n.id"
            class="surface news-tile"
            ><div class="news-meta">
              <span class="pill">航空资讯</span
              ><span class="muted">0{{ index + 1 }}</span>
            </div>
            <h3>{{ n.title }}</h3>
            <p>{{ n.profile || '了解航空动态，为下一次出行做好准备。' }}</p>
            <small class="muted"
              >{{ date(n.time) }} <span>阅读全文 ↗</span></small
            ></router-link
          ><el-empty
            v-if="!news.length"
            class="section-empty"
            description="暂无资讯，更多航空动态即将更新"
          />
        </div>
      </section>
      <section aria-labelledby="companies-title">
        <div class="section-heading">
          <div>
            <span class="section-kicker">AIRLINE DIRECTORY</span>
            <h2 id="companies-title">航空公司</h2>
          </div>
          <router-link to="/company"
            >查看全部 <i class="el-icon-right"
          /></router-link>
        </div>
        <div class="home-grid companies">
          <router-link
            v-for="c in companies"
            :key="c.companyId"
            :to="'/company/' + c.companyId"
            class="surface company-tile"
            ><img
              :src="companyImage(c.companyName, c.companyPicture)"
              :alt="c.companyName"
              @error="companyImageError"
              loading="lazy"
            />
            <div>
              <h3>{{ c.companyName }}</h3>
              <span class="muted">查看航司详情 ↗</span>
            </div></router-link
          ><el-empty
            v-if="!loading && !companies.length"
            class="section-empty"
            description="暂无航空公司信息"
          />
        </div>
      </section>
    </div>
  </div>
</template>
<script>
import SearchPanel from '../components/SearchPanel.vue'
import { searchFlights } from '../api/hbxx'
import { getCompanyList } from '../api/company'
import { getNewsList } from '../api/news'
import { getCompanyImage, companyImageError } from '../utils/companyImage'
export default {
  components: { SearchPanel },
  data() {
    return {
      searchForm: { departure: '', arrival: '', date: '' },
      slides: [
        {
          tag: '每一程，都值得期待',
          title: '把向往，变成下一站。',
          text: '从航班查询到行程规划，让出发更简单。',
          image: '/images/banner/轮播图2.jpg',
          action: '探索可售航班',
          path: '/flight',
        },
        {
          tag: '智能规划 · 从想法到行程',
          title: '你的灵感，我们来规划。',
          text: '说出目的地与出行偏好，发现适合你的飞行方案。',
          image: '/images/banner/轮播图.png',
          action: '开启智能规划',
          path: '/planner',
        },
        {
          tag: '掌握资讯 · 从容出发',
          title: '出发之前，多了解一点。',
          text: '了解航空动态与出行资讯，为下一程做好准备。',
          image: '/images/banner/轮播图2.jpg',
          action: '阅读出行资讯',
          path: '/news',
        },
      ],
      shortcuts: [
        {
          icon: 'el-icon-position',
          title: '航班查询',
          text: '查时刻、选舱位、轻松预订',
          path: '/flight',
        },
        {
          icon: 'el-icon-magic-stick',
          title: '智能规划',
          text: '让出行需求变成行程方案',
          path: '/planner',
        },
        {
          icon: 'el-icon-tickets',
          title: '我的订单',
          text: '查看预订，随时管理行程',
          path: '/user/orders',
        },
        {
          icon: 'el-icon-reading',
          title: '新闻资讯',
          text: '航空动态与实用出行信息',
          path: '/news',
        },
      ],
      flights: [],
      companies: [],
      news: [],
      loading: false,
      error: '',
    }
  },
  created() {
    this.load()
  },
  methods: {
    companyImage: getCompanyImage,
    companyImageError,
    date(v) {
      if (!v || isNaN(new Date(v).getTime())) return '日期待更新'
      return new Date(v).toLocaleDateString('zh-CN', {
        timeZone: 'Asia/Shanghai',
      })
    },
    search() {
      this.$router.push({ path: '/flight', query: { ...this.searchForm } })
    },
    route(f) {
      this.$router.push({
        path: '/flight',
        query: { departure: f.start, arrival: f.end },
      })
    },
    async load() {
      this.loading = true
      this.error = ''
      const result = await Promise.allSettled([
        searchFlights({ page: 1, pageSize: 100 }),
        getCompanyList({ page: 1, pageSize: 4 }),
        getNewsList({ page: 1, pageSize: 3 }),
      ])
      ;['flights', 'companies', 'news'].forEach((key, i) => {
        if (result[i].status === 'fulfilled')
          this[key] =
            key === 'flights'
              ? (result[i].value.data.records || [])
                  .filter(
                    (f) =>
                      f.number > 0 &&
                      f.saleStatus === 1 &&
                      f.operationalStatus === 'SCHEDULED' &&
                      new Date(f.saleCloseAt || f.starttime).getTime() >
                        Date.now() &&
                      (!f.saleOpenAt ||
                        new Date(f.saleOpenAt).getTime() <= Date.now())
                  )
                  .slice(0, 3)
              : result[i].value.data.records || []
        else this.error = '部分内容暂时无法加载，请稍后重试。'
      })
      this.loading = false
    },
  },
}
</script>
<style scoped>
.home-hero {
  background: var(--ink);
}
.hero-slide {
  height: 100%;
  background-size: cover;
  background-position: center 58%;
}
.hero-inner {
  max-width: 1200px;
  margin: auto;
  padding: 64px 60px 90px;
  color: white;
  position: relative;
  height: 100%;
}
.hero-tag {
  display: inline-block;
  letter-spacing: 3px;
  font-size: 13px;
  border-left: 3px solid #56d5c5;
  padding-left: 12px;
  color: #d1efed;
}
.hero-inner h1 {
  font-size: 46px;
  letter-spacing: 1px;
  font-weight: 600;
  margin: 23px 0 14px;
  line-height: 1.4;
}
.hero-inner p {
  color: #e0e9ee;
  font-size: 16px;
  margin-bottom: 27px;
}
.hero-action {
  background: #fff;
  border: 0;
  color: #123c50;
  padding: 14px 22px;
}
.slide-number {
  position: absolute;
  right: 60px;
  bottom: 82px;
  letter-spacing: 4px;
  font-size: 12px;
  color: #d3e6e8;
}
.hero-search {
  max-width: 1152px;
  margin: -46px auto 0;
  position: relative;
  z-index: 3;
}
.home-content {
  padding-top: 28px;
}
.quick-links {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 22px;
  padding: 12px 0 28px;
  border-bottom: 1px solid var(--border);
}
.quick-links > a {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--ink);
  padding: 12px 0;
}
.quick-links > a > i:first-child {
  font-size: 23px;
  padding: 13px;
  background: var(--tint);
  border-radius: 14px;
  color: var(--brand);
}
.quick-links strong {
  display: block;
  font-size: 16px;
}
.quick-links small {
  color: var(--muted);
  font-size: 11px;
  display: block;
  margin-top: 4px;
}
.quick-links .el-icon-top-right {
  margin-left: auto;
  color: var(--muted);
}
.section-kicker {
  font-size: 10px;
  letter-spacing: 2px;
  color: var(--brand);
  font-weight: 700;
  display: block;
  margin-bottom: 8px;
}
.section-heading {
  margin-top: 38px;
}
.section-heading p {
  margin: 7px 0 0;
}
.section-heading > a {
  font-size: 13px;
  white-space: nowrap;
}
.home-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}
.section-empty {
  grid-column: 1/-1;
  background: white;
  border: 1px dashed var(--border);
  border-radius: var(--radius);
}
.route-card {
  padding: 0 22px 12px;
  overflow: hidden;
}
.route-art {
  position: relative;
  background: linear-gradient(120deg, #e0efed, #f2f6ee);
  padding: 22px;
  margin: 0 -22px 20px;
  overflow: hidden;
}
.route-art > div {
  font-size: 25px;
  color: var(--ink);
  margin-top: 16px;
  position: relative;
  z-index: 1;
}
.route-art > div i {
  font-size: 18px;
  margin: 0 10px;
  color: var(--brand);
}
.route-index {
  color: var(--muted);
  font-size: 10px;
  letter-spacing: 2px;
}
.route-plane {
  position: absolute;
  right: 15px;
  top: 15px;
  font-size: 90px;
  color: #b8d9d3;
  transform: rotate(-15deg);
}
.route-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.route-card p {
  font-size: 12px;
  margin: 12px 0 0;
}
.planner-feature {
  margin-top: 42px;
  border-radius: 20px;
  background: #102f42;
  color: #fff;
  padding: 40px 48px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 56px;
  position: relative;
  overflow: hidden;
}
.planner-feature .section-kicker {
  color: #72d8c8;
}
.planner-copy h2 {
  font-size: 32px;
  line-height: 1.5;
  margin: 12px 0;
}
.planner-copy p {
  color: #bdd0da;
  line-height: 1.9;
}
.planner-copy .el-button {
  background: #bce9dc;
  border-color: #bce9dc;
  color: #103e3d;
}
.planner-preview {
  align-self: center;
  background: #ffffff0c;
  border: 1px solid #ffffff26;
  border-radius: 16px;
  padding: 26px;
  transform: rotate(-2deg);
}
.planner-label {
  color: #a6e0d7;
  font-size: 13px;
}
.prompt-example {
  border-radius: 10px;
  background: #ffffff12;
  padding: 18px;
  margin: 18px 0;
  line-height: 1.8;
}
.planner-steps {
  display: flex;
  flex-direction: column;
  gap: 12px;
  font-size: 13px;
  margin-bottom: 22px;
}
.planner-steps i {
  color: #75cdbd;
  margin-right: 10px;
}
.planner-preview small {
  color: #a5bdc9;
}
.news-tile {
  color: var(--text);
  transition: transform 0.2s, border-color 0.2s;
}
.news-tile:hover,
.company-tile:hover {
  transform: translateY(-3px);
  border-color: #8bb9b3;
}
.news-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.news-tile h3 {
  font-size: 18px;
  line-height: 1.6;
  margin: 20px 0 12px;
}
.news-tile p {
  color: var(--muted);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.8;
  min-height: 76px;
}
.news-tile small {
  display: flex;
  justify-content: space-between;
  border-top: 1px solid var(--border);
  padding-top: 16px;
}
.news-tile small span {
  color: var(--brand);
}
.companies {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}
.company-tile {
  display: flex;
  gap: 14px;
  align-items: center;
  transition: transform 0.2s, border-color 0.2s;
}
.company-tile img {
  width: 48px;
  height: 48px;
  object-fit: contain;
}
.company-tile h3 {
  font-size: 15px;
  color: var(--ink);
  margin: 0 0 4px;
}
.company-tile .muted {
  font-size: 11px;
}
@media (max-width: 1000px) {
  .hero-search {
    margin: -46px 24px 0;
  }
  .quick-links {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .companies {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .planner-feature {
    gap: 24px;
    padding: 32px;
  }
  .hero-inner h1 {
    font-size: 38px;
  }
}
@media (max-width: 650px) {
  .hero-inner {
    padding: 52px 34px 85px;
  }
  .hero-inner h1 {
    font-size: 30px;
    max-width: 270px;
  }
  .hero-inner p {
    font-size: 14px;
    max-width: 260px;
  }
  .hero-search {
    margin: -40px 16px 0;
  }
  .slide-number {
    right: 32px;
  }
  .home-grid {
    grid-template-columns: 1fr;
  }
  .quick-links {
    gap: 6px 16px;
  }
  .quick-links > a {
    gap: 8px;
  }
  .quick-links > a > i:first-child {
    font-size: 20px;
    padding: 9px;
  }
  .quick-links small {
    font-size: 10px;
  }
  .quick-links .el-icon-top-right {
    display: none;
  }
  .planner-feature {
    grid-template-columns: 1fr;
    padding: 28px 24px;
    gap: 20px;
  }
  .planner-copy h2 {
    font-size: 28px;
  }
  .planner-preview {
    transform: none;
    padding: 20px;
  }
  .companies {
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }
  .company-tile {
    flex-direction: column;
    align-items: start;
    padding: 16px;
  }
  .section-heading {
    gap: 12px;
  }
  .section-heading h2 {
    font-size: 21px;
  }
}
</style>
