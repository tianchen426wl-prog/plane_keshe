<template>
  <div class="dashboard">
    <div class="welcome-card">
      <h2>欢迎回来，{{ admin ? admin.username : '管理员' }}</h2>
      <p>这是航空售票管理系统的后台管理首页。</p>
    </div>
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card-blue">
          <div class="stat-icon">
            <i class="el-icon-user"></i>
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ stats.userCount }}</div>
            <div class="stat-label">平台用户</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card-green">
          <div class="stat-icon">
            <i class="el-icon-s-promotion"></i>
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ stats.flightCount }}</div>
            <div class="stat-label">航班数量</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card-orange">
          <div class="stat-icon">
            <i class="el-icon-s-order"></i>
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ stats.orderCount }}</div>
            <div class="stat-label">订单总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card-red">
          <div class="stat-icon">
            <i class="el-icon-s-claim"></i>
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ stats.refundCount }}</div>
            <div class="stat-label">退订申请</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-card class="quick-links">
      <div slot="header">
        <span>快捷操作</span>
      </div>
      <el-row :gutter="12">
        <el-col :span="4" v-for="menu in quickMenus" :key="menu.title">
          <div class="quick-link-item" @click="$router.push(menu.path)">
            <i :class="menu.icon"></i>
            <span>{{ menu.title }}</span>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script>
import { mapState } from 'vuex'
import { getUserList } from '@/api/admin'
import { searchList } from '@/api/hbxx'
import { getOrderList } from '@/api/jpdd'
import { getRefundList } from '@/api/tdjp'

export default {
  name: 'DashboardIndex',
  data() {
    return {
      stats: {
        userCount: '-',
        flightCount: '-',
        orderCount: '-',
        refundCount: '-'
      }
    }
  },
  computed: {
    ...mapState(['admin']),
    quickMenus() {
      return this.$store.state.menus || []
    }
  },
  created() {
    this.fetchStats()
  },
  methods: {
    async fetchStats() {
      try {
        const [userRes, flightRes, orderRes, refundRes] = await Promise.allSettled([
          getUserList(),
          searchList({ page: 1, pageSize: 1 }),
          getOrderList({ page: 1, pageSize: 1 }),
          getRefundList({ page: 1, pageSize: 1, process: 0 })
        ])
        if (userRes.status === 'fulfilled' && userRes.value.data) {
          const userData = userRes.value.data
          if (Array.isArray(userData)) {
            this.stats.userCount = userData.length
          } else {
            this.stats.userCount = userData.total ?? 0
          }
        }
        if (flightRes.status === 'fulfilled' && flightRes.value.data) {
          this.stats.flightCount = flightRes.value.data.total ?? 0
        }
        if (orderRes.status === 'fulfilled' && orderRes.value.data) {
          this.stats.orderCount = orderRes.value.data.total ?? 0
        }
        if (refundRes.status === 'fulfilled' && refundRes.value.data) {
          this.stats.refundCount = refundRes.value.data.total ?? 0
        }
      } catch (e) {
        // placeholder
      }
    }
  }
}
</script>

<style scoped>
.dashboard {
  max-width: 1200px;
}
.welcome-card {
  background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);
  color: #fff;
  padding: 30px;
  border-radius: 8px;
  margin-bottom: 20px;
}
.welcome-card h2 {
  margin: 0 0 10px;
}
.welcome-card p {
  margin: 0;
  opacity: 0.85;
}
.stats-row {
  margin-bottom: 20px;
}
.stat-card {
  display: flex;
  align-items: center;
  cursor: pointer;
}
.stat-card >>> .el-card__body {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 20px;
}
.stat-icon {
  font-size: 40px;
  margin-right: 15px;
}
.stat-card-blue .stat-icon { color: #409eff; }
.stat-card-blue { border-top: 3px solid #409eff; }
.stat-card-green .stat-icon { color: #67c23a; }
.stat-card-green { border-top: 3px solid #67c23a; }
.stat-card-orange .stat-icon { color: #e6a23c; }
.stat-card-orange { border-top: 3px solid #e6a23c; }
.stat-card-red .stat-icon { color: #f56c6c; }
.stat-card-red { border-top: 3px solid #f56c6c; }
.stat-num {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}
.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}
.quick-links {
  margin-top: 20px;
}
.quick-link-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 10px;
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.3s;
}
.quick-link-item:hover {
  background: #f0f2f5;
}
.quick-link-item i {
  font-size: 28px;
  color: #409eff;
  margin-bottom: 8px;
}
.quick-link-item span {
  font-size: 13px;
  color: #606266;
}
</style>