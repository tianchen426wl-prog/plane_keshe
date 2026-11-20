<template>
  <div class="admin-shell">
    <div v-if="mobileOpen" class="nav-mask" @click="mobileOpen = false" />
    <aside class="admin-sidebar" :class="{ collapsed, mobileOpen }">
      <router-link class="admin-brand" to="/admin/dashboard"
        ><i class="el-icon-position" />
        <span v-if="!collapsed">云际航空管理</span></router-link
      >
      <nav aria-label="后台导航">
        <section v-for="group in groups" :key="group.title">
          <div v-if="!collapsed" class="nav-group">{{ group.title }}</div>
          <router-link
            v-for="m in group.items"
            :key="m.id"
            :to="m.path"
            @click.native="mobileOpen = false"
            ><i :class="m.icon || 'el-icon-menu'" /><span v-if="!collapsed">{{
              m.title
            }}</span></router-link
          >
        </section>
      </nav>
    </aside>
    <div class="admin-workspace">
      <header class="admin-top">
        <el-button
          class="desktop-toggle"
          type="text"
          :aria-label="collapsed ? '展开导航' : '折叠导航'"
          @click="collapsed = !collapsed"
          ><i class="el-icon-s-fold" /></el-button
        ><el-button
          class="mobile-toggle"
          type="text"
          aria-label="打开导航"
          @click="
            collapsed = false
            mobileOpen = !mobileOpen
          "
          ><i class="el-icon-menu" /></el-button
        ><span class="muted">工作空间 / {{ title }}</span
        ><el-dropdown class="admin-account" @command="logout"
          ><el-button
            >{{ admin ? admin.username : '管理员' }}
            <i class="el-icon-arrow-down" /></el-button
          ><el-dropdown-menu slot="dropdown"
            ><el-dropdown-item>退出登录</el-dropdown-item></el-dropdown-menu
          ></el-dropdown
        >
      </header>
      <main class="admin-main">
        <div class="admin-page-header">
          <h1>{{ title }}</h1>
          <p>{{ subtitle }}</p>
        </div>
        <router-view />
      </main>
    </div>
  </div>
</template>
<script>
import { mapState } from 'vuex'
export default {
  data() {
    return { collapsed: false, mobileOpen: false }
  },
  computed: {
    ...mapState(['admin', 'menus']),
    title() {
      const m = (this.menus || []).find((m) => m.path === this.$route.path)
      return m ? m.title : '工作台'
    },
    subtitle() {
      return (
        {
          '/admin/dashboard': '概览平台数据，开始今天的管理工作。',
          '/admin/flight': '维护航班销售状态、舱位价格和座位容量。',
          '/admin/refund': '处理退订申请，保留每一次审核记录。',
          '/admin/order': '查询订单与行程，追踪交易状态变化。',
        }[this.$route.path] || '查看并维护业务资料，操作按当前账号权限执行。'
      )
    },
    groups() {
      const defs = [
        ['工作空间', ['dashboard']],
        ['基础资料', ['user', 'company', 'flight']],
        ['交易管理', ['order', 'refund']],
        ['内容管理', ['news']],
        ['系统设置', ['menu', 'role']],
      ]
      const used = new Set()
      const groups = defs.map(([title, paths]) => ({
        title,
        items: (this.menus || []).filter((m) =>
          paths.some((p) => m.path === '/admin/' + p)
        ),
      }))
      groups.forEach((g) => g.items.forEach((m) => used.add(m.id)))
      const other = (this.menus || []).filter((m) => !used.has(m.id))
      if (other.length) groups.push({ title: '其他功能', items: other })
      return groups.filter((g) => g.items.length)
    },
  },
  watch: {
    $route() {
      this.mobileOpen = false
    },
  },
  methods: {
    async logout() {
      try {
        await this.$confirm('确定退出登录吗？', '退出登录')
        await fetch('/api/admin/logout', {
          method: 'POST',
          headers: { Authorization: 'Bearer ' + this.$store.state.token },
        })
        this.$store.commit('LOGOUT')
        this.$router.push('/admin/login')
      } catch (e) {}
    },
  },
}
</script>
<style scoped>
.admin-sidebar {
  width: 232px;
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid var(--border);
  overflow: auto;
  transition: width 0.15s;
}
.admin-sidebar.collapsed {
  width: 68px;
}
.admin-brand {
  height: 76px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 22px;
  color: var(--ink);
  font-size: 18px;
  font-weight: 700;
  white-space: nowrap;
}
.admin-brand i {
  color: var(--brand);
  font-size: 23px;
}
.admin-sidebar nav {
  padding: 4px 12px 28px;
}
.nav-group {
  font-size: 11px;
  letter-spacing: 2px;
  color: var(--muted);
  padding: 22px 14px 7px;
}
.admin-sidebar nav a {
  display: flex;
  align-items: center;
  gap: 12px;
  border-radius: 8px;
  padding: 12px 14px;
  color: var(--muted);
  margin: 3px 0;
  white-space: nowrap;
}
.admin-sidebar nav a.router-link-exact-active {
  background: var(--tint);
  color: var(--brand);
  font-weight: 600;
}
.admin-sidebar nav a i {
  font-size: 18px;
}
.admin-top {
  height: 64px;
  flex-shrink: 0;
  background: white;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 0 28px;
}
.admin-top .el-button--text {
  font-size: 21px;
}
.admin-account {
  margin-left: auto;
}
.mobile-toggle {
  display: none;
}
.nav-mask {
  display: none;
}
@media (max-width: 800px) {
  .admin-sidebar {
    position: fixed;
    inset: 0 auto 0 0;
    z-index: 101;
    width: 232px !important;
    transform: translateX(-100%);
  }
  .admin-sidebar.mobileOpen {
    transform: translateX(0);
  }
  .admin-top {
    padding: 0 16px;
    gap: 12px;
  }
  .desktop-toggle {
    display: none;
  }
  .mobile-toggle {
    display: inline-block;
  }
  .nav-mask {
    display: block;
    position: fixed;
    inset: 0;
    background: #0f274760;
    z-index: 100;
  }
  .admin-top > .muted {
    font-size: 12px;
  }
}
</style>
