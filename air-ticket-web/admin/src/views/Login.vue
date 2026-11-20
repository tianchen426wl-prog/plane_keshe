<template>
  <div class="login-container">
    <div class="admin-auth-brand">
      <i class="el-icon-position" /> 云际航空 <span>运营管理平台</span>
    </div>
    <div class="admin-auth-layout">
      <aside class="admin-auth-story">
        <span class="admin-auth-eyebrow">YUNJI · OPERATIONS CENTER</span>
        <h1>让每一程运行，<br />井然有序。</h1>
        <p>连接航班、订单与旅客服务，<br />在一个工作台，掌握日常运营。</p>
        <div class="operations-visual" aria-hidden="true">
          <div class="orbit orbit-one"></div>
          <div class="orbit orbit-two"></div>
          <i class="el-icon-position" /><span class="orbit-dot dot-one"></span
          ><span class="orbit-dot dot-two"></span
          ><span class="visual-caption">航班 · 订单 · 服务</span>
        </div>
        <div class="admin-auth-modules">
          <span><i class="el-icon-position" /> 航班运营</span
          ><span><i class="el-icon-tickets" /> 订单管理</span
          ><span><i class="el-icon-data-analysis" /> 数据概览</span>
        </div>
      </aside>
      <div class="login-card">
        <span class="auth-form-kicker">ADMINISTRATOR ACCESS</span>
        <div class="admin-lock"><i class="el-icon-lock" /></div>
        <h2 class="login-title">欢迎登录管理平台</h2>
        <p class="login-subtitle">使用管理员账户，进入航空运营工作台。</p>
        <el-form
          ref="loginForm"
          :model="form"
          :rules="rules"
          label-position="top"
        >
          <el-form-item label="管理员账号" prop="username">
            <el-input
              v-model="form.username"
              prefix-icon="el-icon-user"
              placeholder="请输入用户名"
            />
          </el-form-item>
          <el-form-item label="登录密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              autocomplete="current-password"
              prefix-icon="el-icon-lock"
              placeholder="请输入密码"
              @keyup.enter.native="handleLogin"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              style="width: 100%"
              @click="handleLogin"
              >登录管理平台</el-button
            >
          </el-form-item>
        </el-form>
        <p class="admin-access-note">
          <i class="el-icon-lock" /> 仅限授权管理员访问
        </p>
      </div>
    </div>
    <footer class="admin-auth-footer">云际航空 · 航空售票管理系统</footer>
  </div>
</template>

<script>
import { login } from '@/api/admin'

export default {
  name: 'Login',
  data() {
    return {
      loading: false,
      form: {
        username: '',
        password: '',
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
        ],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
      },
    }
  },
  methods: {
    handleLogin() {
      this.$refs.loginForm.validate(async (valid) => {
        if (!valid) return
        this.loading = true
        try {
          const res = await login({
            username: this.form.username,
            password: this.form.password,
          })
          const data = res.data
          await this.$store.dispatch('login', {
            token: data.token,
            admin: data.admin,
            menus: data.menus || [],
          })
          this.$message.success('登录成功')
          this.$router.push('/admin/dashboard')
        } catch (e) {
          // error already shown by interceptor
        } finally {
          this.loading = false
        }
      })
    },
  },
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 32px 6vw 20px;
  background: radial-gradient(ellipse at 20% 45%, #e0eeeb, transparent 65%),
    #f4f7f8;
}
.admin-auth-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 22px;
  font-weight: 700;
  color: var(--ink);
}
.admin-auth-brand > i {
  font-size: 30px;
  color: var(--brand);
}
.admin-auth-brand span {
  font-size: 12px;
  color: var(--muted);
  font-weight: 400;
  letter-spacing: 2px;
  padding-left: 16px;
  border-left: 1px solid #becfce;
  margin-left: 8px;
}
.admin-auth-layout {
  display: grid;
  grid-template-columns: 1fr 440px;
  gap: 80px;
  align-items: center;
  max-width: 1120px;
  width: 100%;
  margin: 45px auto;
  flex: 1;
}
.admin-auth-story {
  padding: 20px 0;
}
.admin-auth-eyebrow {
  color: var(--brand);
  font-size: 11px;
  letter-spacing: 3px;
}
.admin-auth-story h1 {
  font-size: 44px;
  color: var(--ink);
  line-height: 1.5;
  font-weight: 600;
  margin: 22px 0 16px;
}
.admin-auth-story p {
  color: var(--muted);
  line-height: 1.9;
  font-size: 15px;
}
.operations-visual {
  position: relative;
  height: 180px;
  width: 340px;
  margin: 14px 0 24px;
}
.orbit {
  position: absolute;
  border: 1px solid #9ebeb8;
  border-radius: 50%;
  transform: rotate(-22deg);
}
.orbit-one {
  width: 300px;
  height: 110px;
  left: 10px;
  top: 30px;
}
.orbit-two {
  width: 230px;
  height: 150px;
  left: 45px;
  top: 12px;
  transform: rotate(25deg);
  border-style: dashed;
  border-color: #bfd3cd;
}
.operations-visual > i {
  position: absolute;
  left: 130px;
  top: 48px;
  color: var(--brand);
  font-size: 72px;
  transform: rotate(15deg);
}
.orbit-dot {
  position: absolute;
  width: 9px;
  height: 9px;
  background: var(--brand);
  border: 3px solid #e4efeb;
  border-radius: 50%;
  box-sizing: content-box;
}
.dot-one {
  top: 45px;
  left: 15px;
}
.dot-two {
  top: 110px;
  right: 35px;
}
.visual-caption {
  position: absolute;
  bottom: 0;
  left: 87px;
  font-size: 10px;
  letter-spacing: 4px;
  color: var(--muted);
}
.admin-auth-modules {
  display: flex;
  gap: 24px;
  color: var(--ink);
  font-size: 12px;
}
.admin-auth-modules i {
  color: var(--brand);
  margin-right: 5px;
}
.login-card {
  width: 100%;
  padding: 40px;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 20px;
  box-shadow: 0 24px 60px #1237460c;
}
.admin-lock {
  background: var(--tint);
  border-radius: 14px;
  width: 54px;
  height: 54px;
  display: grid;
  place-items: center;
  color: var(--brand);
  font-size: 26px;
  margin: 26px 0 20px;
}
.login-title {
  font-size: 25px;
  color: var(--ink);
  margin-bottom: 10px;
}
.login-subtitle {
  color: var(--muted);
  font-size: 13px;
  margin-bottom: 28px;
}
.login-card ::v-deep .el-form-item__label {
  line-height: 28px;
}
.login-card ::v-deep .el-input__inner {
  height: 46px;
  background: #f8fafb;
}
.login-card .el-button {
  height: 46px;
  margin-top: 8px;
}
.admin-access-note {
  text-align: center;
  font-size: 11px;
  color: var(--muted);
  margin: 24px 0 0;
}
.admin-auth-footer {
  text-align: center;
  font-size: 11px;
  letter-spacing: 2px;
  color: var(--muted);
}
@media (max-width: 1000px) {
  .admin-auth-layout {
    gap: 32px;
    grid-template-columns: 1fr 400px;
  }
  .admin-auth-story h1 {
    font-size: 34px;
  }
  .admin-auth-modules {
    gap: 12px;
  }
  .login-container {
    padding: 24px;
  }
}
@media (max-width: 900px) {
  .admin-auth-layout {
    grid-template-columns: 1fr;
    max-width: 440px;
    margin: 28px auto;
  }
  .admin-auth-story {
    display: none;
  }
  .login-card {
    padding: 30px 24px;
  }
  .admin-auth-brand {
    font-size: 19px;
  }
  .admin-auth-brand span {
    font-size: 10px;
  }
  .login-container {
    padding: 24px 16px;
  }
}
</style>
