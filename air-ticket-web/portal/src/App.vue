<template>
  <div class="portal-shell">
    <header class="portal-header">
      <router-link class="brand" to="/"
        ><i class="el-icon-position" aria-hidden="true" /> 云际航空<small
          >航空售票管理系统</small
        ></router-link
      ><button
        class="mobile-menu"
        aria-label="切换导航"
        :aria-expanded="String(open)"
        @click="open = !open"
      >
        <i class="el-icon-menu" />
      </button>
      <nav :class="{ open }" aria-label="主导航">
        <router-link to="/" exact>首页</router-link>
        <router-link to="/planner">智能规划</router-link
        ><router-link to="/flight">订机票</router-link
        ><router-link to="/company">航空公司</router-link
        ><router-link to="/news">出行资讯</router-link
        ><template v-if="user && user.token"
          ><router-link to="/user/orders">我的订单</router-link
          ><el-dropdown @command="account"
            ><el-button class="account-button"
              >{{ user.name || user.username }}
              <i class="el-icon-arrow-down" /></el-button
            ><el-dropdown-menu slot="dropdown"
              ><el-dropdown-item command="profile">个人中心</el-dropdown-item
              ><el-dropdown-item command="logout"
                >退出登录</el-dropdown-item
              ></el-dropdown-menu
            ></el-dropdown
          ></template
        ><template v-else
          ><router-link to="/login">登录</router-link
          ><router-link class="register-link" to="/register"
            >注册账户</router-link
          ></template
        >
      </nav>
    </header>
    <main :class="{ 'portal-content': $route.path !== '/' }">
      <router-view />
    </main>
    <footer class="portal-footer">
      <strong>云际航空</strong><span>清晰预订，从容出行。</span
      ><small>航班查询 · 在线预订 · 智能行程规划</small>
    </footer>
  </div>
</template>
<script>
import { mapState } from 'vuex'
export default {
  data() {
    return { open: false }
  },
  computed: { ...mapState(['user']) },
  watch: {
    $route() {
      this.open = false
    },
  },
  methods: {
    async account(command) {
      if (command === 'profile') {
        this.$router.push('/user/center')
        return
      }
      await this.$store.dispatch('logout')
      if (this.$route.path !== '/') this.$router.push('/')
      this.$message.success('已退出登录')
    },
  },
}
</script>
<style scoped>
.portal-header {
  min-height: 76px;
  max-width: 1200px;
  margin: auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}
.brand {
  font-size: 22px;
  font-weight: 700;
  color: var(--ink);
  white-space: nowrap;
}
.brand i {
  color: var(--brand);
}
.brand small {
  display: block;
  font-size: 10px;
  font-weight: 400;
  color: var(--muted);
  letter-spacing: 2px;
  margin-left: 28px;
}
.portal-header nav {
  display: flex;
  align-items: center;
  gap: 25px;
}
.portal-header nav > a {
  color: var(--muted);
}
.portal-header nav > .router-link-active {
  color: var(--brand);
  font-weight: 600;
}
.register-link {
  padding: 9px 15px;
  border: 1px solid var(--border);
  border-radius: 8px;
}
.mobile-menu {
  display: none;
}
.portal-footer {
  border-top: 1px solid var(--border);
  padding: 30px 24px;
  text-align: center;
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: 8px;
  color: var(--muted);
}
.portal-footer strong {
  color: var(--ink);
}
@media (max-width: 800px) {
  .portal-header {
    flex-wrap: wrap;
    padding: 14px 20px;
    gap: 14px;
  }
  .mobile-menu {
    display: block;
    background: white;
    border: 1px solid var(--border);
    border-radius: 8px;
    padding: 10px;
    font-size: 20px;
  }
  .portal-header nav {
    display: none;
    width: 100%;
    flex-wrap: wrap;
    gap: 20px;
    padding: 12px 0;
  }
  .portal-header nav.open {
    display: flex;
  }
}
</style>
